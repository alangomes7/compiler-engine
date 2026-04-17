package core.parser.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import core.parser.models.Grammar;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;

/**
 * Reads a grammar file (supports multi-line productions and EBNF-like syntax).
 */
public class GrammarReader {

    public static Grammar readFromFile(String filePath, String startSymbolName) throws IOException {
        Symbol startSymbol = new Symbol(startSymbolName, false);
        Grammar grammar = new Grammar(startSymbol);

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            String currentLhs = null;
            StringBuilder currentRhs = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = stripComments(line).trim();
                if (line.isEmpty()) continue;

                if (line.contains("::=")) {
                    // Flush previous production
                    if (currentLhs != null) {
                        addProductions(grammar, currentLhs, currentRhs.toString());
                    }

                    String[] parts = line.split("::=", 2);
                    currentLhs = parts[0].trim();
                    currentRhs = new StringBuilder(parts[1].trim());

                } else if (currentLhs != null) {
                    // Continuation line
                    currentRhs.append(" ").append(line.trim());
                }
            }

            // Flush last production
            if (currentLhs != null) {
                addProductions(grammar, currentLhs, currentRhs.toString());
            }
        }

        return grammar;
    }

    private static void addProductions(Grammar grammar, String lhsName, String rhsFull) {
        Symbol lhs = new Symbol(lhsName, false);

        String[] alternatives = splitAlternatives(rhsFull);

        for (String alt : alternatives) {
            List<Symbol> rhs = tokenize(alt.trim());
            if (rhs.isEmpty()) {
                rhs.add(Symbol.EPSILON);
            }
            grammar.addProduction(new Production(lhs, rhs));
        }
    }

    /**
     * Splits RHS by '|' but ignores pipes inside quotes.
     */
    private static String[] splitAlternatives(String rhs) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inQuotes = false;
        char quoteChar = 0;

        for (char c : rhs.toCharArray()) {
            if ((c == '"' || c == '\'') && (quoteChar == 0 || c == quoteChar)) {
                inQuotes = !inQuotes;
                quoteChar = inQuotes ? c : 0;
            }

            if (c == '|' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());
        return result.toArray(String[]::new);
    }

    /**
     * Tokenizer that preserves:
     * - quoted strings
     * - symbols like (), {}, [], ?, *, +
     */
    private static List<Symbol> tokenize(String input) {
        List<Symbol> symbols = new ArrayList<>();

        StringBuilder buffer = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            // Handle quotes
            if ((c == '"' || c == '\'') && (quoteChar == 0 || c == quoteChar)) {
                if (inQuotes) {
                    buffer.append(c);
                    symbols.add(new Symbol(stripQuotes(buffer.toString()), true));
                    buffer.setLength(0);
                    inQuotes = false;
                    quoteChar = 0;
                } else {
                    flushBuffer(buffer, symbols);
                    inQuotes = true;
                    quoteChar = c;
                    buffer.append(c);
                }
                continue;
            }

            if (inQuotes) {
                buffer.append(c);
                continue;
            }

            // Special symbols as standalone tokens
            if ("(){}[]?,:+-*/%^=!<>".indexOf(c) >= 0) {
                flushBuffer(buffer, symbols);
                symbols.add(new Symbol(String.valueOf(c), true));
                continue;
            }

            // Whitespace
            if (Character.isWhitespace(c)) {
                flushBuffer(buffer, symbols);
                continue;
            }

            buffer.append(c);
        }

        flushBuffer(buffer, symbols);
        return symbols;
    }

    private static void flushBuffer(StringBuilder buffer, List<Symbol> symbols) {
        if (buffer.length() == 0) return;

        String token = buffer.toString();
        boolean isTerminal = isTerminal(token);

        symbols.add(new Symbol(token, isTerminal));
        buffer.setLength(0);
    }

    private static boolean isTerminal(String token) {
        return token.startsWith("\"") ||
               token.startsWith("'") ||
               token.matches("[^a-zA-Z].*");
    }

    private static String stripQuotes(String s) {
        if ((s.startsWith("\"") && s.endsWith("\"")) ||
            (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String stripComments(String line) {
        int hash = line.indexOf('#');
        int semi = line.indexOf(';');

        int cut = -1;
        if (hash >= 0) cut = hash;
        if (semi >= 0) cut = (cut == -1) ? semi : Math.min(cut, semi);

        return (cut >= 0) ? line.substring(0, cut) : line;
    }
}