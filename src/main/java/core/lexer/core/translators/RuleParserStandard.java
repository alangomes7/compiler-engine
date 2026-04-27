package core.lexer.core.translators;

import core.lexer.core.conversors.ReToNFAE;
import core.lexer.models.atomic.Rule;
import core.lexer.models.automata.NFAE;

/**
 * Parser for standard (non‑extended) regular expression rules. Supports concatenation, alternation
 * ('|'), grouping '()', quantifiers ('*', '+', '?'), character classes, and escaped control
 * characters.
 *
 * @author Generated
 * @version 1.1
 */
public class RuleParserStandard {

    private final ReToNFAE generator;

    private char[] input;
    private int pos;
    private int length;

    /**
     * Constructs a standard rule parser with the given NFA-ε generator.
     *
     * @param generator the converter used to build NFA-ε fragments
     */
    public RuleParserStandard(ReToNFAE generator) {
        this.generator = generator;
    }

    /**
     * Parses a standard (non‑extended) rule into an NFA-ε.
     *
     * @param rule the rule to parse (extended flag must be false)
     * @return the NFA-ε representing the rule's regular expression
     * @throws RuntimeException if the regex contains syntax errors
     */
    public NFAE parse(Rule rule) {
        this.input = rule.getRegex().toCharArray();
        this.pos = 0;
        this.length = input.length;

        NFAE nfa = parseExpression();

        if (pos < length) {
            throw new RuntimeException(
                    "Standard Parser: Unexpected '"
                            + input[pos]
                            + "' at pos "
                            + pos
                            + " in rule "
                            + rule.getTokenType());
        }

        if (rule.isSkip()) {
            generator.addSkipPattern(rule.getTokenType(), nfa);
        }
        return generator.nameToken(rule.getTokenType(), nfa);
    }

    private NFAE parseExpression() {
        NFAE nfa = parseTerm();
        while (pos < length && input[pos] == '|') {
            pos++;
            NFAE right = parseTerm();
            nfa = generator.union(nfa, right);
        }
        return nfa;
    }

    private NFAE parseTerm() {
        NFAE nfa = parseFactor();
        while (pos < length) {
            char c = input[pos];
            if (c == '|' || c == ')') break;
            NFAE right = parseFactor();
            nfa = generator.concat(nfa, right);
        }
        return nfa;
    }

    private NFAE parseFactor() {
        NFAE nfa = parseBase();
        OUTER:
        while (pos < length) {
            char c = input[pos];
            switch (c) {
                case '*' -> {
                    pos++;
                    nfa = generator.kleeneStar(nfa);
                }
                case '+' -> {
                    pos++;
                    nfa = generator.oneOrMore(nfa);
                }
                case '?' -> {
                    pos++;
                    nfa = generator.optional(nfa);
                }
                default -> {
                    break OUTER;
                }
            }
        }
        return nfa;
    }

    /**
     * Parses a base atom:
     *
     * <ul>
     *   <li>grouped expression (parentheses)
     *   <li>character class (brackets)
     *   <li>escaped character (backslash mapped to control chars)
     *   <li>literal character
     * </ul>
     *
     * @return NFA-ε for the parsed base
     * @throws RuntimeException on syntax errors
     */
    private NFAE parseBase() {
        char c = input[pos];

        if (c == '(') {
            pos++;
            NFAE nfa = parseExpression();
            if (input[pos] != ')') throw error(")");
            pos++;
            return nfa;
        }

        if (c == '[') {
            return parseCharacterClass();
        }

        if (c == '\\') {
            pos++;
            if (pos >= length) throw new RuntimeException("Dangling escape at pos " + pos);
            char escaped = input[pos++];
            // Fix: Translate known escape sequences
            String sym =
                    switch (escaped) {
                        case 'n' -> "\n";
                        case 'r' -> "\r";
                        case 't' -> "\t";
                        case 's' -> " ";
                        default -> String.valueOf(escaped);
                    };
            return generator.symbol(sym);
        }

        pos++;
        return generator.symbol(String.valueOf(c));
    }

    /**
     * Parses a character class, e.g., [abc], [^0-9]. Supports ranges (a-z) and escaping inside the
     * class.
     *
     * @return NFA-ε for the character class
     */
    private NFAE parseCharacterClass() {
        pos++;

        boolean negate = false;
        if (input[pos] == '^') {
            negate = true;
            pos++;
        }

        boolean[] table = new boolean[128];

        while (pos < length && input[pos] != ']') {
            char start = readChar();

            if (pos < length - 1 && input[pos] == '-' && input[pos + 1] != ']') {
                pos++;
                char end = readChar();

                for (int c = start; c <= end; c++) {
                    if (c < 128) table[c] = true;
                }
            } else {
                if (start < 128) table[start] = true;
            }
        }

        pos++;
        NFAE result = null;

        for (int c = 0; c < 128; c++) {
            if (negate != table[c]) {
                result = unionFast(result, (char) c);
            }
        }

        return result != null ? result : generator.symbol("");
    }

    /**
     * Reads a single character from the input, correctly mapping escape sequences.
     *
     * @return the character read (after unescaping)
     * @throws RuntimeException if end of input is reached unexpectedly
     */
    private char readChar() {
        if (input[pos] == '\\') {
            pos++;
            if (pos >= length) throw new RuntimeException("Dangling escape at pos " + pos);
            char c = input[pos++];
            // Fix: Map the literal character correctly
            return switch (c) {
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 's' -> ' ';
                default -> c;
            };
        }
        return input[pos++];
    }

    private NFAE unionFast(NFAE acc, char c) {
        NFAE nfa = generator.symbol(String.valueOf(c));
        return (acc == null) ? nfa : generator.union(acc, nfa);
    }

    private RuntimeException error(String expected) {
        return new RuntimeException("Expected '" + expected + "' at pos " + pos);
    }
}
