package models.atomic;

import java.util.ArrayList;
import java.util.List;

public class State {
    private final int id;
    private boolean isFinal;
    private String acceptedToken;
    private final List<Transition> transitions;

    public State(int id) {
        this.id = id;
        this.isFinal = false;
        this.acceptedToken = null;
        this.transitions = new ArrayList<>();
    }

    public void addTransition(String symbol, State target) {
        this.transitions.add(new Transition(symbol, target));
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    // ADDED: Getters and setters for the token type
    public void setAcceptedToken(String acceptedToken) {
        this.acceptedToken = acceptedToken;
    }
    public String getAcceptedToken() {
        return acceptedToken;
    }

    public int getId() { return id; }
    public boolean isFinal() { return isFinal; }
    public List<Transition> getTransitions() { return transitions; }
}