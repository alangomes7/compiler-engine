package core.lexer.models;

import core.lexer.models.atomic.Token;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Symbol Table and Token Stream for the lexer. Maintains the full sequential list of tokens for the
 * parser, while using a Map for O(1) fast lookups.
 *
 * <p>The table stores every token in the order it appears (token stream) and also keeps a mapping
 * from lexeme to its first occurrence for quick symbol lookup during semantic analysis.
 *
 * @author Generated
 * @version 1.0
 */
public class SymbolTable {

    /**
     * Maintains the exact sequence of ALL tokens (including duplicates) for the parser. Used during
     * syntax analysis to process the input in order.
     */
    private final List<Token> tokenStream;

    /**
     * Maintains O(1) lookup for fast searching (stores the first occurrence of each lexeme). Useful
     * for semantic analysis (e.g., checking if a variable was declared).
     */
    private final Map<String, Token> fastLookupTable;

    /** Constructs an empty symbol table with an empty token stream and an empty lookup map. */
    public SymbolTable() {
        this.tokenStream = new ArrayList<>();
        this.fastLookupTable = new LinkedHashMap<>();
    }

    /**
     * Inserts a token into the sequential stream and registers it in the lookup table. The token is
     * added to the end of the token stream. If the lexeme has not been seen before, it is also
     * added to the fast lookup table.
     *
     * @param tokenType the type of the token (e.g., "IDENTIFIER", "NUMBER")
     * @param lexeme the actual text of the token
     * @param line the line number where the token appears (1‑based)
     * @param col the column number where the token appears (1‑based)
     * @return the newly created Token object
     */
    public Token insert(String tokenType, String lexeme, int line, int col) {
        Token token = new Token(tokenType, lexeme, line, col);

        // 1. Add to the sequential token stream (for syntax analysis)
        tokenStream.add(token);

        // 2. Register in the lookup table if it's the first time we've seen it
        fastLookupTable.putIfAbsent(lexeme, token);

        return token;
    }

    /**
     * Looks up a lexeme in the symbol table in O(1) time. Returns the first registered occurrence
     * of this lexeme.
     *
     * @param lexeme the lexeme to search for
     * @return the first Token with the given lexeme, or null if not found
     */
    public Token lookup(String lexeme) {
        return fastLookupTable.get(lexeme);
    }

    /**
     * Clears both the token stream and the fast lookup table. Prints a confirmation message and the
     * (now empty) table.
     */
    public void clearTable() {
        this.tokenStream.clear();
        this.fastLookupTable.clear();
        System.out.println("\n=== Symbol Table: cleaned ===");
        printTable();
    }

    /**
     * Prints the entire token stream (all tokens in order) to standard output. If the stream is
     * empty, prints "(Empty)".
     */
    public void printTable() {
        System.out.println("\n=== Token Stream ===");
        if (tokenStream.isEmpty()) {
            System.out.println(" (Empty) ");
        } else {
            for (Token tok : tokenStream) {
                System.out.println(" " + tok);
            }
        }
        System.out.println("====================\n");
    }

    /**
     * Returns the full sequential list of tokens (including duplicates). The parser should call
     * this method for syntax analysis.
     *
     * @return the list of all tokens in the order they were inserted
     */
    public List<Token> getTable() {
        return tokenStream;
    }

    /**
     * Returns only the unique symbols (first occurrence of each lexeme) found. Useful later for
     * semantic analysis, e.g., building a symbol table.
     *
     * @return a list containing the first Token for each distinct lexeme
     */
    public List<Token> getUniqueSymbols() {
        return new ArrayList<>(fastLookupTable.values());
    }
}
