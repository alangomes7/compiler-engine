package scanner;

import java.util.ArrayList;
import java.util.List;

import models.AFD;
import models.AFND;
import models.AFNDE;
import models.State;
import models.TokenRule;
import scanner.conversors.AFDMinimizer;
import scanner.conversors.AFNDEtoAFND;
import scanner.conversors.AFNDtoAFD;
import scanner.conversors.ERtoAFNDE;

public class Scanner {
    
    private AFD masterAutomaton;
    private List<TokenRule> rules;

    public Scanner(List<TokenRule> rules) {
        this.rules = rules;
        buildScanner();
    }

    private void buildScanner() {
        System.out.println("--- Building Scanner Pipeline ---");
        
        ERtoAFNDE afndeGenerator = new ERtoAFNDE();
        List<AFNDE> afndeList = new ArrayList<>();

        for (TokenRule rule : rules) {
            AFNDE partialAutomaton = afndeGenerator.symbol(rule.getRegex());
            partialAutomaton.getFinalState().setAcceptedToken(rule.getTokenType()); 
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

    public void scan(String input) {
        System.out.println("Scanning input...");
        int currentIndex = 0;
        int inputLength = input.length();

        while (currentIndex < inputLength) {
            if (Character.isWhitespace(input.charAt(currentIndex))) {
                currentIndex++;
                continue;
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
                System.out.println("Found Token: <" + lastAcceptingToken + ", '" + lexeme + "'>");
                currentIndex = lastAcceptingIndex + 1; 
            } else {
                System.err.println("Lexical Error at index " + currentIndex + ": Unexpected character '" + input.charAt(currentIndex) + "'");
                currentIndex++; 
            }
        }
        System.out.println("Scanning complete.");
    }
}