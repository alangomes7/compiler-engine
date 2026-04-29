package core.parser.utils;

import core.lexer.models.atomic.Token;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TokenFilter {

    private static final Set<String> DEFAULT_IGNORED_TOKEN_TYPES =
            Set.of("__SKIP__", "WHITESPACE", "SPACE", "TAB", "comment");

    private final Set<String> ignoredTokenTypes;

    public TokenFilter() {
        this.ignoredTokenTypes = new HashSet<>(DEFAULT_IGNORED_TOKEN_TYPES);
    }

    public void addSkipToken(String tokenType) {
        if (tokenType != null && !tokenType.trim().isEmpty()) {
            this.ignoredTokenTypes.add(tokenType);
        }
    }

    public List<Token> filter(List<Token> tokens) {
        if (tokens == null) {
            return null;
        }

        return tokens.stream()
                .filter(token -> token != null && !this.ignoredTokenTypes.contains(token.getType()))
                .collect(Collectors.toList());
    }
}
