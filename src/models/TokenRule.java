package models;

public class TokenRule {
    private final String tokenType;
    private final String regex;
    private final boolean skip;

    /**
     * Backward-compatible constructor (default: not skipped)
     */
    public TokenRule(String tokenType, String regex) {
        this(tokenType, regex, false);
    }

    /**
     * New constructor with skip support
     */
    public TokenRule(String tokenType, String regex, boolean skip) {
        this.tokenType = tokenType;
        this.regex = regex;
        this.skip = skip;
    }

    public String getTokenType() { 
        return tokenType; 
    }

    public String getRegex() { 
        return regex; 
    }

    public boolean isSkip() { 
        return skip; 
    }

    @Override
    public String toString() {
        return "TokenRule{" +
                "type='" + tokenType + '\'' +
                ", regex='" + regex + '\'' +
                ", skip=" + skip +
                '}';
    }
}