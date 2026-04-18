package core.lexer.models.atomic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class State {
    private final int id;
    private boolean isFinal;
    private String acceptedToken;
    private final List<Transition> transitions;
    private final Set<Symbol> symbols;

    public State(int id) {
        this.id = id;
        this.isFinal = false;
        this.acceptedToken = null;
        this.transitions = new ArrayList<>();
        this.symbols = new HashSet<>();
    }

    public void addTransition(Symbol symbol, State target) {
        Transition transition = new Transition(this, target, symbol);
        this.transitions.add(transition);
        this.symbols.add(symbol);
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public void setAcceptedToken(String acceptedToken) {
        this.acceptedToken = acceptedToken;
    }

    public String getAcceptedToken() {
        return acceptedToken;
    }

    public int getId() {
        return id;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public Set<Symbol> getSymbols() {
        return symbols;
    }
}
