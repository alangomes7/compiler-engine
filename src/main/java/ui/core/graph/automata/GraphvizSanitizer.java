package ui.core.graph.automata;

import core.lexer.models.atomic.Transition;
import models.atomic.Constants;

/**
 * Provides escaping and sanitisation for strings used as Graphviz label attributes.
 *
 * <p>Graphviz uses HTML-like syntax for rich labels, which conflicts with special characters that
 * may appear in transition symbols. This utility ensures that any string can be safely embedded in
 * a Graphviz DOT file without breaking the syntax.
 *
 * <p>Common problematic cases handled:
 *
 * <ul>
 *   <li>Angle brackets (&lt; &gt;) - used in HTML tags
 *   <li>Ampersands (&amp;) - used for HTML entities
 *   <li>Quotes and backslashes - used for string delimiters
 *   <li>Whitespace characters - normalized to visible representations
 *   <li>Control characters - replaced with hexadecimal escapes
 * </ul>
 *
 * <p>The sanitized output is wrapped in angle brackets to create an HTML-like label that supports
 * proper formatting.
 *
 * <p>Typical usage:
 *
 * <pre>
 * String safe = GraphvizSanitizer.sanitizeLabel("if (x > 10)");
 * // Returns: "&lt;if&nbsp;(x&nbsp;&gt;&nbsp;10)&gt;"
 * </pre>
 *
 * @see AutomataVisualizer
 */
public class GraphvizSanitizer {

    /**
     * Escapes a symbol string so that it can be safely placed inside a Graphviz HTML‑like label.
     *
     * <p>Special characters are replaced with HTML entities or escape sequences:
     *
     * <ul>
     *   <li>&amp; → {@code &amp;amp;}
     *   <li>&lt; → {@code &amp;lt;}
     *   <li>&gt; → {@code &amp;gt;}
     *   <li>" → {@code &amp;#34;}
     *   <li>' → {@code &amp;#39;}
     *   <li>\ → {@code &amp;#92;}
     *   <li>newline → {@code &amp;#92;n}
     *   <li>carriage return → {@code &amp;#92;r}
     *   <li>tab → {@code &amp;#92;t}
     *   <li>space → {@code &amp;nbsp;}
     * </ul>
     *
     * <p>If the symbol is null or empty, returns {@code <ε>} to represent epsilon transitions.
     *
     * @param symbol the raw symbol string (may be null)
     * @return a safe label suitable for use in a DOT edge label, enclosed in {@code <...>}
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
     * <p>This method extracts the symbol value from the transition and passes it through {@link
     * #sanitizeLabel(String)}.
     *
     * @param transition the transition whose symbol should be sanitised; must not be null
     * @return the sanitised label string (enclosed in angle brackets)
     * @throws NullPointerException if transition is null
     */
    public static String getSafeLabel(Transition transition) {
        return sanitizeLabel(transition.getSymbol().getValue());
    }
}
