package core.parser;

import java.util.ArrayList;
import java.util.List;

import core.parser.models.SyntaxTreeNode;
import core.parser.models.atomic.Symbol;

public class RecursiveDescentParser {

    private final List<Symbol> tokens;
    private int currentPosition = 0;
    private final List<String> errors = new ArrayList<>();

    public RecursiveDescentParser(List<Symbol> tokens) {
        this.tokens = tokens;
    }

    /**
     * Inicia a análise sintática.
     * Retorna a raiz da árvore em caso de sucesso, ou null se houver erros impeditivos.
     */
    public SyntaxTreeNode parse() {
        SyntaxTreeNode root = awkProgram();
        
        if (!isAtEnd()) {
            reportError("Tokens inesperados no final do arquivo a partir de: " + peek().getLexeme());
        }
        
        return root;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // =========================================================================
    // MÉTODOS DA GRAMÁTICA (Mapeamento EBNF para Java)
    // =========================================================================

    // awk-program ::= { top-level-element | comment }
    private SyntaxTreeNode awkProgram() {
        SyntaxTreeNode node = new SyntaxTreeNode("awk-program");
        
        while (!isAtEnd()) {
            // Ignorar comentários, se o Lexer não os filtrou
            if (matchTokenType("COMMENT")) {
                continue; 
            }
            try {
                node.addChild(topLevelElement());
            } catch (ParseException e) {
                synchronize(); // Tenta recuperar do erro pulando tokens até um ponto seguro
            }
        }
        return node;
    }

    // top-level-element ::= function-definition | pattern-action-pair
    private SyntaxTreeNode topLevelElement() {
        SyntaxTreeNode node = new SyntaxTreeNode("top-level-element");
        
        if (checkLexeme("function")) {
            node.addChild(functionDefinition());
        } else {
            node.addChild(patternActionPair());
        }
        return node;
    }

    // function-definition ::= "function" identifier '(' [ identifier-list ] ')' compound-statements
    private SyntaxTreeNode functionDefinition() {
        SyntaxTreeNode node = new SyntaxTreeNode("function-definition");
        
        node.addChild(new SyntaxTreeNode(consumeLexeme("function", "Esperado 'function'").getLexeme()));
        node.addChild(new SyntaxTreeNode(consumeTokenType("IDENTIFIER", "Esperado nome da função").getLexeme()));
        node.addChild(new SyntaxTreeNode(consumeLexeme("(", "Esperado '(' após nome da função").getLexeme()));
        
        if (!checkLexeme(")")) {
            node.addChild(identifierList());
        }
        
        node.addChild(new SyntaxTreeNode(consumeLexeme(")", "Esperado ')' após parâmetros").getLexeme()));
        node.addChild(compoundStatements());
        
        return node;
    }

    // pattern-action-pair ::= [ pattern ] action
    private SyntaxTreeNode patternActionPair() {
        SyntaxTreeNode node = new SyntaxTreeNode("pattern-action-pair");
        
        // Se o próximo token indicar o início de um bloco de ação diretamente, pattern é omitido
        if (!checkLexeme("{")) {
            node.addChild(pattern());
        }
        node.addChild(action());
        return node;
    }

    // pattern ::= expression-range | "BEGIN" | "END"
    private SyntaxTreeNode pattern() {
        SyntaxTreeNode node = new SyntaxTreeNode("pattern");
        
        if (matchLexeme("BEGIN")) {
            node.addChild(new SyntaxTreeNode("BEGIN"));
        } else if (matchLexeme("END")) {
            node.addChild(new SyntaxTreeNode("END"));
        } else {
            node.addChild(expressionRange());
        }
        return node;
    }

    // expression-range ::= expression [ ',' expression ]
    private SyntaxTreeNode expressionRange() {
        SyntaxTreeNode node = new SyntaxTreeNode("expression-range");
        node.addChild(expression());
        
        if (matchLexeme(",")) {
            node.addChild(new SyntaxTreeNode(","));
            node.addChild(expression());
        }
        return node;
    }

    // action ::= compound-statements
    private SyntaxTreeNode action() {
        SyntaxTreeNode node = new SyntaxTreeNode("action");
        node.addChild(compoundStatements());
        return node;
    }

    // compound-statements ::= '{' statement-list '}'
    private SyntaxTreeNode compoundStatements() {
        SyntaxTreeNode node = new SyntaxTreeNode("compound-statements");
        node.addChild(new SyntaxTreeNode(consumeLexeme("{", "Esperado '{' para iniciar o bloco").getLexeme()));
        node.addChild(statementList());
        node.addChild(new SyntaxTreeNode(consumeLexeme("}", "Esperado '}' para fechar o bloco").getLexeme()));
        return node;
    }

    // statement-list ::= statement { terminate statement }
    private SyntaxTreeNode statementList() {
        SyntaxTreeNode node = new SyntaxTreeNode("statement-list");
        
        // Enquanto não encontrar o fim do bloco '}'
        while (!checkLexeme("}") && !isAtEnd()) {
            node.addChild(statement());
            // Consome os terminadores (; ou \n) se existirem
            while (matchLexeme(";") || matchTokenType("NEWLINE")) {
                // opcionalmente adiciona à árvore ou simplesmente consome
            }
        }
        return node;
    }

    // statement ::= ... (Mapeamento direto usando o first-set de cada regra)
    private SyntaxTreeNode statement() {
        SyntaxTreeNode node = new SyntaxTreeNode("statement");
        
        if (checkLexeme("print")) {
            node.addChild(printStatement());
        } else if (checkLexeme("if")) {
            node.addChild(ifStatement());
        } else if (checkLexeme("while")) {
            node.addChild(whileStatement());
        } else if (checkLexeme("for")) {
            // node.addChild(forStatement()); // Implementar seguindo o padrão
        } else {
            node.addChild(expressionStatement());
        }
        return node;
    }

    // if-statement ::= if-without-else | if-with-else
    // Esta implementação resolve o if unambíguo lendo a keyword 'else' de forma gulosa
    private SyntaxTreeNode ifStatement() {
        SyntaxTreeNode node = new SyntaxTreeNode("if-statement");
        node.addChild(new SyntaxTreeNode(consumeLexeme("if", "Esperado 'if'").getLexeme()));
        node.addChild(new SyntaxTreeNode(consumeLexeme("(", "Esperado '(' após if").getLexeme()));
        node.addChild(expression());
        node.addChild(new SyntaxTreeNode(consumeLexeme(")", "Esperado ')' após condição").getLexeme()));
        
        node.addChild(compoundStatements());
        
        if (matchLexeme("else")) {
            node.addChild(new SyntaxTreeNode("else"));
            if (checkLexeme("if")) {
                node.addChild(ifStatement());
            } else {
                node.addChild(compoundStatements());
            }
        }
        
        return node;
    }

    // Exemplo de regra básica para suportar a árvore: identifier-list ::= identifier { ',' identifier }
    private SyntaxTreeNode identifierList() {
        SyntaxTreeNode node = new SyntaxTreeNode("identifier-list");
        node.addChild(new SyntaxTreeNode(consumeTokenType("IDENTIFIER", "Esperado identificador").getLexeme()));
        
        while (matchLexeme(",")) {
            node.addChild(new SyntaxTreeNode(consumeTokenType("IDENTIFIER", "Esperado identificador após vírgula").getLexeme()));
        }
        return node;
    }

    // =========================================================================
    // MÉTODOS DE SUPORTE PARA EXPRESSÕES (Para evitar recursão à esquerda)
    // =========================================================================
    
    // stub temporário para permitir compilação.
    // Expanda expression() descendo pelas regras binary-expr, unary-expr, etc.
    private SyntaxTreeNode expression() {
        SyntaxTreeNode node = new SyntaxTreeNode("expression");
        node.addChild(new SyntaxTreeNode(consumeTokenType("IDENTIFIER", "Esperado expressão válida").getLexeme())); // Simplificado
        return node;
    }
    
    private SyntaxTreeNode printStatement() {
        SyntaxTreeNode node = new SyntaxTreeNode("print-statement");
        node.addChild(new SyntaxTreeNode(consumeLexeme("print", "Esperado 'print'").getLexeme()));
        // Adicionar expr-list etc...
        return node;
    }
    
    private SyntaxTreeNode whileStatement() {
        SyntaxTreeNode node = new SyntaxTreeNode("while-statement");
        node.addChild(new SyntaxTreeNode(consumeLexeme("while", "Esperado 'while'").getLexeme()));
        node.addChild(new SyntaxTreeNode(consumeLexeme("(", "Esperado '('").getLexeme()));
        node.addChild(expression());
        node.addChild(new SyntaxTreeNode(consumeLexeme(")", "Esperado ')'").getLexeme()));
        node.addChild(compoundStatements());
        return node;
    }
    
    private SyntaxTreeNode expressionStatement() {
        SyntaxTreeNode node = new SyntaxTreeNode("expression-statement");
        node.addChild(expression());
        return node;
    }

    // =========================================================================
    // MECÂNICA DE PARSING (Avanço, Checagem e Erros)
    // =========================================================================

    private boolean matchTokenType(String tokenType) {
        if (checkTokenType(tokenType)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchLexeme(String lexeme) {
        if (checkLexeme(lexeme)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean checkTokenType(String tokenType) {
        if (isAtEnd()) return false;
        return peek().getTokenType().equals(tokenType);
    }

    private boolean checkLexeme(String lexeme) {
        if (isAtEnd()) return false;
        return peek().getLexeme().equals(lexeme);
    }

    private Symbol advance() {
        if (!isAtEnd()) currentPosition++;
        return previous();
    }

    private Symbol peek() {
        return tokens.get(currentPosition);
    }

    private Symbol previous() {
        return tokens.get(currentPosition - 1);
    }

    private boolean isAtEnd() {
        return currentPosition >= tokens.size() || peek().getLexeme().equals("$");
    }

    private Symbol consumeLexeme(String lexeme, String errorMessage) {
        if (checkLexeme(lexeme)) return advance();
        throw reportError(errorMessage + " | Encontrado: '" + peek().getLexeme() + "' na linha " + peek().getLine());
    }

    private Symbol consumeTokenType(String tokenType, String errorMessage) {
        if (checkTokenType(tokenType)) return advance();
        throw reportError(errorMessage + " | Encontrado: '" + peek().getTokenType() + "' na linha " + peek().getLine());
    }

    private ParseException reportError(String message) {
        errors.add(message);
        return new ParseException();
    }

    // Panic Mode Sincronization
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().getLexeme().equals(";") || previous().getLexeme().equals("\n")) return;

            switch (peek().getLexeme()) {
                case "function":
                case "BEGIN":
                case "END":
                case "if":
                case "while":
                case "for":
                case "print":
                case "return":
                    return;
            }
            advance();
        }
    }

    private static class ParseException extends RuntimeException { }
}