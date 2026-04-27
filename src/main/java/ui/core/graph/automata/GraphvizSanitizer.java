package ui.core.graph.automata;

import core.lexer.models.atomic.Transition;
import models.atomic.Constants;

/**
 * Provides escaping and sanitisation for strings used as Graphviz label attributes. Prevents
 * special characters (e.g., angle brackets, ampersands) from breaking the DOT syntax and ensures
 * proper HTML‑like encoding for complex labels.
 *
 * @author Generated
 * @version 1.0
 */
public class GraphvizSanitizer {

    /**
     * Escapes a symbol string so that it can be safely placed inside a Graphviz HTML‑like label.
     * Special characters are replaced with HTML entities or escape sequences. If the symbol is null
     * or empty, returns {@code <ε>}.
     *
     * @param symbol the raw symbol string
     * @return a safe label suitable for use in a DOT edge label (enclosed in {@code <...>})
     */
    public static String sanitizeLabel(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return "<" + Constants.EPSILON + ">";
        }

        StringBuilder safe = new StringBuilder();
        for (char c : symbol.toCharArray()) {
            switch (c) {
                case '&' -> safe.append("&amp;");
                case '<' -> safe.append("&lt;");
                case '>' -> safe.append("&gt;");
                case '\"' -> safe.append("&#34;");
                case '\'' -> safe.append("&#39;");
                case '\\' -> safe.append("&#92;");
                case '\n' -> safe.append("&#92;n");
                case '\r' -> safe.append("&#92;r");
                case '\t' -> safe.append("&#92;t");
                case ' ' -> safe.append("&nbsp;");
                default -> {
                    if (c < 32 || c == 127) {
                        safe.append(String.format("&#92;x%02X", (int) c));
                    } else {
                        safe.append(c);
                    }
                }
            }
        }
        return "<" + safe.toString() + ">";
    }

    /**
     * Convenience method to obtain a safe label from a transition.
     *
     * @param transition the transition whose symbol should be sanitised
     * @return the sanitised label string (enclosed in angle brackets)
     */
    public static String getSafeLabel(Transition transition) {
        return sanitizeLabel(transition.getSymbol().getValue());
    }
}
