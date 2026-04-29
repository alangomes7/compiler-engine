package core.parser.core.grammar;

import core.parser.models.Grammar;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GrammarReader {

    public static Grammar readFromFile(String filePath) throws IOException {
        Grammar grammar = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            String currentLhs = null;
            StringBuilder currentRhs = new StringBuilder();
            StringBuilder pendingLhs = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = stripComments(line).trim();

                if (line.isEmpty()) {
                    if (currentLhs != null) {
                        if (grammar == null) {
                            grammar = new Grammar(new Symbol(currentLhs, false));
                        }
                        addProductions(grammar, currentLhs, currentRhs.toString());
                        currentLhs = null;
                        currentRhs.setLength(0);
                    }
                    continue;
                }

                if (line.contains("::=")) {
                    if (currentLhs != null) {
                        if (grammar == null) {
                            grammar = new Grammar(new Symbol(currentLhs, false));
                        }
                        addProductions(grammar, currentLhs, currentRhs.toString());
                    }

                    String[] parts = line.split("::=", 2);
                    String lhsCandidate = parts[0].trim();

                    if (lhsCandidate.isEmpty()) {
                        lhsCandidate = pendingLhs.toString().trim();
                    }

                    if (lhsCandidate.isEmpty()) {
                        currentLhs = null;
                        currentRhs.setLength(0);
                        pendingLhs.setLength(0);
                        continue;
                    }

                    currentLhs = lhsCandidate;
                    currentRhs = new StringBuilder(parts[1].trim());
                    pendingLhs.setLength(0);

                } else if (currentLhs != null) {
                    currentRhs.append(" ").append(line);
                } else {
                    if (pendingLhs.length() > 0) {
                        pendingLhs.append(" ");
                    }
                    pendingLhs.append(line);
                }
            }

            if (currentLhs != null) {
                if (grammar == null) {
                    grammar = new Grammar(new Symbol(currentLhs, false));
                }
                addProductions(grammar, currentLhs, currentRhs.toString());
            }
        }

        if (grammar == null) {
            throw new IllegalArgumentException(
                    "Grammar file is empty or contains no valid productions.");
        }

        return grammar;
    }

    private static void addProductions(Grammar grammar, String lhsName, String rhsFull) {
        if (lhsName.isEmpty()) return;
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

    private static List<Symbol> tokenize(String input) {
        List<Symbol> symbols = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

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

            if ("(){}[]?,:+*/%^=!<>|".indexOf(c) >= 0) {
                flushBuffer(buffer, symbols);
                symbols.add(new Symbol(String.valueOf(c), true));
                continue;
            }

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

        if (token.equals("ε") || token.equals("EPSILON")) {
            symbols.add(Symbol.EPSILON);
        } else {
            boolean isTerminal = isTerminal(token);
            symbols.add(new Symbol(token, isTerminal));
        }

        buffer.setLength(0);
    }

    private static boolean isTerminal(String token) {
        if (token.startsWith("\"") || token.startsWith("'")) return true;
        if (token.matches("[0-9]+(\\.[0-9]+)?")) return true;
        return !token.matches("[a-zA-Z_].*");
    }

    private static String stripQuotes(String s) {
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String stripComments(String line) {
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if ((c == '"' || c == '\'') && (quoteChar == 0 || c == quoteChar)) {
                inQuotes = !inQuotes;
                quoteChar = inQuotes ? c : 0;
            }

            if (!inQuotes && c == '#') {
                return line.substring(0, i);
            }
        }
        return line;
    }
}
