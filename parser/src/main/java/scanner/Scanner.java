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
        
        ERtoAFNDE afndeGenerator = new ERtoAFNDE();
        RuleParser parser = new RuleParser(afndeGenerator);
        List<AFNDE> afndeList = new ArrayList<>();

        for (TokenRule rule : rules) {
            AFNDE partialAutomaton = parser.parse(rule.getRegex(), rule.getTokenType()); 
            afndeList.add(partialAutomaton);
        }

        AFNDE masterAFNDE = afndeGenerator.buildMasterScanner(afndeList);
        System.out.println("1. Master AFNDE built successfully.");

        AFNDEtoAFND eConverter = new AFNDEtoAFND();
        AFND masterAFND = eConverter.convert(masterAFNDE);
        System.out.println("2. Converted to AFND.");

        AFNDtoAFD afdConverter = new AFNDtoAFD();
        AFD masterAFD = afdConverter.convert(masterAFND);
        System.out.println("3. Converted to strictly deterministic AFD.");

        AFDMinimizer minimizer = new AFDMinimizer();
        this.masterAutomaton = minimizer.minimize(masterAFD);
        System.out.println("4. AFD Minimized successfully.");
        System.out.println("---------------------------------\n");
    }

    public AFD getMasterAutomaton() {
        return masterAutomaton;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void scan(String input) {
        System.out.println("Scanning input...");
        int currentIndex = 0;
        int inputLength = input.length();

        while (currentIndex < inputLength) {

            // ============================================================
            // Skip whitespace
            // ============================================================
            if (Character.isWhitespace(input.charAt(currentIndex))) {
                currentIndex++;
                continue;
            }

            // ============================================================
            // 🔥 SPECIAL STRING ERROR HANDLING (Option 3)
            // ============================================================
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
                        "Lexical Error: Unterminated string starting at index " + start
                    );
                    continue; // skip entire broken string
                }

                // If string is valid → rewind so DFA handles it normally
                currentIndex = start;
            }

            // ============================================================
            // 🔥 DFA MAXIMAL MUNCH
            // ============================================================
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

            // ============================================================
            // ✅ TOKEN FOUND
            // ============================================================
            if (lastAcceptingIndex != -1) {
                String lexeme = input.substring(currentIndex, lastAcceptingIndex + 1);

                System.out.println(
                    "Found Token: <" + lastAcceptingToken + ", '" + lexeme + "'>"
                );

                // Use the dynamically generated Set to verify inclusion
                if (this.symbolTableTokens.contains(lastAcceptingToken)) {
                    symbolTable.insert(lexeme, lastAcceptingToken);
                }

                currentIndex = lastAcceptingIndex + 1;
            } else {
                // ========================================================
                // ❌ GENERAL ERROR
                // ========================================================
                System.err.println(
                    "Lexical Error at index " + currentIndex +
                    ": Unexpected character '" + input.charAt(currentIndex) + "'"
                );
                currentIndex++;
            }
        }

        System.out.println("Scanning complete.");
    }
}