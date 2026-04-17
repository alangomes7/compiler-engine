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
     * Parses tokens and returns the Parse Tree or null if invalid.
     */
    public ParseTree parse(List<Token> tokens) {
        errors.clear();
        Stack<Node> stack = new Stack<>();

        // 1. Initialize Root and Stack
        Node root = new Node(grammar.getStartSymbol());
        // Use a placeholder for the EOF node matching the Symbol constant
        stack.push(new Node(Symbol.EOF)); 
        stack.push(root);

        int lookaheadIndex = 0;

        while (!stack.isEmpty()) {
            Node currentNode = stack.pop();
            Symbol top = currentNode.getSymbol();

            // 2. Resolve Lookahead Token and Symbol
            Token currentToken = (lookaheadIndex < tokens.size()) 
                ? tokens.get(lookaheadIndex) 
                : null;
            
            // Map Lexer String type to Parser Symbol object
            Symbol lookahead = resolveLookahead(currentToken);

            if (top.isTerminal()) {
                if (top.equals(lookahead)) {
                    // Successful Match: link the lexeme to the tree node
                    if (currentToken != null) {
                        currentNode.setLexeme(currentToken.getLexeme());
                    }
                    // Only advance if we matched a real token (not the end-of-file symbol)
                    if (!top.equals(Symbol.EOF)) {
                        lookaheadIndex++;
                    }
                } else {
                    errors.add(String.format("Syntax Error at line %d:%d: Expected '%s', but found '%s'",
                        (currentToken != null ? currentToken.getLine() : 0),
                        (currentToken != null ? currentToken.getCol() : 0),
                        top.getName(), lookahead.getName()));
                    return null;
                }
            } else {
                // 3. Non-terminal: Query the Parse Table
                List<Production> productions = parseTable.getEntry(top, lookahead);

                if (productions.isEmpty()) {
                    errors.add(String.format("Syntax Error: No rule to derive '%s' with lookahead '%s'", 
                        top.getName(), lookahead.getName()));
                    return null;
                }

                // In LL(1), we take the first production in the list
                Production production = productions.get(0);
                List<Symbol> rhs = production.getRhs();

                // Build children for the current tree node
                List<Node> children = new ArrayList<>();
                for (Symbol s : rhs) {
                    Node child = new Node(s);
                    currentNode.addChild(child);
                    children.add(child);
                }

                // Push RHS symbols to stack in reverse (skipping EPSILON)
                if (!production.isEpsilonProduction()) {
                    for (int i = children.size() - 1; i >= 0; i--) {
                        stack.push(children.get(i));
                    }
                }
            }
        }

        // Final check: Ensure all tokens were consumed
        if (lookaheadIndex < tokens.size()) {
            errors.add("Syntax Error: Unexpected tokens after program end.");
            return null;
        }

        return new ParseTree(root);
    }

    /**
     * Maps the Lexer's Token (String-based) to the Parser's Symbol (Object-based).
     */
    private Symbol resolveLookahead(Token token) {
        if (token == null) {
            return Symbol.EOF;
        }
        
        String typeName = token.getTokenType();
        
        // Check if the type matches any terminal in our grammar
        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(typeName)) {
                return terminal;
            }
        }
        
        // If not found in grammar, return a temporary terminal for error reporting
        return new Symbol(typeName, true);
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}