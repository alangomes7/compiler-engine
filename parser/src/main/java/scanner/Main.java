package scanner;

import java.util.ArrayList;
import java.util.List;

import models.TokenRule;


public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing Lexical Analyzer System...\n");

        List<TokenRule> lexicalRules = new ArrayList<>();
        
        // 1. Exact matches / keywords FIRST for priority
        lexicalRules.add(new TokenRule("ASSIGN", "="));

        // 2. Complex patterns
        lexicalRules.add(new TokenRule("NUMBER", "-?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([eE]-?[0-9]+)?"));
        lexicalRules.add(new TokenRule("STRING", "\"([^\"\\\\]|\\\\[\"\\\\nrt])*\""));
        lexicalRules.add(new TokenRule("BOOLEAN", "#(t|T|f|F)"));
        
        // 3. Identifier (letters, digits, non-consecutive/non-trailing underscores)
        lexicalRules.add(new TokenRule("IDENTIFIER", "[a-zA-Z](_?[a-zA-Z0-9])*"));

        Scanner lexer = new Scanner(lexicalRules);

        // ====================================================================
        // COMPREHENSIVE TEST INPUT
        // ====================================================================
        String testInput = 
            // 1. Valid Identifiers & Assignments
            "valid_id = camelCase_123 " +
            
            // 2. Numbers (Integer, Decimal, Missing Leading/Trailing zero, Scientific, Negative)
            "42 -0.99 .5 7. 1e10 -3.14e-2 " +
            
            // 3. Booleans
            "#t #F " +
            
            // 4. Strings (Normal, Escaped characters, Empty)
            "\"normal string\" \"escaped \\\" quote\\n\" \"\" " +
            
            // 5. ADA Identifier Violations (These should split into multiple tokens or throw errors)
            "123invalid _start invalid_ invalid__id " +
            
            // 6. Lexical errors
            "\"unterminated error";

        System.out.println("=== Testing Scanner ===");
        lexer.scan(testInput);
        
        // Output the generated Symbol Table
        lexer.getSymbolTable().printTable();
    }
}