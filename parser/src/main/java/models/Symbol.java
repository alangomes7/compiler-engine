package models;

public class Symbol {
    private final String lexeme;
    private String tokenType;
    // You can add more fields here later for the semantic analyzer 
    // (e.g., dataType, scope, memoryAddress, value)

    public Symbol(String lexeme, String tokenType) {
        this.lexeme = lexeme;
        this.tokenType = tokenType;
    }

    public String getLexeme() { return lexeme; }
    
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    @Override
    public String toString() {
        return String.format("Symbol{lexeme='%s', tokenType='%s'}", lexeme, tokenType);
    }
}