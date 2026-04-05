package models;

import java.util.ArrayList;
import java.util.List;

public class State {
    private final int id;
    private boolean isFinal;
    private final List<Transition> transitions;

    public State(int id) {
        this.id = id;
        this.isFinal = false;
        this.transitions = new ArrayList<>();
    }

    public void addTransition(String symbol, State target) {
        this.transitions.add(new Transition(symbol, target));
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public int getId() { return id; }
    public boolean isFinal() { return isFinal; }
    public List<Transition> getTransitions() { return transitions; }
}