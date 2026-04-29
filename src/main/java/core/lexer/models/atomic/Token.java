package core.lexer.models.atomic;

import lombok.Getter;

@Getter
public class Token {
    private final String type;
    private final String lexeme;
    private final int line;
    private final int col;

    public Token(String type, String lexeme, int line, int col) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
    }

    @Override
    public String toString() {
        return String.format("<%s, '%s', line %d:%d>", type, lexeme, line, col);
    }
}
