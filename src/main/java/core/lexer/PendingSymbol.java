package core.lexer;

class PendingSymbol {
    final String lexeme;
    final String tokenType;
    final int line;
    final int col;

    PendingSymbol(String lexeme, String tokenType, int line, int col) {
        this.lexeme = lexeme;
        this.tokenType = tokenType;
        this.line = line;
        this.col = col;
    }
}
