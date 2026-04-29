package core.lexer.models.atomic;

import lombok.Getter;
import lombok.Setter;

@Getter
public class State {

    private final int id;
    private boolean isInitial;
    private boolean isFinal;
    @Setter private String acceptedToken;

    public State(int id) {
        this(id, false, false);
    }

    public State(int id, boolean isInitial, boolean isFinal) {
        this.id = id;
        this.isInitial = isInitial;
        this.isFinal = isFinal;
    }

    public void setInitial(boolean initial) {
        this.isInitial = initial;
    }

    public void setFinalState(boolean isFinal) {
        this.isFinal = isFinal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        return id == ((State) o).id;
    }

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
