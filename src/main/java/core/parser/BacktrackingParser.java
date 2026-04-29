package core.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import core.lexer.models.atomic.Token;
import core.parser.models.Grammar;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import core.parser.models.tree.ParseTree;
import core.parser.utils.TokenFilter;
import models.atomic.Constants;

/**
 * A Top-Down Recursive Descent Parser with Backtracking.
 * Includes Deepest-Failure tracking to provide accurate syntax error reporting.
 */
public class BacktrackingParser {
    private final Grammar grammar;
    private final List<String> errors;

    private List<Token> tokens;
    private int lookaheadIndex;

    // --- Error Tracking for Backtracking ---
    private int maxLookaheadIndex = 0;
    private final Set<String> expectedAtMax = new HashSet<>();

    public BacktrackingParser(Grammar grammar) {
        this.grammar = grammar;
        this.errors = new ArrayList<>();
    }

    public ParseTree parse(List<Token> rawTokens) {
        this.errors.clear();
        this.lookaheadIndex = 0;
        this.maxLookaheadIndex = 0;
        this.expectedAtMax.clear();

        // Filter out comments and whitespaces
        TokenFilter tokenFilter = new TokenFilter();
        this.tokens = tokenFilter.filter(rawTokens);

        Node root = parseSymbol(grammar.getStartSymbol());

        // If it didn't consume all tokens, an error occurred. Report the DEEPEST error reached!
        if (root == null || (lookaheadIndex < tokens.size() && !isEofToken(tokens.get(lookaheadIndex)))) {
            Token failToken = (maxLookaheadIndex < tokens.size()) ? tokens.get(maxLookaheadIndex) : null;
            
            String failLexeme = (failToken != null) ? failToken.getLexeme() : "EOF";
            String failType = (failToken != null) ? failToken.getType() : "EOF";
            int line = (failToken != null) ? failToken.getLine() : -1;
            int col = (failToken != null) ? failToken.getCol() : -1;

            String expected = String.join(", ", expectedAtMax);
            
            errors.add(String.format(
                "Syntax Error at line %d:%d. Deepest parse reached token '%s' (Type: %s).\nExpected one of: [%s]",
                line, col, failLexeme, failType, expected
            ));
            
            // Return whatever partial tree we built
            return root != null ? new ParseTree(root) : null; 
        }

        return new ParseTree(root);
    }

    private Node parseSymbol(Symbol symbol) {
        if (symbol.equals(Symbol.EPSILON)) {
            return new Node(Symbol.EPSILON);
        }
        if (symbol.isTerminal() || symbol.equals(Symbol.EOF)) {
            return matchTerminal(symbol);
        } else {
            return parseNonTerminal(symbol);
        }
    }

    private Node parseNonTerminal(Symbol nonTerminal) {
        List<Production> productions = grammar.getProductionsFor(nonTerminal);
        int savedIndex = this.lookaheadIndex;

        for (Production production : productions) {
            Node currentNode = new Node(nonTerminal);
            boolean matchSuccess = true;

            for (Symbol rhsSymbol : production.getRhs()) {
                if (rhsSymbol.equals(Symbol.EPSILON)) continue;

                Node childNode = parseSymbol(rhsSymbol);

                if (childNode != null) {
                    currentNode.addChild(childNode);
                } else {
                    matchSuccess = false;
                    break; 
                }
            }

            if (matchSuccess) {
                return currentNode; // Successfully parsed this production
            } else {
                this.lookaheadIndex = savedIndex; // BACKTRACK!
            }
        }
        return null; // Exhausted all productions
    }

    private Node matchTerminal(Symbol expectedTerminal) {
        Token currentToken = (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;
        Symbol lookahead = resolveLookahead(currentToken);

        if (expectedTerminal.equals(lookahead)) {
            Node node = new Node(expectedTerminal);
            if (currentToken != null) node.setLexeme(currentToken.getLexeme());
            if (!expectedTerminal.equals(Symbol.EOF)) lookaheadIndex++;
            return node;
        }

        // --- TRACK FARTHEST FAILURE ---
        // If the match fails, log it only if it's the furthest point the parser has reached
        if (lookaheadIndex > maxLookaheadIndex) {
            maxLookaheadIndex = lookaheadIndex;
            expectedAtMax.clear();
            expectedAtMax.add(expectedTerminal.getName());
        } else if (lookaheadIndex == maxLookaheadIndex) {
            expectedAtMax.add(expectedTerminal.getName());
        }

        return null;
    }

    private Symbol resolveLookahead(Token token) {
        if (token == null) return Symbol.EOF;

        String lexeme = token.getLexeme();
        String tokenType = token.getType();

        // --- NORMALIZATION ---
        // The Lexer emits types like 'INT_NUM' or 'NEWLINE_CH', but the EBNF grammar 
        // strictly defines terminals as 'number' or 'newline'. We must bridge this gap.
        String normalizedType = tokenType;
        if ("comment".equals(tokenType)) normalizedType = "#";
        else if ("NEWLINE_CH".equals(tokenType)) normalizedType = "newline";
        else if (tokenType != null && tokenType.endsWith("_NUM")) normalizedType = "number";
        else if ("DIGIT".equals(tokenType)) normalizedType = "number";
        else if ("LOWER".equals(tokenType) || "UPPER".equals(tokenType)) normalizedType = "identifier";
        else if ("INC_PRE".equals(tokenType)) normalizedType = "++_pre";
        else if ("DEC_PRE".equals(tokenType)) normalizedType = "--_pre";
        else if ("INC_POST".equals(tokenType)) normalizedType = "++_post";
        else if ("DEC_POST".equals(tokenType)) normalizedType = "--_post";

        // 1. Check normalized type (e.g., matching INT_NUM to "number")
        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(normalizedType)) return terminal;
        }

        // 2. Check exact lexeme (for operators like "+", "-", "==", "if")
        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(lexeme)) return terminal;
        }

        // 3. Fallback to raw token type
        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(tokenType)) return terminal;
        }

        return new Symbol(normalizedType != null ? normalizedType : lexeme, true);
    }

    private boolean isEofToken(Token token) {
        if (token.getLexeme() == null) return false;
        return token.getLexeme().equals(Constants.EOF) || token.getType().equals("EOF");
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}