package core.lexer.models.atomic;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a state in a finite automaton (NFA/DFA). Each state has a unique identifier, can be
 * initial and/or final, and contains a list of outgoing transitions.
 *
 * @author Generated
 * @version 1.0
 */
@Getter
public class State {

    private final int id;
    private boolean isInitial;
    private boolean isFinal;
    @Setter private String acceptedToken;

    /**
     * Constructs a new non‑initial, non‑final state with the given ID.
     *
     * @param id unique identifier for this state
     */
    public State(int id) {
        this(id, false, false);
    }

    /**
     * Constructs a new state with the given ID and initial/final flags.
     *
     * @param id unique identifier
     * @param isInitial true if this state is an initial state
     * @param isFinal true if this state is a final (accepting) state
     */
    public State(int id, boolean isInitial, boolean isFinal) {
        this.id = id;
        this.isInitial = isInitial;
        this.isFinal = isFinal;
    }

    /**
     * Sets whether this state is an initial state.
     *
     * @param initial true to mark as initial, false otherwise
     */
    public void setInitial(boolean initial) {
        this.isInitial = initial;
    }

    /**
     * Sets whether this state is a final (accepting) state.
     *
     * @param isFinal true to mark as final, false otherwise
     */
    public void setFinalState(boolean isFinal) {
        this.isFinal = isFinal;
    }

    /**
     * Compares two states based on their unique ID.
     *
     * @param o the object to compare
     * @return true if the IDs are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        return id == ((State) o).id;
    }

    /**
     * Returns the hash code based on the state's ID.
     *
     * @return hash code of the ID
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "q"
                + id
                + (isInitial ? "[INITIAL]" : "")
                + (isFinal ? "[FINAL]" : "")
                + (acceptedToken != null ? "(" + acceptedToken + ")" : "");
    }
}
