package core.lexer.models.atomic;

import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Rule {
    private final String tokenType;
    private final String regex;

    @Getter(AccessLevel.NONE)
    private final boolean skip;

    @Getter(AccessLevel.NONE)
    private final boolean extended;

    private final Map<String, String> macros;

    public Rule(
            String tokenType,
            String regex,
            boolean skip,
            boolean extended,
            Map<String, String> macros) {
        this.tokenType = tokenType;
        this.regex = regex;
        this.skip = skip;
        this.extended = extended;
        this.macros = macros != null ? new HashMap<>(macros) : new HashMap<>();
    }

    public boolean isSkip() {
        return skip;
    }

    public boolean isExtended() {
        return extended;
    }

    @Override
    public String toString() {
        return "TokenRule{type='" + tokenType + "', regex='" + regex + "', skip=" + skip + "}";
    }
}
