package app;

import java.io.IOException;
import java.util.List;

import Utils.Utils;
import core.lexer.Lexer;
import core.lexer.TokenReader;
import core.parser.FirstFollowCalculator;
import core.parser.GrammarBuilder;
import models.Grammar;
import models.TokenRule;

public class Main {

    // Configuration Constants
    private static final String TOKENS_PATH = "src/main/resources/core/lexer/awk-tokens.txt";
    private static final String BNF_PATH = "src/main/resources/core/lexer/awk-bnf.txt";
    private static final String INPUT_1 = "src/main/resources/core/lexer/scanner-input1.txt";
    private static final String INPUT_2 = "src/main/resources/core/lexer/scanner-input2.txt";
    private static final String INPUT_3 = "src/main/resources/core/lexer/scanner-input3.txt";

    public static void main(String[] args) {
        try {
            System.out.println("Lexical Analyzer (CLI Mode)\n");

            // 1. Initialize Lexer
            Lexer lexer = initializeLexer();

            // 2. Initialize Parser Components
            Grammar grammar = GrammarBuilder.buildFromBnfFile(BNF_PATH);
            FirstFollowCalculator calculator = new FirstFollowCalculator(grammar);

            // 3. Execute Lexical Analysis
            runLexerSuite(lexer);

            // 4. Compute and Display Parser Tables
            System.out.println("\n=== Building Parser Tables ===");
            calculator.computeSets();
            calculator.printTables();

        } catch (IOException e) {
            System.err.println("❌ Error processing files: " + e.getMessage());
        }
    }

    /**
     * Loads rules and initializes the Lexer instance.
     */
    private static Lexer initializeLexer() throws IOException {
        List<TokenRule> rules = TokenReader.readTokens(TOKENS_PATH);
        return new Lexer(rules);
    }

    /**
     * Executes the scan for multiple input files.
     */
    private static void runLexerSuite(Lexer lexer) throws IOException {
        runAnalysis("Right Input", Utils.readTextFile(INPUT_1), lexer);
        runAnalysis("Broken Input 1", Utils.readTextFile(INPUT_2), lexer);
        runAnalysis("Broken Input 2", Utils.readTextFile(INPUT_3), lexer);
        
        System.out.println("\n✅ Lexical Analysis Completed.");
    }

    /**
     * Helper to perform individual analysis and print results.
     */
    private static void runAnalysis(String label, String source, Lexer lexer) {
        System.out.println("\n=== Testing: " + label + " ===");
        String result = lexer.scan(source);
        System.out.println(result);
    }
}