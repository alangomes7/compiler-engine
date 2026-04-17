package ui.core.graph;

import core.lexer.models.atomic.Transition;
import models.atomic.Constants;

public class GraphvizSanitizer {

    public static String sanitizeLabel(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            // Return Graphviz HTML label format <ε>
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
                    // Trap raw control characters (ASCII 0-31 or 127)
                    if (c < 32 || c == 127) {
                        safe.append(String.format("&#92;x%02X", (int) c));
                    } else {
                        safe.append(c);
                    }
                }
            }
            // Escape XML-sensitive characters
            // Convert problem characters into safe HTML entities
            // Visually print structural whitespace so you can see it on the arrow
                    }
        
        // Wrap in < and > to force Graphviz to treat this as an HTML Label.
        // This entirely avoids the buggy "..." string parser.
        return "<" + safe.toString() + ">";
    }

    public static String getSafeLabel(Transition transition) {
        return sanitizeLabel(transition.getSymbol().getValue());
    }
}