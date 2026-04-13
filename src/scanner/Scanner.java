package scanner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.AFD;
import models.AFND;
import models.AFNDE;
import models.State;
import models.SymbolTable;
import models.TokenRule;
import scanner.conversors.AFDMinimizer;
import scanner.conversors.AFNDEtoAFND;
import scanner.conversors.AFNDtoAFD;
import scanner.conversors.ERtoAFNDE;

public class Scanner {
    
    private AFD masterAutomaton;
    private final List<TokenRule> rules;
    private final SymbolTable symbolTable;
    
    // Converted to a dynamic instance variable
    private final Set<String> symbolTableTokens;

    public Scanner(List<TokenRule> rules) {
        this.rules = rules;
        this.symbolTable = new SymbolTable();
        this.symbolTableTokens = determineSymbolTableTokens(rules);
        
        buildScanner();
    }

    /**
     * Evaluates the regular expressions to differentiate between static literals 
     * and dynamic patterns that require symbol table storage.
     */
    private Set<String> determineSymbolTableTokens(List<TokenRule> rules) {
        Set<String> dynamicTokens = new HashSet<>();
        
        // Regex to detect common regex metacharacters: \ [ ] ( ) * + ? |
        String metacharacterPattern = ".*[\\\\\\[\\]\\(\\)\\*\\+\\?\\|].*";

        for (TokenRule rule : rules) {
            if (rule.getRegex().matches(metacharacterPattern)) {
                dynamicTokens.add(rule.getTokenType());
            }
        }
        
        return dynamicTokens;
    }

    private void buildScanner() {
        System.out.println("--- Building Scanner Pipeline ---");
        long start, end, totalStart = System.nanoTime();

        // Step 0 - Build AFNDE list
        start = totalStart;

        ERtoAFNDE afndeGenerator = new ERtoAFNDE();
        RuleParser parser = new RuleParser(afndeGenerator);
        List<AFNDE> afndeList = new ArrayList<>();

        for (TokenRule rule : rules) {
            AFNDE partialAutomaton = parser.parse(rule.getRegex(), rule.getTokenType());
            afndeList.add(partialAutomaton);
        }

        end = System.nanoTime();
        System.out.println("0. Rules parsed into AFNDEs. Time: " + ((end - start) / 1_000_000) + " ms");

        // Step 1 - Master AFNDE
        start = System.nanoTime();

        AFNDE masterAFNDE = afndeGenerator.buildMasterScanner(afndeList);

        end = System.nanoTime();
        System.out.println("1. Master AFNDE built successfully. Time: " + ((end - start) / 1_000_000) + " ms");

        // Step 2 - AFNDE -> AFND
        start = System.nanoTime();

        AFNDEtoAFND eConverter = new AFNDEtoAFND();
        AFND masterAFND = eConverter.convert(masterAFNDE);

        end = System.nanoTime();
        System.out.println("2. AFNDE Converted to AFND. Time: " + ((end - start) / 1_000_000) + " ms");

        // Step 3 - AFND -> AFD
        start = System.nanoTime();

        AFNDtoAFD afdConverter = new AFNDtoAFD();
        AFD masterAFD = afdConverter.convert(masterAFND);

        end = System.nanoTime();
        System.out.println("3. AFND converted to strictly deterministic AFD. Time: " + ((end - start) / 1_000_000) + " ms");

        // Step 4 - Minimization
        start = System.nanoTime();

        AFDMinimizer minimizer = new AFDMinimizer();
        this.masterAutomaton = minimizer.minimize(masterAFD);

        end = System.nanoTime();
        System.out.println("4. AFD Minimized successfully. Time: " + ((end - start) / 1_000_000) + " ms");

        System.out.println("---------------------------------");
        System.out.println("Total pipeline time: " + ((end - totalStart) / 1_000_000) + " ms\n");
    }

    public AFD getMasterAutomaton() {
        return masterAutomaton;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * Calculates the line and column number for a given absolute index in the source code.
     */
    private String getPosition(String input, int index) {
        int line = 1;
        int col = 1;
        for (int i = 0; i < index && i < input.length(); i++) {
            if (input.charAt(i) == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
        return "[" + line + ", " + col + " ]";
    }

    public void scan(String input) {
        System.out.println("Scanning input...");
        int currentIndex = 0;
        int inputLength = input.length();

        while (currentIndex < inputLength) {

            if (input.charAt(currentIndex) == '"') {
                int start = currentIndex;
                currentIndex++; // skip opening quote

                boolean closed = false;

                while (currentIndex < inputLength) {
                    char c = input.charAt(currentIndex);

                    if (c == '\\') {
                        // skip escaped char safely
                        currentIndex += 2;
                        continue;
                    }

                    if (c == '"') {
                        currentIndex++; // consume closing quote
                        closed = true;
                        break;
                    }

                    currentIndex++;
                }

                if (!closed) {
                    System.err.println(
                        getPosition(input, start) +": Lexical Error: Unterminated string"
                    );
                    continue; // skip entire broken string
                }

                // If string is valid → rewind so DFA handles it normally
                currentIndex = start;
            }

            int lastAcceptingIndex = -1;
            String lastAcceptingToken = null;

            State currentState = masterAutomaton.getStartState();

            for (int i = currentIndex; i < inputLength; i++) {
                char c = input.charAt(i);

                currentState = masterAutomaton.getNextState(currentState, c);

                if (currentState == null) {
                    break;
                }

                if (currentState.isFinal()) {
                    lastAcceptingIndex = i;
                    lastAcceptingToken = currentState.getAcceptedToken();
                }
            }

            if (lastAcceptingIndex != -1) {
                String lexeme = input.substring(currentIndex, lastAcceptingIndex + 1);

                // Determine if this token should be ignored (e.g., WHITESPACE, COMMENT)
                boolean isSkipToken = false;
                for (TokenRule rule : rules) {
                    if (rule.getTokenType().equals(lastAcceptingToken)) {
                        isSkipToken = rule.isSkip();
                        break;
                    }
                }

                // Print all tokens, even the skip ones. 
                // We escape newlines/tabs so it prints cleanly on a single line in the console.
                String displayLexeme = lexeme.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
                System.out.println(
                    getPosition(input, currentIndex) + ": Found Token: <" + lastAcceptingToken + ", '" + displayLexeme + "'>"
                );

                // Store the token ONLY if it is not a skip token
                if (!isSkipToken) {
                    // Use the dynamically generated Set to verify inclusion
                    if (this.symbolTableTokens.contains(lastAcceptingToken)) {
                        symbolTable.insert(lexeme, lastAcceptingToken);
                    }
                }

                currentIndex = lastAcceptingIndex + 1;
            } else {
                System.err.println(
                    getPosition(input, currentIndex) +
                    ": Unexpected character '" + input.charAt(currentIndex) + "'"
                );
                currentIndex++;
            }
        }

        System.out.println("Scanning complete.");
    }
}