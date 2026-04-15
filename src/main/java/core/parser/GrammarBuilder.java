package core.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import models.atomic.Symbol;
import models.others.Grammar;

public class GrammarBuilder {

    public static Grammar buildFromBnfFile(String filePath) throws IOException {
        Grammar grammar = new Grammar();
        List<String> lines = Files.readAllLines(Path.of(filePath));
        
        Symbol currentLhs = null;
        boolean isFirstRule = true; // Flag for the start symbol

        String cleanUpRegex = "\\\\";
        
        // Use a standard for-loop to track the line number (1-based index)
        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i).replaceAll(cleanUpRegex, "").trim();
            
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.contains("::=")) {
                String[] parts = line.split("::=");
                String lhsLexeme = parts[0].trim();
                
                // Construct LHS Symbol with line number
                currentLhs = new Symbol(lhsLexeme, "NON_TERMINAL", lineNumber, 1);
                
                // Set the start symbol on the very first rule found
                if (isFirstRule) {
                    grammar.setStartSymbol(currentLhs);
                    isFirstRule = false;
                }
                
                String rhsRaw = parts[1].trim();
                addRhsToGrammar(grammar, currentLhs, rhsRaw, lineNumber);
                
            } else if (line.startsWith("|") && currentLhs != null) {
                addRhsToGrammar(grammar, currentLhs, line.substring(1).trim(), lineNumber);
            } else if (currentLhs != null) {
                // Continuation of previous RHS on a new line
                addRhsToGrammar(grammar, currentLhs, line, lineNumber);
            }
        }
        return grammar;
    }

    private static void addRhsToGrammar(Grammar grammar, Symbol lhs, String rhsRaw, int lineNumber) {
        if (rhsRaw.isEmpty()) return;
        
        // Split by the OR pipe, respecting quoted strings
        String[] rules = rhsRaw.split("\\|"); 
        for (String rule : rules) {
            String trimmed = rule.trim();
            if (trimmed.isEmpty()) continue;
            
            // Basic tokenizer for the symbols (splits by space)
            String[] lexemes = trimmed.split("\\s+");
            List<Symbol> rhsSymbols = new ArrayList<>();
            
            for (String lexeme : lexemes) {
                // Instantiate the RHS Symbol with the line and column
                rhsSymbols.add(new Symbol(lexeme, "GRAMMAR_SYMBOL", lineNumber, 1));
            }
            
            // Pass the Symbol objects to the Grammar
            grammar.addRule(lhs, rhsSymbols);
        }
    }
}