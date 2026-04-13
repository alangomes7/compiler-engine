package scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import models.TokenRule;

public class TokenReader {

    public static List<TokenRule> readTokens(String filePath) throws IOException {
        List<TokenRule> rules = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of(filePath));

        boolean isDynamicSection = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            // Skip empty lines
            if (line.isEmpty()) continue;

            // Section detection and full-line comments
            if (line.startsWith("#")) {
                String lower = line.toLowerCase();
                if (lower.contains("dynamic")) {
                    isDynamicSection = true;
                }
                continue;
            }

            // Split rule: TOKEN : pattern [-> skip]
            String[] parts = line.split(":", 2);
            if (parts.length != 2) continue;

            String tokenName = parts[0].trim();
            String rightSide = parts[1].trim();

            boolean skip = false;

            // Handle skip directive
            if (rightSide.contains("->")) {
                String[] split = rightSide.split("->");
                rightSide = split[0].trim();
                String action = split[1].trim();

                if (action.equalsIgnoreCase("skip")) {
                    skip = true;
                }
            }

            String pattern = rightSide;

            // Escape ONLY static literals
            if (!isDynamicSection) {
                pattern = escapeLiteral(pattern);
            } else {
                pattern = pattern.replace("\\n", "\n")
                                 .replace("\\r", "\r")
                                 .replace("\\t", "\t");
            }

            rules.add(new TokenRule(tokenName, pattern, skip));
        }

        return rules;
    }

    private static String escapeLiteral(String literal) {
        StringBuilder sb = new StringBuilder();
        String specialChars = "|()*+?[]{}.^$\\-";

        for (char c : literal.toCharArray()) {
            if (specialChars.indexOf(c) != -1) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}