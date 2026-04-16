package core.lexer.translators;

import core.lexer.conversors.ReToAFNDE;
import core.lexer.models.atomic.Rule;
import core.lexer.models.automata.AFNDE;

public class RuleParserStandard {

    private final ReToAFNDE generator;

    private char[] input;
    private int pos;
    private int length;

    public RuleParserStandard(ReToAFNDE generator) {
        this.generator = generator;
    }

    public AFNDE parse(Rule rule) { 
        this.input = rule.getRegex().toCharArray();
        this.pos = 0;
        this.length = input.length;

        AFNDE nfa = parseExpression();

        if (pos < length) {
            throw new RuntimeException(
                "Standard Parser: Unexpected '" + input[pos] + "' at pos " + pos + " in rule " + rule.getTokenType()
            );
        }

        return generator.nameToken(rule.getTokenType(), nfa);
    }

    private AFNDE parseExpression() {
        AFNDE nfa = parseTerm();

        while (pos < length && input[pos] == '|') {
            pos++;
            AFNDE right = parseTerm();
            nfa = generator.union(nfa, right);
        }

        return nfa;
    }

    private AFNDE parseTerm() {
        AFNDE nfa = parseFactor();

        while (pos < length) {
            char c = input[pos];
            if (c == '|' || c == ')') break;

            AFNDE right = parseFactor();
            nfa = generator.concat(nfa, right);
        }

        return nfa;
    }

    private AFNDE parseFactor() {
        AFNDE nfa = parseBase();

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

    private AFNDE parseBase() {
        char c = input[pos];

        if (c == '(') {
            pos++;
            AFNDE nfa = parseExpression();
            if (input[pos] != ')') throw error(")");
            pos++;
            return nfa;
        }

        if (c == '[') {
            return parseCharacterClass();
        }

        if (c == '\\') {
            pos++;
            return generator.symbol(String.valueOf(input[pos++]));
        }

        pos++;
        return generator.symbol(String.valueOf(c));
    }

    private AFNDE parseCharacterClass() {
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

        AFNDE result = null;

        if (negate) {
            for (int c = 0; c < 128; c++) {
                if (!table[c]) {
                    result = unionFast(result, (char) c);
                }
            }
        } else {
            for (int c = 0; c < 128; c++) {
                if (table[c]) {
                    result = unionFast(result, (char) c);
                }
            }
        }

        return result != null ? result : generator.symbol("");
    }

    private char readChar() {
        if (input[pos] == '\\') {
            pos++;
        }
        return input[pos++];
    }

    private AFNDE unionFast(AFNDE acc, char c) {
        AFNDE nfa = generator.symbol(String.valueOf(c));
        return (acc == null) ? nfa : generator.union(acc, nfa);
    }

    private RuntimeException error(String expected) {
        return new RuntimeException(
            "Expected '" + expected + "' at pos " + pos
        );
    }
}