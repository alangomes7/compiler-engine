package core.lexer.core.translators;

import core.lexer.core.conversors.ReToNFAE;
import core.lexer.models.atomic.Rule;
import core.lexer.models.automata.NFAE;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for extended regular expression rules. Supports macro expansion, character classes, and
 * extended syntax features such as custom character class shorthands.
 */
public class RuleParserExtended {

    private final ReToNFAE generator;
    private char[] input;
    private int pos;
    private int length;

    public RuleParserExtended(ReToNFAE generator) {
        this.generator = generator;
    }

    public NFAE parse(Rule rule) {
        String resolvedRegex = resolveExtendedSyntax(rule.getRegex(), rule.getMacros());

        this.input = resolvedRegex.toCharArray();
        this.pos = 0;
        this.length = input.length;

        if (this.length == 0) {
            System.err.println(
                    "⚠️ Warning: Token '" + rule.getTokenType() + "' resolved to an empty regex.");
            return generator.symbol("");
        }

        NFAE nfa = parseExpression();

        if (pos < length) {
            throw new RuntimeException(
                    "Extended Parser: Unexpected '"
                            + input[pos]
                            + "' at pos "
                            + pos
                            + " in rule "
                            + rule.getTokenType()
                            + "\nResolved Regex: "
                            + resolvedRegex);
        }

        if (rule.isSkip()) {
            generator.addSkipPattern(rule.getTokenType(), nfa);
        }
        return generator.nameToken(rule.getTokenType(), nfa);
    }

    private String resolveExtendedSyntax(String pattern, Map<String, String> macros) {
        String result = pattern;

        if (macros != null && !macros.isEmpty()) {
            for (Map.Entry<String, String> m : macros.entrySet()) {
                String val = m.getValue();
                val = val.replace("[ANY_CHAR ^ [NEWLINE_CH]]", "!!NOT_NL!!");
                val = val.replace("[ANY_CHAR ^ [DQUOTE | BACKSLASH]]", "!!NOT_DQ!!");
                val = val.replace("[ANY_CHAR ^ [SQUOTE | BACKSLASH]]", "!!NOT_SQ!!");
                val = val.replace("[ANY_CHAR ^ [NEWLINE_CH | SLASH]]", "!!NOT_NL_SL!!");
                m.setValue(val);
            }

            boolean changed = true;
            while (changed) {
                changed = false;
                for (Map.Entry<String, String> m : macros.entrySet()) {
                    String regex = "\\b" + m.getKey() + "\\b";
                    Pattern p = Pattern.compile(regex);
                    Matcher matcher = p.matcher(result);
                    if (matcher.find()) {
                        result = matcher.replaceAll(Matcher.quoteReplacement(m.getValue()));
                        changed = true;
                    }
                }
            }
        }

        result = result.replace("[ANY_CHAR ^ [NEWLINE_CH]]", "!!NOT_NL!!");
        result = result.replace("[ANY_CHAR ^ [DQUOTE | BACKSLASH]]", "!!NOT_DQ!!");
        result = result.replace("[ANY_CHAR ^ [SQUOTE | BACKSLASH]]", "!!NOT_SQ!!");
        result = result.replace("[ANY_CHAR ^ [NEWLINE_CH | SLASH]]", "!!NOT_NL_SL!!");

        result = result.replace("!!NOT_NL!!", "[^\r\n]");
        result = result.replace("!!NOT_DQ!!", "[^\"\\\\]");
        result = result.replace("!!NOT_SQ!!", "[^'\\\\]");
        result = result.replace("!!NOT_NL_SL!!", "[^\r\n/]");

        result = result.replaceAll("\\s*\\|\\s*", "|");
        result = result.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
        result = result.replace("( )", "!!SPACE!!");

        StringBuilder sb = new StringBuilder();
        boolean inClass = false;
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (c == '[') inClass = true;
            else if (c == ']') inClass = false;

            if (c == ' ' && !inClass) continue;
            sb.append(c);
        }
        result = sb.toString();
        result = result.replace("!!SPACE!!", " ");

        return result;
    }

    private NFAE parseExpression() {
        NFAE nfa = parseTerm();
        while (pos < length && input[pos] == '|') {
            pos++;
            if (pos >= length) {
                throw new RuntimeException("Unexpected end of regex after '|'");
            }
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

    private NFAE parseBase() {
        if (pos >= length) throw error("character, but reached end of pattern");

        char c = input[pos];

        if (c == '(') {
            pos++;
            NFAE nfa = parseExpression();
            if (pos >= length || input[pos] != ')') throw error(")");
            pos++;
            return nfa;
        }

        if (c == '[') return parseCharacterClass();

        if (c == '\\') {
            pos++;
            if (pos >= length)
                throw new RuntimeException("Dangling escape character '\\' at end of pattern");
            return generator.symbol(String.valueOf(input[pos++]));
        }

        pos++;
        return generator.symbol(String.valueOf(c));
    }

    /** FIX 2: Added bracket depth tracking to handle nested classes seamlessly. */
    private NFAE parseCharacterClass() {
        pos++;
        if (pos >= length)
            throw new RuntimeException("Unclosed character class '[' at end of pattern");

        boolean negate = false;
        if (input[pos] == '^') {
            negate = true;
            pos++;
        }

        boolean[] table = new boolean[128];
        int depth = 1;

        while (pos < length && depth > 0) {
            if (input[pos] == '[') {
                depth++;
                pos++;
                continue;
            }
            if (input[pos] == ']') {
                depth--;
                if (depth == 0) break; // Reached the end of the outermost class
                pos++;
                continue;
            }
            if (input[pos] == '|') {
                pos++; // Skip unescaped syntactic OR operators inside the class definitions
                continue;
            }

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

        if (depth > 0) throw new RuntimeException("Unclosed character class '[' (missing ']')");
        pos++; // Consume the final closing ']'

        NFAE result = null;

        if (negate) {
            for (int c = 0; c < 128; c++) {
                if (!table[c]) result = unionFast(result, (char) c);
            }
        } else {
            for (int c = 0; c < 128; c++) {
                if (table[c]) result = unionFast(result, (char) c);
            }
        }

        return result != null ? result : generator.symbol("");
    }

    private char readChar() {
        if (pos >= length)
            throw new RuntimeException("Unexpected end of pattern while reading character");

        if (input[pos] == '\\') {
            pos++;
            if (pos >= length)
                throw new RuntimeException("Dangling escape '\\' inside character class");
        }
        return input[pos++];
    }

    private NFAE unionFast(NFAE acc, char c) {
        NFAE nfa = generator.symbol(String.valueOf(c));
        return (acc == null) ? nfa : generator.union(acc, nfa);
    }

    private RuntimeException error(String expected) {
        return new RuntimeException(
                "Expected '"
                        + expected
                        + "' at pos "
                        + pos
                        + " but found '"
                        + (pos < length ? input[pos] : "EOF")
                        + "'");
    }
}
