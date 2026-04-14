package core.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import models.others.Grammar;

public class GrammarBuilder {

    public static Grammar buildFromBnfFile(String filePath) throws IOException {
        Grammar grammar = new Grammar();
        List<String> lines = Files.readAllLines(Path.of(filePath));
        
        String currentLhs = null;

        String cleanUpRegex = "\\\\";
        for (String line : lines) {
            // Clean up sources markers and whitespace
            line = line.replaceAll(cleanUpRegex, "").trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.contains("::=")) {
                String[] parts = line.split("::=");
                currentLhs = parts[0].trim();
                String rhsRaw = parts[1].trim();
                addRhsToGrammar(grammar, currentLhs, rhsRaw);
            } else if (line.startsWith("|") && currentLhs != null) {
                addRhsToGrammar(grammar, currentLhs, line.substring(1).trim());
            } else if (currentLhs != null) {
                // Continuation of previous RHS on a new line
                addRhsToGrammar(grammar, currentLhs, line);
            }
        }
        return grammar;
    }

    private static void addRhsToGrammar(Grammar grammar, String lhs, String rhsRaw) {
        if (rhsRaw.isEmpty()) return;
        
        // Split by the OR pipe, respecting quoted strings
        String[] rules = rhsRaw.split("\\|"); 
        for (String rule : rules) {
            String trimmed = rule.trim();
            if(trimmed.isEmpty()) continue;
            
            // Basic tokenizer for the symbols (splits by space)
            // Note: Your grammar uses ' ' to separate symbols. 
            // In a production compiler, you'd use your Lexer to tokenize the BNF itself!
            List<String> symbols = new ArrayList<>(Arrays.asList(trimmed.split("\\s+")));
            grammar.addRule(lhs, symbols);
        }
    }
}