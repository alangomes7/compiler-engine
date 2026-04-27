package core.lexer.models.atomic;

import lombok.Getter;

/**
 * Represents a lexical token produced by the lexer. A token consists of a type (e.g.,
 * "IDENTIFIER"), the actual lexeme (matched string), and positional information (line and column).
 *
 * @author Generated
 * @version 1.0
 */
@Getter
public class Token {
    private final String type;
    private final String lexeme;
    private final int line;
    private final int col;

    /**
     * Constructs a new Token.
     *
     * @param type the token type (e.g., keyword, number, identifier)
     * @param lexeme the exact text matched from the input
     * @param line the line number where the token starts (1‑based)
     * @param col the column number where the token starts (1‑based)
     */
    public Token(String type, String lexeme, int line, int col) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
    }

    /**
     * Returns a human‑readable string representation of this token.
     *
     * @return a formatted string like: &lt;type, 'lexeme', line X:Y&gt;
     */
    @Override
    public String toString() {
        return String.format("<%s, '%s', line %d:%d>", type, lexeme, line, col);
    }
}
