package core.lexer;

/**
 * Temporary container for a token that has been recognised but not yet inserted into the symbol
 * table. Used to defer insertion until after error checking.
 *
 * <p>If scanning completes without errors, all pending symbols are added to the symbol table;
 * otherwise, they are discarded.
 *
 * @author Generated
 * @version 1.0
 */
class PendingSymbol {
    final String lexeme;
    final String tokenType;
    final int line;
    final int col;

    /**
     * Constructs a pending symbol.
     *
     * @param lexeme the actual text matched
     * @param tokenType the type of the token
     * @param line the line number where the token starts (1‑based)
     * @param col the column number where the token starts (1‑based)
     */
    PendingSymbol(String lexeme, String tokenType, int line, int col) {
        this.lexeme = lexeme;
        this.tokenType = tokenType;
        this.line = line;
        this.col = col;
    }
}
