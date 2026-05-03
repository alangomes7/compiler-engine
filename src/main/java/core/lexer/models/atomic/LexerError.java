package core.lexer.models.atomic;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LexerError {
    private final int line;
    private final int col;
    private final String message;

    @Override
    public String toString() {
        if (line <= 0 && col <= 0) {
            return "Lexical Error: " + message;
        }
        return String.format("Lexical Error at line %d:%d: %s", line, col, message);
    }
}
