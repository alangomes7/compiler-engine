package core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import core.lexer.models.atomic.Token;
import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import core.parser.models.tree.ParseTree;

public class LL1Parser {
    private final Grammar grammar;
    private final ParseTable parseTable;
    private final List<String> errors;

    public LL1Parser(Grammar grammar, ParseTable parseTable) {
        this.grammar = grammar;
        this.parseTable = parseTable;
        this.errors = new ArrayList<>();
    }

    /**
     * Parses tokens and returns the Parse Tree. If errors occur, it returns the 
     * partial broken tree constructed up to the error point.
     */
    public ParseTree parse(List<Token> tokens) {
        errors.clear();
        Stack<Node> stack = new Stack<>();

        Node root = new Node(grammar.getStartSymbol());
        stack.push(new Node(Symbol.EOF)); 
        stack.push(root);

        int lookaheadIndex = 0;

        while (!stack.isEmpty()) {
            Node currentNode = stack.pop();
            Symbol top = currentNode.getSymbol();

            Token currentToken = (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;
            Symbol lookahead = resolveLookahead(currentToken);

            if (top.isTerminal()) {
                if (top.equals(lookahead)) {
                    if (currentToken != null) {
                        currentNode.setLexeme(currentToken.getLexeme());
                    }
                    if (!top.equals(Symbol.EOF)) {
                        lookaheadIndex++;
                    }
                } else {
                    errors.add(String.format("Syntax Error at line %d:%d: Expected '%s', but found '%s'",
                        (currentToken != null ? currentToken.getLine() : 0),
                        (currentToken != null ? currentToken.getCol() : 0),
                        top.getName(), 
                        (currentToken != null ? currentToken.getLexeme() : "EOF")));
                    return new ParseTree(root); // Return partial broken tree
                }
            } else {
                List<Production> productions = this.parseTable.getEntry(top, lookahead);

                if (productions.isEmpty()) {
                    errors.add(String.format("Syntax Error at line %d:%d: No rule to derive '%s' with lookahead '%s'", 
                        (currentToken != null ? currentToken.getLine() : 0),
                        (currentToken != null ? currentToken.getCol() : 0),
                        top.getName(), 
                        (currentToken != null ? currentToken.getLexeme() : "EOF")));
                    return new ParseTree(root); // Return partial broken tree
                }

                Production production = productions.get(0);
                List<Symbol> rhs = production.getRhs();

                List<Node> children = new ArrayList<>();
                for (Symbol s : rhs) {
                    Node child = new Node(s);
                    currentNode.addChild(child);
                    children.add(child);
                }

                if (!production.isEpsilonProduction()) {
                    for (int i = children.size() - 1; i >= 0; i--) {
                        stack.push(children.get(i));
                    }
                }
            }
        }

        if (lookaheadIndex < tokens.size()) {
            errors.add("Syntax Error: Unexpected tokens after program end.");
        }

        return new ParseTree(root);
    }

    /**
     * Maps the Lexer's Token to the Parser's Symbol (Object-based).
     * Must check BOTH the token's Lexeme (for literals) 
     * and the TokenType (for sets like "IDENTIFIER" or "INT").
     */
    private Symbol resolveLookahead(Token token) {
        if (token == null) {
            return Symbol.EOF;
        }
        
        String lexeme = token.getLexeme();
        String tokenType = token.getTokenType();
        
        // 1. Try to match by Exact Lexeme first (e.g., "+", "&&", "==")
        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(lexeme)) {
                return terminal;
            }
        }

        // 2. Try to match by Token Type (e.g., "IDENTIFIER", "INT", "FLOAT")
        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(tokenType)) {
                return terminal;
            }
        }
        
        // If not found in grammar, return a temporary terminal for error reporting
        return new Symbol(tokenType != null ? tokenType : lexeme, true);
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}