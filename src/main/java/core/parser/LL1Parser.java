package core.parser;

import core.lexer.models.atomic.Token;
import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import core.parser.models.tree.ParseTree;
import core.parser.utils.TokenFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import models.atomic.Constants;

/**
 * LL(1) predictive parser that uses a parse table to perform top‑down syntax analysis. The parser
 * consumes a token stream (from the lexer) and builds a parse tree. In case of syntax errors, a
 * partial parse tree is returned and errors are recorded.
 *
 * <p>The parser expects the grammar to be LL(1) – i.e., the parse table should have at most one
 * production per cell. Conflicts are not resolved automatically.
 *
 * @version 1.2
 */
public class LL1Parser {
    private final Grammar grammar;
    private final ParseTable parseTable;
    private final List<String> errors;

    /**
     * Constructs an LL(1) parser with the given grammar and precomputed parse table.
     *
     * @param grammar the context‑free grammar
     * @param parseTable the LL(1) parse table (must be compatible with the grammar)
     */
    public LL1Parser(Grammar grammar, ParseTable parseTable) {
        this.grammar = grammar;
        this.parseTable = parseTable;
        this.errors = new ArrayList<>();
    }

    /**
     * Parses a list of tokens and returns the parse tree. If syntax errors are encountered, the
     * method returns a partial tree constructed up to the point of the first error. All errors are
     * stored and can be retrieved via {@link #getErrors()}.
     *
     * @param rawTokens the token stream from the lexer (must not be null)
     * @return the parse tree (may be partial if errors occurred)
     */
    public ParseTree parse(List<Token> rawTokens) {
        errors.clear();
        Stack<Node> stack = new Stack<>();

        // 0. Filter out irrelevant tokens (like comments and spaces)
        List<Token> tokens = TokenFilter.filter(rawTokens);

        Node root = new Node(grammar.getStartSymbol());
        stack.push(new Node(Symbol.EOF));
        stack.push(root);

        int lookaheadIndex = 0;

        while (!stack.isEmpty()) {
            Node currentNode = stack.pop();
            Symbol top = currentNode.getSymbol();

            // 1. Handle EPSILON (Empty string) productions safely
            if (top.equals(Symbol.EPSILON)) {
                // Epsilon nodes do not consume any input. Just proceed.
                continue;
            }

            Token currentToken =
                    (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;
            Symbol lookahead = resolveLookahead(currentToken);

            // 2. Terminal matching
            if (top.isTerminal() || top.equals(Symbol.EOF)) {
                if (top.equals(lookahead)) {
                    if (currentToken != null) {
                        currentNode.setLexeme(currentToken.getLexeme());
                    }
                    if (!top.equals(Symbol.EOF)) {
                        lookaheadIndex++;
                    }
                } else {
                    recordError("Expected '%s', but found '%s'", currentToken, top.getName());
                    return new ParseTree(root); // Return partial broken tree
                }
            }
            // 3. Non-Terminal Derivation
            else {
                List<Production> productions = this.parseTable.getEntry(top, lookahead);

                // Safe check: handle null returns for empty parse table cells
                if (productions == null || productions.isEmpty()) {
                    recordError(
                            "No rule to derive '%s' with lookahead '%s'",
                            currentToken, top.getName());
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

                // Push onto the stack in reverse order
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(children.get(i));
                }
            }
        }

        // 4. Safe EOF Check
        if (lookaheadIndex < tokens.size()) {
            Token remaining = tokens.get(lookaheadIndex);
            if (remaining != null && !isEofToken(remaining)) {
                errors.add(
                        "Syntax Error: Unexpected tokens after program end. Found: "
                                + remaining.getLexeme());
            }
        }

        return new ParseTree(root);
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
        } else if (tokenType.endsWith("_NUM")) {
            // Catches INT_NUM, FLOAT_NUM, HEX_NUM, etc.
            normalizedType = "number";
        } else if ("LOWER".equals(tokenType) || "UPPER".equals(tokenType)) {
            // Lexer emits LOWER/UPPER for single-character variables, but parser expects identifier
            normalizedType = "identifier";
        }

        // 1. Try to match by Normalized Token Type first
        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(normalizedType)) {
                return terminal;
            }
        }

        // 2. Fallback to Exact Lexeme matching (handles operators like "+", "==", and keywords)
        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(lexeme)) {
                return terminal;
            }
        }

        // If not found in grammar, return a temporary terminal for error reporting
        return new Symbol(tokenType != null ? tokenType : lexeme, true);
    }

    /** Helper to consolidate error message generation. */
    private void recordError(String messageTemplate, Token currentToken, String expectedOrDerived) {
        int line = (currentToken != null) ? currentToken.getLine() : 0;
        int col = (currentToken != null) ? currentToken.getCol() : 0;
        String found = (currentToken != null) ? currentToken.getLexeme() : "EOF";

        String detail = String.format(messageTemplate, expectedOrDerived, found);
        errors.add(String.format("Syntax Error at line %d:%d: %s", line, col, detail));
    }

    /** Helper to safely check if a token represents EOF. */
    private boolean isEofToken(Token token) {
        if (token.getLexeme() == null) return false;
        // Allows for both constant matching and literal string matching
        return token.getLexeme().equals(Constants.EOF) || token.getType().equals("EOF");
    }

    /**
     * Returns the list of syntax errors encountered during the last parse operation.
     *
     * @return a copy of the error list (may be empty)
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}
