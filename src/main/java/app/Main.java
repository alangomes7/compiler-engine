package app;

import java.io.IOException;
import java.util.List;

import Utils.Utils;
import core.lexer.Lexer;
import core.lexer.TokenReader;
import models.TokenRule;

public class Main {

    public static void main(String[] args) {

        System.out.println("Lexical Analyzer (CLI Mode)\n");

        String tokensAndRegularExpressions = "src/main/resources/core/lexer/awk-tokens.txt";

        // The Lexer (Scanner)
        List<TokenRule> rules = TokenReader.readTokens(tokensAndRegularExpressions);
        Lexer lexer = new Lexer(rules);

        // Input files variables
        String input1 = "", input2 = "", input3 = "";

        try {
            // Reding Input
            input1 = Utils.readTextFile("src/main/resources/core/lexer/scanner-input1.txt");
            input2 = Utils.readTextFile("src/main/resources/core/lexer/scanner-input2.txt");
            input3 = Utils.readTextFile("src/main/resources/core/lexer/scanner-input3.txt");            

        } catch (IOException e) {
            System.err.println("❌ Error reading input files: " + e.getMessage());
        }

        // Run Lexer (scanning)
        runAnalysis("right input", input1, lexer);
        runAnalysis("broken input", input2, lexer);
        runAnalysis("broken input", input3, lexer);

        System.out.println("\n✅ Lexical Analysis Completed.");
    }

    private static void runAnalysis(String label, String source, Lexer lexer) {
        System.out.println("\n=== " + label + " ===");

        String result = lexer.scan(source);

        System.out.println(result);
    }
}