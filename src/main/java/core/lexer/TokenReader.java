package core.lexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import models.TokenRule;

public class TokenReader {

    public static List<TokenRule> readTokens(String filePath) {
        List<String> lines;

        try {
            lines = Files.readAllLines(Path.of(filePath));
        } catch (IOException e) {
            System.err.println("❌ Error reading token file: " + filePath);
            System.err.println("   Reason: " + e.getMessage());
             e.printStackTrace();
            return List.of(); // safe fallback
        }

        if (lines.isEmpty()) {
            System.out.println("⚠️ Token file is empty: " + filePath);
            return List.of();
        }

        List<TokenRule> rules = new ArrayList<>();
        boolean isDynamicSection = false;
        int lineNumber = 0;

        for (String rawLine : lines) {
            lineNumber++;
            String line = rawLine.trim();

            if (line.isEmpty()) continue;

            // Handle comments and section switching
            if (line.startsWith("#")) {
                if (line.toLowerCase().contains("dynamic")) {
                    isDynamicSection = true;
                }
                continue;
            }

            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                System.err.println("⚠️ Invalid rule format at line " + lineNumber + ": " + rawLine);
                continue;
            }

            String tokenName = parts[0].trim();
            String rightSide = parts[1].trim();

            boolean skip = false;

            // Handle -> skip
            if (rightSide.contains("->")) {
                String[] split = rightSide.split("->", 2);
                rightSide = split[0].trim();

                if (split[1].trim().equalsIgnoreCase("skip")) {
                    skip = true;
                } else {
                    System.err.println("⚠️ Unknown action at line " + lineNumber + ": " + split[1]);
                }
            }

            if (tokenName.isEmpty() || rightSide.isEmpty()) {
                System.err.println("⚠️ Empty token or pattern at line " + lineNumber + ": " + rawLine);
                continue;
            }

            String pattern = processPattern(rightSide, isDynamicSection);

            rules.add(new TokenRule(tokenName, pattern, skip));
        }

        if (rules.isEmpty()) {
            System.err.println("⚠️ No valid token rules were loaded from: " + filePath);
        } else {
            System.out.println("✅ Loaded " + rules.size() + " token rules.");
        }

        return rules;
    }

    private static String processPattern(String pattern, boolean isDynamic) {
        if (isDynamic) {
            return pattern
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }
        return escapeLiteral(pattern);
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