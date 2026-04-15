package core.lexer.conversors;

import java.util.List;

import models.atomic.Constants;
import models.atomic.State;
import models.automata.AFNDE;

public class ReToAFNDE {

    // Reused labels (avoid repeated string creation)
    private static final String CONCAT = "Concat";
    private static final String UNION = "Union";
    private static final String STAR = "Star";
    private static final String OPTIONAL = "Optional";
    private static final String PLUS = "Plus";
    private static final String MASTER = "MasterLexer";

    private int stateCounter = 0;

    // ========================================================================
    // 1. BASE SYMBOL
    // ========================================================================
    public AFNDE symbol(String sym) {
        State start = new State(stateCounter++);
        State accept = new State(stateCounter++);
        accept.setFinal(true);

        start.addTransition(sym, accept);
        return new AFNDE(sym, start, accept);
    }

    // ========================================================================
    // 2. THOMPSON OPERATORS
    // ========================================================================

    // CONCAT (A . B)
    public AFNDE concat(AFNDE a, AFNDE b) {
        State aStart = a.getStartState();
        State aFinal = a.getFinalState();
        State bStart = b.getStartState();
        State bFinal = b.getFinalState();

        if (aFinal.isFinal()) {
            aFinal.setFinal(false);
        }

        aFinal.addTransition(Constants.EPSILON, bStart);

        return new AFNDE(CONCAT, aStart, bFinal);
    }

    // UNION (A | B)
    public AFNDE union(AFNDE a, AFNDE b) {
        State start = new State(stateCounter++);
        State accept = new State(stateCounter++);
        accept.setFinal(true);

        State aStart = a.getStartState();
        State bStart = b.getStartState();
        State aFinal = a.getFinalState();
        State bFinal = b.getFinalState();

        if (aFinal.isFinal()) aFinal.setFinal(false);
        if (bFinal.isFinal()) bFinal.setFinal(false);

        start.addTransition(Constants.EPSILON, aStart);
        start.addTransition(Constants.EPSILON, bStart);

        aFinal.addTransition(Constants.EPSILON, accept);
        bFinal.addTransition(Constants.EPSILON, accept);

        return new AFNDE(UNION, start, accept);
    }

    // KLEENE STAR (A*)
    public AFNDE kleeneStar(AFNDE a) {
        State start = new State(stateCounter++);
        State accept = new State(stateCounter++);
        accept.setFinal(true);

        State aStart = a.getStartState();
        State aFinal = a.getFinalState();

        if (aFinal.isFinal()) {
            aFinal.setFinal(false);
        }

        // Enter or bypass
        start.addTransition(Constants.EPSILON, aStart);
        start.addTransition(Constants.EPSILON, accept);

        // Loop + exit
        aFinal.addTransition(Constants.EPSILON, aStart);
        aFinal.addTransition(Constants.EPSILON, accept);

        return new AFNDE(STAR, start, accept);
    }

    // OPTIONAL (A?)
    public AFNDE optional(AFNDE a) {
        State start = new State(stateCounter++);
        State accept = new State(stateCounter++);
        accept.setFinal(true);

        State aStart = a.getStartState();
        State aFinal = a.getFinalState();

        if (aFinal.isFinal()) {
            aFinal.setFinal(false);
        }

        start.addTransition(Constants.EPSILON, aStart);
        start.addTransition(Constants.EPSILON, accept);

        aFinal.addTransition(Constants.EPSILON, accept);

        return new AFNDE(OPTIONAL, start, accept);
    }

    // ONE OR MORE (A+)
    // Optimized: reuse A's start state (no extra start node)
    public AFNDE oneOrMore(AFNDE a) {
        State aStart = a.getStartState();
        State aFinal = a.getFinalState();

        State accept = new State(stateCounter++);
        accept.setFinal(true);

        if (aFinal.isFinal()) {
            aFinal.setFinal(false);
        }

        aFinal.addTransition(Constants.EPSILON, aStart);
        aFinal.addTransition(Constants.EPSILON, accept);

        return new AFNDE(PLUS, aStart, accept);
    }

    // TOKEN NAMING
    public AFNDE nameToken(String tokenName, AFNDE nfa) {
        State finalState = nfa.getFinalState();
        finalState.setAcceptedToken(tokenName);

        return new AFNDE(tokenName, nfa.getStartState(), finalState);
    }

    // ========================================================================
    // 3. MASTER SCANNER
    // ========================================================================
    public AFNDE buildMasterScanner(List<AFNDE> tokenRules) {
        State masterStart = new State(stateCounter++);

        // Avoid repeated lookups
        for (int i = 0, size = tokenRules.size(); i < size; i++) {
            AFNDE tokenNfa = tokenRules.get(i);
            masterStart.addTransition(Constants.EPSILON, tokenNfa.getStartState());
        }

        return new AFNDE(MASTER, masterStart, null);
    }

    @Override
    public String toString() {
        return "ErToAFNDE Generator [Total States Generated: " + stateCounter + "]";
    }
}