package models;

public class TokenRule {
    private final String tokenType;
    private final String regex;

    public TokenRule(String tokenType, String regex) {
        this.tokenType = tokenType;
        this.regex = regex;
    }

    public String getTokenType() { return tokenType; }
    public String getRegex() { return regex; }
}