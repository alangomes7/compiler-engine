package core.lexer.models.atomic;

import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Represents a lexical rule that associates a regular expression pattern with a token type. Rules
 * can be marked as "skip" (ignored during lexing) or "extended" (enables extended regex syntax).
 * Macros can be used to substitute predefined patterns inside the regex.
 *
 * @author Generated
 * @version 1.0
 */
@Getter
public class Rule {
    private final String tokenType;
    private final String regex;

    @Getter(AccessLevel.NONE)
    private final boolean skip;

    @Getter(AccessLevel.NONE)
    private final boolean extended;

    private final Map<String, String> macros;

    /**
     * Constructs a new Rule.
     *
     * @param tokenType the type of token produced when this rule matches
     * @param regex the regular expression pattern to match
     * @param skip if true, matched tokens are ignored (not emitted)
     * @param extended if true, extended regex syntax is enabled
     * @param macros a map of macro names to their expansions; may be null
     */
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

    /**
     * Returns whether this rule's matched tokens should be skipped.
     *
     * @return true if the token should be ignored, false otherwise
     */
    public boolean isSkip() {
        return skip;
    }

    /**
     * Returns whether extended regex syntax is enabled for this rule.
     *
     * @return true if extended syntax is enabled, false otherwise
     */
    public boolean isExtended() {
        return extended;
    }

    /**
     * Returns a string representation of this rule.
     *
     * @return a string containing token type, regex, and skip flag
     */
    @Override
    public String toString() {
        return "TokenRule{type='" + tokenType + "', regex='" + regex + "', skip=" + skip + "}";
    }
}
