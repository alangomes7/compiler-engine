package core.parser.models.atomic;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParserError {
    private final int line;
    private final int col;
    private final String message;

    @Override
    public String toString() {
        if (line <= 0 && col <= 0) {
            return "Syntax Error: " + message;
        }
        return String.format("Syntax Error at line %d:%d: %s", line, col, message);
    }
}
