package core.parser.utils;

import core.lexer.models.atomic.Token;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Utility class to filter out tokens that should be ignored by the syntax analyzer. */
public class TokenFilter {

    private static final Set<String> DEFAULT_IGNORED_TOKEN_TYPES =
            Set.of("__SKIP__", "WHITESPACE", "SPACE", "TAB", "comment");

    // Now a mutable HashSet to allow dynamic additions
    private final Set<String> ignoredTokenTypes;

    /** Initializes the TokenFilter with the default set of ignored tokens. */
    public TokenFilter() {
        this.ignoredTokenTypes = new HashSet<>(DEFAULT_IGNORED_TOKEN_TYPES);
    }

    /**
     * Adds a new token type to the skip list.
     *
     * @param tokenType The type of token to ignore (e.g., "COMMENT").
     */
    public void addSkipToken(String tokenType) {
        if (tokenType != null && !tokenType.trim().isEmpty()) {
            this.ignoredTokenTypes.add(tokenType);
        }
    }

    /**
     * Filters the input token stream, removing tokens that the parser does not need.
     *
     * @param tokens The original list of tokens from the lexer.
     * @return A new list containing only the relevant tokens for parsing.
     */
    public List<Token> filter(List<Token> tokens) {
        if (tokens == null) {
            return null; // Or return Collections.emptyList() depending on your project's
            // null-handling conventions
        }

        return tokens.stream()
                .filter(token -> token != null && !this.ignoredTokenTypes.contains(token.getType()))
                .collect(Collectors.toList());
    }
}
