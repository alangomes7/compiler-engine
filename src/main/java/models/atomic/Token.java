package models.atomic;

import java.util.HashMap;
import java.util.Map;

public class Token {
    private final String tokenType;
    private final String regex;
    private final boolean skip;
    private final boolean extended;
    private final Map<String, String> macros;

    public Token(String tokenType, String regex, boolean skip, boolean extended, Map<String, String> macros) {
        this.tokenType = tokenType;
        this.regex = regex;
        this.skip = skip;
        this.extended = extended;
        this.macros = macros != null ? new HashMap<>(macros) : new HashMap<>();
    }

    public boolean isExtended() {
        return extended;
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

    public Map<String, String> getMacros() {
        return macros;
    }

    @Override
    public String toString() {
        return "TokenRule{" +
                "type='" + tokenType + '\'' +
                ", regex='" + regex + '\'' +
                ", skip=" + skip +
                ", extended=" + extended +
                '}';
    }
}