package ui.core.graph.automata;

import core.lexer.models.atomic.Transition;
import models.atomic.Constants;

public class GraphvizSanitizer {

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

    public static String getSafeLabel(Transition transition) {
        return sanitizeLabel(transition.getSymbol().getValue());
    }
}
