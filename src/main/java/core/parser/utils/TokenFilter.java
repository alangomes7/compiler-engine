package core.parser.utils;

import core.lexer.models.atomic.Token;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Utility class to filter out tokens that should be ignored by the syntax analyzer. */
public class TokenFilter {

    private static final Set<String> IGNORED_TOKEN_TYPES =
            Set.of("WHITESPACE", "NEWLINE_CH", "NOT_NEWLINE", "SPACE", "TAB", "newline");

    /**
     * Filters the input token stream, removing tokens that the parser does not need.
     *
     * @param tokens The original list of tokens from the lexer.
     * @return A new list containing only the relevant tokens for parsing.
     */
    public static List<Token> filter(List<Token> tokens) {
        if (tokens == null) {
            return null;
        }

        return tokens.stream()
                .filter(token -> !IGNORED_TOKEN_TYPES.contains(token.getType()))
                .collect(Collectors.toList());
    }
}
