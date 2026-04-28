package core.parser;

import java.util.ArrayList;
import java.util.List;

import core.lexer.models.atomic.Token;
import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import core.parser.models.tree.ParseTree;
import core.parser.utils.TokenFilter;
import models.atomic.Constants;

/**
 * A generic predictive Recursive Descent Parser for LL(1) grammars..
 */
public class RecursiveDescentParser {
    private final Grammar grammar;
    private final ParseTable parseTable;
    private final List<String> errors;

    // Track the parser's state during the recursion
    private List<Token> tokens;
    private int lookaheadIndex;

    /**
     * Constructs a Recursive Descent parser.
     *
     * @param grammar the context‑free grammar
     * @param parseTable the LL(1) parse table computed from the grammar
     */
    public RecursiveDescentParser(Grammar grammar, ParseTable parseTable) {
        this.grammar = grammar;
        this.parseTable = parseTable;
        this.errors = new ArrayList<>();
    }

    /**
     * Parses the stream of tokens and builds the corresponding AST recursively.
     */
    public ParseTree parse(List<Token> rawTokens) {
        this.errors.clear();
        this.lookaheadIndex = 0;
        
        // 0. Filter out irrelevant tokens (like comments and spaces)
        TokenFilter tokenFilter = new TokenFilter();
        this.tokens = tokenFilter.filter(rawTokens);

        // 1. Begin recursive descent from the start symbol
        Node root = parseNonTerminal(grammar.getStartSymbol());

        // 2. Safe EOF Check to ensure no trailing invalid tokens exist
        if (lookaheadIndex < tokens.size()) {
            Token remaining = tokens.get(lookaheadIndex);
            if (remaining != null && !isEofToken(remaining)) {
                errors.add("Syntax Error: Unexpected tokens after program end. Found: " + remaining.getLexeme());
            }
        }

        return new ParseTree(root);
    }

    /**
     * Routes the parsing flow depending on whether the symbol is a terminal or non-terminal.
     */
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

    /**
     * Recursively parses a non-terminal by looking ahead and expanding the correct production rule.
     */
    private Node parseNonTerminal(Symbol nonTerminal) {
        Node currentNode = new Node(nonTerminal);
        
        Token currentToken = (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;
        Symbol lookahead = resolveLookahead(currentToken);

        // Consult the parse table to predict the next production
        List<Production> productions = this.parseTable.getEntry(nonTerminal, lookahead);

        if (productions == null || productions.isEmpty()) {
            recordError("No rule to derive '%s' with lookahead '%s'", currentToken, nonTerminal.getName());
            return currentNode;
        }

        // Apply the predicted production
        Production production = productions.get(0);

        for (Symbol rhsSymbol : production.getRhs()) {
            if (rhsSymbol.equals(Symbol.EPSILON)) {
                continue;
            }
            
            // Recurse into the RHS symbol
            Node childNode = parseSymbol(rhsSymbol);
            
            if (childNode != null) {
                currentNode.addChild(childNode);
            }
        }

        return currentNode;
    }

    /**
     * Consumes a token if it successfully matches the expected terminal symbol.
     */
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

    /** Maps a lexer token to a parser grammar symbol. */
    private Symbol resolveLookahead(Token token) {
        if (token == null) {
            return Symbol.EOF;
        }

        String lexeme = token.getLexeme();
        String tokenType = token.getType();

        // 0. Normalize token types to match parser terminal names
        String normalizedType = tokenType;
        if ("comment".equals(tokenType)) {
            normalizedType = "#";
        } else if (tokenType != null && tokenType.endsWith("_NUM")) {
            normalizedType = "number";
        } else if ("LOWER".equals(tokenType) || "UPPER".equals(tokenType)) {
            normalizedType = "identifier";
        }

        // 1. Match by Normalized Token Type first
        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(normalizedType)) {
                return terminal;
            }
        }

        // 2. Fallback to Exact Lexeme matching
        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(lexeme)) {
                return terminal;
            }
        }

        return new Symbol(tokenType != null ? tokenType : lexeme, true);
    }

    /** Centralized error recording. */
    private void recordError(String messageTemplate, Token currentToken, String expectedOrDerived) {
        int line = (currentToken != null) ? currentToken.getLine() : 0;
        int col = (currentToken != null) ? currentToken.getCol() : 0;
        String found = (currentToken != null) ? currentToken.getLexeme() : "EOF";

        String detail = String.format(messageTemplate, expectedOrDerived, found);
        errors.add(String.format("Syntax Error at line %d:%d: %s", line, col, detail));
    }

    private boolean isEofToken(Token token) {
        if (token.getLexeme() == null) return false;
        return token.getLexeme().equals(Constants.EOF) || token.getType().equals("EOF");
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}