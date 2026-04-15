package core.lexer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import core.lexer.conversors.AFDMinimizer;
import core.lexer.conversors.AFNDEtoAFND;
import core.lexer.conversors.AFNDtoAFD;
import core.lexer.conversors.ReToAFNDE;
import core.lexer.translators.RuleParser;
import models.atomic.State;
import models.atomic.Token;
import models.automata.AFD;
import models.automata.AFND;
import models.automata.AFNDE;
import models.tables.SymbolTable;

public class Lexer {
    
    private AFD masterAutomaton;
    private final List<Token> rules;
    private final SymbolTable symbolTable;
    private final Set<String> symbolTableTokens;

    public Lexer(List<Token> rules) {
        this.rules = rules;
        System.out.println("--- Building Scanner For: " + rules.size() + " Tokens ---");
        this.symbolTable = new SymbolTable();
        this.symbolTableTokens = determineSymbolTableTokens(rules);
        
        buildScanner();
    }

    private Set<String> determineSymbolTableTokens(List<Token> rules) {
        Set<String> dynamicTokens = new HashSet<>();
        String metacharacterPattern = ".*[\\\\\\[\\]\\(\\)\\*\\+\\?\\|].*";

        for (Token rule : rules) {
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
        ReToAFNDE afndeGenerator = new ReToAFNDE();
        RuleParser parser = new RuleParser(afndeGenerator);
        List<AFNDE> afndeList = new ArrayList<>();

        for (Token rule : rules) {
            AFNDE partialAutomaton = parser.parse(rule);
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

    // Helper method to extract precise line and column integers
    private int[] getLineCol(String input, int index) {
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
        return new int[]{line, col};
    }

    private String getPosition(String input, int index) {
        int[] pos = getLineCol(input, index);
        return "[" + pos[0] + ", " + pos[1] + " ]";
    }

    // Helper class to buffer tokens before committing them to the symbol table
    private static class PendingSymbol {
        String lexeme;
        String tokenType;
        int line;
        int col;

        PendingSymbol(String lexeme, String tokenType, int line, int col) {
            this.lexeme = lexeme;
            this.tokenType = tokenType;
            this.line = line;
            this.col = col;
        }
    }

    public String scan(String input) {
        System.out.println("Scanning input...");
        int currentIndex = 0;
        int inputLength = input.length();
        
        // Tracking state for the buffer-and-commit logic
        boolean hasErrors = false;
        List<PendingSymbol> pendingSymbols = new ArrayList<>();

        while (currentIndex < inputLength) {
            
            // --- MAXIMAL MUNCH ALGORITHM ---
            int lastAcceptingIndex = -1;
            String lastAcceptingToken = null;

            State currentState = masterAutomaton.getStartState();

            // 1. Scan forward as far as physically possible
            for (int i = currentIndex; i < inputLength; i++) {
                char c = input.charAt(i);

                currentState = masterAutomaton.getNextState(currentState, c);

                // 2. Dead end reached: halt forward scanning
                if (currentState == null) {
                    break; 
                }

                // 3. Valid state reached: Record this as our longest match so far
                if (currentState.isFinal()) {
                    lastAcceptingIndex = i;
                    lastAcceptingToken = currentState.getAcceptedToken();
                }
            }

            // 4. Resolve the Longest Match
            if (lastAcceptingIndex != -1) {
                // Consume only up to the last accepting state we recorded
                String lexeme = input.substring(currentIndex, lastAcceptingIndex + 1);

                boolean isSkipToken = false;
                for (Token rule : rules) {
                    if (rule.getTokenType().equals(lastAcceptingToken)) {
                        isSkipToken = rule.isSkip();
                        break;
                    }
                }

                String displayLexeme = lexeme.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
                System.out.println(
                    getPosition(input, currentIndex) + ": Found Token: <" + lastAcceptingToken + ", '" + displayLexeme + "'>"
                );

                if (!isSkipToken) {
                    if (this.symbolTableTokens.contains(lastAcceptingToken)) {
                        // Extract precise position
                        int[] pos = getLineCol(input, currentIndex);
                        // BUFFER the token with line and col
                        pendingSymbols.add(new PendingSymbol(lexeme, lastAcceptingToken, pos[0], pos[1]));
                    }
                }

                // Advance pointer to resume scanning AFTER the matched token
                currentIndex = lastAcceptingIndex + 1;
                
            } else {
                // --- FIXED: PANIC MODE ERROR RECOVERY ---
                hasErrors = true; // Flag the error
                
                int errorStart = currentIndex;
                
                while (currentIndex < inputLength && !Character.isWhitespace(input.charAt(currentIndex))) {
                    currentIndex++;
                }
                
                String malformedChunk = input.substring(errorStart, currentIndex);
                
                System.err.println(
                    getPosition(input, errorStart) + ": Lexical Error: Invalid sequence '" + malformedChunk + "'"
                );
            }
        }

        // --- FINAL COMMIT PHASE ---
        if (hasErrors) {
            System.err.println("Scanning finished with errors. Symbol table was NOT populated.");
            
            symbolTable.clearTable();
            
            return "Scanning failed with lexical errors.";
        } else {
            // 100% success: Flush the buffer into the actual SymbolTable
            for (PendingSymbol ps : pendingSymbols) {
                symbolTable.insert(ps.lexeme, ps.tokenType, ps.line, ps.col);
            }
            System.out.println("Scanning complete. Symbol table populated successfully.");
            return "Scanning complete.";
        }
    }
}