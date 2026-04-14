package core.lexer.translators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.atomic.Token;

public class TokenReader {

    public static List<Token> readTokens(String filePath) {
        List<String> lines;

        try {
            lines = Files.readAllLines(Path.of(filePath));
        } catch (IOException e) {
            System.err.println("❌ Error reading token file: " + filePath);
            System.err.println("   Reason: " + e.getMessage());
            return List.of(); 
        }

        if (lines.isEmpty()) {
            System.out.println("⚠️ Token file is empty: " + filePath);
            return List.of();
        }

        // 1. Reconstruct multi-line rule definitions
        List<String> joinedLines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
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

            // Group rules broken across multiple lines
            if (trimmed.contains(":")) {
                if (current.length() > 0) joinedLines.add(current.toString());
                current = new StringBuilder(trimmed);
            } else {
                current.append(" ").append(trimmed);
            }
        }
        if (current.length() > 0) joinedLines.add(current.toString());

        List<Token> rules = new ArrayList<>();
        Map<String, String> macros = new HashMap<>();

        boolean isDynamicSection = false;
        boolean isMacroSection = false;

        for (String line : joinedLines) {
            String lower = line.toLowerCase();
            
            // 2. Section routing (Supports # comments and un-commented headers)
            if (line.startsWith("#") || !line.contains(":")) {
                if (lower.contains("dynamic") || lower.contains("lexical elements")) {
                    isDynamicSection = true;
                    isMacroSection = false;
                } else if (lower.contains("primitive sets") || lower.contains("derived sets") || lower.contains("escape handling")) {
                    isDynamicSection = true;
                    isMacroSection = true;
                } else if (lower.contains("keywords") || lower.contains("operators") || lower.contains("delimiters") || lower.contains("ignored")) {
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

            // 3. Identify the Extended Flag individually
            if (tokenName.startsWith("@DER")) {
                isExtendedRule = true;
                tokenName = tokenName.substring(4).trim(); // Strip the flag from the key/name
            }

            // Handle -> skip actions
            if (rightSide.contains("->")) {
                String[] split = rightSide.split("->", 2);
                rightSide = split[0].trim();
                if (split[1].trim().equalsIgnoreCase("skip")) skip = true;
            }

            if (tokenName.isEmpty() || rightSide.isEmpty()) continue;

            // 4. Populate macros or build final rules
            if (isMacroSection) {
                // Keep definitions as reference macros
                macros.put(tokenName, rightSide);
            } else {
                String pattern;

                if (isExtendedRule) {
                    // Send RAW syntax. ExtendedRuleParser handles macros and formatting natively.
                    pattern = rightSide;
                } else if (isDynamicSection) {
                    // Standard parser formatting (basic structural translations)
                    pattern = rightSide.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
                } else {
                    // Static keywords must be escaped regardless of format (e.g., '++' -> '\+\+')
                    pattern = escapeLiteral(rightSide);
                }
                
                rules.add(new Token(tokenName, pattern, skip, isExtendedRule, macros));
            }
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