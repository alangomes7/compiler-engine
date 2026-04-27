package core.lexer.core.translators;

import core.lexer.models.atomic.Rule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads lexical rule definitions from a file and parses them into {@link Rule} objects. */
public class RuleReader {

    public static List<Rule> readRules(String filePath) {
        List<String> lines;

        try {
            lines = Files.readAllLines(Path.of(filePath));
        } catch (IOException e) {
            System.err.println("❌ Error reading token file: " + filePath);
            return List.of();
        }

        if (lines.isEmpty()) return List.of();

        List<String> joinedLines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        // Join multi-line definitions
        for (String rawLine : lines) {
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("==")) continue;

            if (trimmed.startsWith("#")) {
                if (current.length() > 0) {
                    joinedLines.add(current.toString());
                    current.setLength(0);
                }
                joinedLines.add(trimmed);
                continue;
            }

            if (trimmed.contains(":")) {
                if (current.length() > 0) joinedLines.add(current.toString());
                current = new StringBuilder(trimmed);
            } else {
                current.append(" ").append(trimmed);
            }
        }
        if (current.length() > 0) joinedLines.add(current.toString());

        List<Rule> rules = new ArrayList<>();
        Map<String, String> macros = new HashMap<>();

        boolean isDynamicSection = false;
        boolean isMacroSection = false;

        for (String line : joinedLines) {
            String lower = line.toLowerCase();

            // Track sections to determine if characters should be escaped
            if (line.startsWith("#") || !line.contains(":")) {
                if (lower.contains("dynamic")
                        || lower.contains("lexical elements")
                        || lower.contains("ignored")) {
                    isDynamicSection = true;
                    isMacroSection = false;
                } else if (lower.contains("primitive sets")
                        || lower.contains("derived sets")
                        || lower.contains("escape handling")) {
                    isDynamicSection = true;
                    isMacroSection = true;
                } else if (lower.contains("keywords")
                        || lower.contains("operators")
                        || lower.contains("delimiters")) {
                    isDynamicSection = false;
                    isMacroSection = false;
                }
                continue;
            }

            String[] parts = line.split(":", 2);
            if (parts.length != 2) continue;

            String tokenName = parts[0].trim();
            String rightSide = parts[1].trim();
            boolean skip = false;
            boolean isExtendedRule = false;

            // Extract tags
            if (tokenName.startsWith("@")) {
                String[] tokenParts = tokenName.split("\\s+", 2);
                if (tokenParts.length == 2) {
                    String tag = tokenParts[0].toUpperCase();
                    tokenName = tokenParts[1].trim();

                    if (tag.equals("@PRI") || tag.equals("@DER")) {
                        isExtendedRule = true;
                    } else if (tag.equals("@ESP")) {
                        isExtendedRule = false;
                    }
                }
            } else if (isMacroSection) {
                isExtendedRule = true;
            }

            // Check for skip directive
            if (rightSide.contains("->")) {
                String[] split = rightSide.split("->", 2);
                rightSide = split[0].trim();
                if (split[1].trim().equalsIgnoreCase("skip")) skip = true;
            }

            if (tokenName.isEmpty() || rightSide.isEmpty()) continue;

            // FIX 1: Only bypass escaping if the section is explicitly dynamic.
            // Operators and Keywords tagged with @PRI still require escaping.
            String pattern = isDynamicSection ? rightSide : escapeLiteral(rightSide);

            if (isExtendedRule || isMacroSection) {
                macros.put(tokenName, pattern);
            }

            rules.add(new Rule(tokenName, pattern, skip, isExtendedRule, macros));
        }

        return rules;
    }

    private static String escapeLiteral(String literal) {
        StringBuilder sb = new StringBuilder();
        String specialChars = "|()*+?[]{}.^$\\-";
        for (char c : literal.toCharArray()) {
            if (specialChars.indexOf(c) != -1) sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }
}
