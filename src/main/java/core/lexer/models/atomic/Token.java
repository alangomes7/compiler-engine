package core.lexer.models.atomic;

public class Token {
    private final String tokenType; 
    private final String lexeme;
    private final int line;
    private final int col;

    public Token(String tokenType, String lexeme, int line, int col) {
        this.tokenType = tokenType;
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
    }

    public String getTokenType() { return tokenType; }
    public String getLexeme() { return lexeme; }
    public int getLine() { return line; }
    public int getCol() { return col; }

    @Override
    public String toString() {
        return String.format("<%s, '%s', line %d:%d>", tokenType, lexeme, line, col);
    }
}