package models.atomic;

import java.util.Objects;

public class Symbol {
    private String lexeme;
    private String tokenType;
    private int line;
    private int col;

    // Constructor explicitly saves the line and col
    public Symbol(String lexeme, String tokenType, int line, int col) {
        this.lexeme = lexeme;
        this.tokenType = tokenType;
        this.line = line;
        this.col = col;
    }

    // --- Getters and Setters ---
    public String getLexeme() { return lexeme; }
    public void setLexeme(String lexeme) { this.lexeme = lexeme; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }

    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }

    // --- CRITICAL FIX FOR HASHMAP LOOKUPS ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        // Two symbols are the same if their lexemes match
        return Objects.equals(lexeme, symbol.lexeme);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lexeme);
    }

    @Override
    public String toString() {
        return lexeme;
    }
}