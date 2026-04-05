package scanner.generator;
import models.AFNDE;
import models.State;
import java.util.List;

public class ErToAFNDE {

    public static final String EPSILON = "ε";
    private int stateCounter = 0;

    private State newState() {
        return new State(stateCounter++);
    }

    // ========================================================================
    // 1. BASE SYMBOL
    // ========================================================================
    
    /**
     * Creates an NFA for a single character or character class (e.g., "a", "[0-9]").
     */
    public AFNDE symbol(String sym) {
        State start = newState();
        State accept = newState();
        accept.setFinal(true);
        start.addTransition(sym, accept);
        return new AFNDE(sym, start, accept);
    }

    // ========================================================================
    // 2. THOMPSON'S CONSTRUCTION OPERATORS
    // ========================================================================

    /**
     * Concatenation (A . B): Connects the end of A to the start of B.
     */
    public AFNDE concat(AFNDE a, AFNDE b) {
        a.getFinalState().setFinal(false);
        a.getFinalState().addTransition(EPSILON, b.getStartState());
        
        return new AFNDE("Concat", a.getStartState(), b.getFinalState());
    }

    /**
     * Union (A | B): Branches to either A or B.
     */
    public AFNDE union(AFNDE a, AFNDE b) {
        State start = newState();
        State accept = newState();
        accept.setFinal(true);

        a.getFinalState().setFinal(false);
        b.getFinalState().setFinal(false);

        // Branching out
        start.addTransition(EPSILON, a.getStartState());
        start.addTransition(EPSILON, b.getStartState());

        // Merging back
        a.getFinalState().addTransition(EPSILON, accept);
        b.getFinalState().addTransition(EPSILON, accept);

        return new AFNDE("Union", start, accept);
    }

    /**
     * Kleene Star (A*): Zero or more occurrences of A.
     */
    public AFNDE kleeneStar(AFNDE a) {
        State start = newState();
        State accept = newState();
        accept.setFinal(true);

        a.getFinalState().setFinal(false);

        // Enter and Bypass
        start.addTransition(EPSILON, a.getStartState());
        start.addTransition(EPSILON, accept); 

        // Loop back and Exit
        a.getFinalState().addTransition(EPSILON, a.getStartState()); 
        a.getFinalState().addTransition(EPSILON, accept);

        return new AFNDE("Star", start, accept);
    }

    /**
     * Optional (A?): Zero or one occurrence of A.
     */
    public AFNDE optional(AFNDE a) {
        State start = newState();
        State accept = newState();
        accept.setFinal(true);

        a.getFinalState().setFinal(false);

        // Enter and Bypass
        start.addTransition(EPSILON, a.getStartState());
        start.addTransition(EPSILON, accept);

        // Exit without looping
        a.getFinalState().addTransition(EPSILON, accept);

        return new AFNDE("Optional", start, accept);
    }

    /**
     * One or More (A+): Equivalent to (A . A*).
     */
    public AFNDE oneOrMore(AFNDE a) {
        State start = newState();
        State accept = newState();
        accept.setFinal(true);

        a.getFinalState().setFinal(false);

        // Must pass through A at least once
        start.addTransition(EPSILON, a.getStartState());
        
        // After passing through A, it can loop back to A's start or exit
        a.getFinalState().addTransition(EPSILON, a.getStartState());
        a.getFinalState().addTransition(EPSILON, accept);

        return new AFNDE("Plus", start, accept);
    }

    /**
     * Wrapper to assign a final Token Name to a completed NFA.
     */
    public AFNDE nameToken(String tokenName, AFNDE nfa) {
        // Tag the final state with the token's identity so the scanner knows what it found
        return new AFNDE(tokenName, nfa.getStartState(), nfa.getFinalState());
    }

    // ========================================================================
    // 3. MASTER SCANNER COMBINER
    // ========================================================================

    /**
     * Receives a list of distinct Regular Expression NFAs and unites them
     * under a single Master Start State for the Lexical Analyzer.
     */
    public AFNDE buildMasterScanner(List<AFNDE> tokenRules) {
        State masterStart = newState();

        for (AFNDE tokenNfa : tokenRules) {
            // Epsilon transition from master start to each token's start state
            masterStart.addTransition(EPSILON, tokenNfa.getStartState());
            
            // Note: We DO NOT merge the final states here.
            // If we merged them, the scanner wouldn't know if it hit an <id> or a <bool>.
        }

        // The master NFA technically has multiple final states now.
        // We pass 'null' for the final state property, as the final states 
        // are distributed among the individual token structures.
        return new AFNDE("MasterLexer", masterStart, null);
    }

    @Override
    public String toString() {
        return "ErToAFNDE Generator [Total States Generated: " + stateCounter + "]";
    }
}