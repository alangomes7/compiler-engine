package core.parser;

import core.lexer.models.atomic.Token;
import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.ParserError;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import core.parser.models.tree.ParseTree;
import core.parser.utils.TokenFilter;
import java.util.ArrayList;
import java.util.List;
import models.atomic.Constants;

public class RecursiveDescentParser {
    private final Grammar grammar;
    private final ParseTable parseTable;
    private final List<ParserError> errors;

    private List<Token> tokens;
    private int lookaheadIndex;

    public RecursiveDescentParser(Grammar grammar, ParseTable parseTable) {
        this.grammar = grammar;
        this.parseTable = parseTable;
        this.errors = new ArrayList<>();
    }

    public ParseTree parse(List<Token> rawTokens) {
        this.errors.clear();
        this.lookaheadIndex = 0;

        TokenFilter tokenFilter = new TokenFilter();
        this.tokens = tokenFilter.filter(rawTokens);

        Node root = parseNonTerminal(grammar.getStartSymbol());

        matchTerminal(Symbol.EOF);

        if (lookaheadIndex < tokens.size()) {
            Token remaining = tokens.get(lookaheadIndex);
            if (remaining != null && !isEofToken(remaining)) {
                errors.add(
                        new ParserError(
                                remaining.getLine(),
                                remaining.getCol(),
                                String.format(
                                        "Syntax Error at [%d, %d]: Unexpected tokens after program end. Found: '%s'",
                                        remaining.getLine(),
                                        remaining.getCol(),
                                        remaining.getLexeme())));
            }
        }

        return new ParseTree(root);
    }

    private Node parseSymbol(Symbol symbol) {
        if (symbol.equals(Symbol.EPSILON)) {
            return null;
        }

        if (symbol.isTerminal() || symbol.equals(Symbol.EOF)) {
            return matchTerminal(symbol);
        } else {
            return parseNonTerminal(symbol);
        }
    }

    private Node parseNonTerminal(Symbol nonTerminal) {
        Node currentNode = new Node(nonTerminal);

        Token currentToken = (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;
        Symbol lookahead = resolveLookahead(currentToken);

        List<Production> productions = this.parseTable.getEntry(nonTerminal, lookahead);

        if (productions == null || productions.isEmpty()) {
            recordError(
                    "No rule to derive '%s' with lookahead '%s'",
                    currentToken, nonTerminal.getName());

            if (currentToken != null && !isEofToken(currentToken)) {
                lookaheadIndex++;
            }
            return currentNode;
        }

        Production production = productions.get(0);

        for (Symbol rhsSymbol : production.getRhs()) {
            if (rhsSymbol.equals(Symbol.EPSILON)) {
                continue;
            }

            Node childNode = parseSymbol(rhsSymbol);

            if (childNode != null) {
                currentNode.addChild(childNode);
            }
        }

        return currentNode;
    }

    private Node matchTerminal(Symbol expectedTerminal) {
        Node node = new Node(expectedTerminal);
        Token currentToken = (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;
        Symbol lookahead = resolveLookahead(currentToken);

        if (expectedTerminal.equals(lookahead)) {
            if (currentToken != null) {
                node.setLexeme(currentToken.getLexeme());
            }
            if (!expectedTerminal.equals(Symbol.EOF)) {
                lookaheadIndex++;
            }
        } else {
            recordError("Expected '%s', but found '%s'", currentToken, expectedTerminal.getName());
        }

        return node;
    }

    private Symbol resolveLookahead(Token token) {
        if (token == null) {
            return Symbol.EOF;
        }

        String lexeme = token.getLexeme();
        String tokenType = token.getType();

        String normalizedType = tokenType;
        if ("comment".equals(tokenType)) {
            normalizedType = "#";
        } else if ("NEWLINE_CH".equals(tokenType)) {
            normalizedType = "newline";
        } else if (tokenType != null && tokenType.endsWith("_NUM")) {
            normalizedType = "number";
        } else if ("DIGIT".equals(tokenType)) {
            normalizedType = "number";
        } else if ("LOWER".equals(tokenType) || "UPPER".equals(tokenType)) {
            normalizedType = "identifier";
        } else if ("INC_PRE".equals(tokenType)) {
            normalizedType = "++_pre";
        } else if ("DEC_PRE".equals(tokenType)) {
            normalizedType = "--_pre";
        } else if ("INC_POST".equals(tokenType)) {
            normalizedType = "++_post";
        } else if ("DEC_POST".equals(tokenType)) {
            normalizedType = "--_post";
        }

        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(normalizedType)) {
                return terminal;
            }
        }

        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(lexeme)) {
                return terminal;
            }
        }

        return new Symbol(tokenType != null ? tokenType : lexeme, true);
    }

    private boolean isEofToken(Token token) {
        if (token.getLexeme() == null) return false;
        return token.getLexeme().equals(Constants.EOF) || token.getType().equals("EOF");
    }

    private void recordError(String messageTemplate, Token currentToken, String expectedOrDerived) {
        int line = (currentToken != null) ? currentToken.getLine() : 0;
        int col = (currentToken != null) ? currentToken.getCol() : 0;
        String found = (currentToken != null) ? currentToken.getLexeme() : "EOF";
        String detail =
                String.format(
                        "Syntax Error at [%d, %d]: " + messageTemplate,
                        line,
                        col,
                        expectedOrDerived,
                        found);
        errors.add(new ParserError(line, col, detail));
    }

    public List<ParserError> getErrors() {
        return new ArrayList<>(errors);
    }
}
