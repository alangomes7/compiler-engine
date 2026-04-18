package core.lexer.core.conversors;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.automata.AFNDE;
import java.util.List;
import models.atomic.Constants;

public class ReToAFNDE {

    private static final String CONCAT = "Concat";
    private static final String UNION = "Union";
    private static final String STAR = "Star";
    private static final String OPTIONAL = "Optional";
    private static final String PLUS = "Plus";
    private static final String MASTER = "MasterLexer";

    private final Symbol EPSILON = new Symbol(Constants.EPSILON);

    private int stateCounter = 0;

    public AFNDE symbol(String sym) {
        State start = new State(stateCounter++);
        State accept = new State(stateCounter++);
        accept.setFinal(true);

        start.addTransition(new Symbol(sym), accept);
        return new AFNDE(sym, start, accept);
    }

    public AFNDE concat(AFNDE a, AFNDE b) {
        State aStart = a.getStartState();
        State aFinal = a.getFinalState();
        State bStart = b.getStartState();
        State bFinal = b.getFinalState();

        if (aFinal.isFinal()) {
            aFinal.setFinal(false);
        }

        aFinal.addTransition(EPSILON, bStart);

        return new AFNDE(CONCAT, aStart, bFinal);
    }

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

        start.addTransition(EPSILON, aStart);
        start.addTransition(EPSILON, bStart);

        aFinal.addTransition(EPSILON, accept);
        bFinal.addTransition(EPSILON, accept);

        return new AFNDE(UNION, start, accept);
    }

    public AFNDE kleeneStar(AFNDE a) {
        State start = new State(stateCounter++);
        State accept = new State(stateCounter++);
        accept.setFinal(true);

        State aStart = a.getStartState();
        State aFinal = a.getFinalState();

        if (aFinal.isFinal()) {
            aFinal.setFinal(false);
        }

        start.addTransition(EPSILON, aStart);
        start.addTransition(EPSILON, accept);

        aFinal.addTransition(EPSILON, aStart);
        aFinal.addTransition(EPSILON, accept);

        return new AFNDE(STAR, start, accept);
    }

    public AFNDE optional(AFNDE a) {
        State start = new State(stateCounter++);
        State accept = new State(stateCounter++);
        accept.setFinal(true);

        State aStart = a.getStartState();
        State aFinal = a.getFinalState();

        if (aFinal.isFinal()) {
            aFinal.setFinal(false);
        }

        start.addTransition(EPSILON, aStart);
        start.addTransition(EPSILON, accept);

        aFinal.addTransition(EPSILON, accept);

        return new AFNDE(OPTIONAL, start, accept);
    }

    public AFNDE oneOrMore(AFNDE a) {
        State aStart = a.getStartState();
        State aFinal = a.getFinalState();

        State accept = new State(stateCounter++);
        accept.setFinal(true);

        if (aFinal.isFinal()) {
            aFinal.setFinal(false);
        }

        aFinal.addTransition(EPSILON, aStart);
        aFinal.addTransition(EPSILON, accept);

        return new AFNDE(PLUS, aStart, accept);
    }

    public AFNDE nameToken(String tokenName, AFNDE nfa) {
        State finalState = nfa.getFinalState();
        finalState.setAcceptedToken(tokenName);

        return new AFNDE(tokenName, nfa.getStartState(), finalState);
    }

    public AFNDE buildMasterScanner(List<AFNDE> tokenRules) {
        State masterStart = new State(stateCounter++);

        for (int i = 0, size = tokenRules.size(); i < size; i++) {
            AFNDE tokenNfa = tokenRules.get(i);
            masterStart.addTransition(EPSILON, tokenNfa.getStartState());
        }

        return new AFNDE(MASTER, masterStart, null);
    }

    @Override
    public String toString() {
        return "ReToAFNDE Generator [Total States Generated: " + stateCounter + "]";
    }
}
