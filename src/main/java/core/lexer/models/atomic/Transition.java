package core.lexer.models.atomic;

import lombok.Getter;

/**
 * Represents a directed transition between two states in a finite automaton. A transition is
 * triggered by a specific {@link Symbol}.
 *
 * @author Generated
 * @version 1.0
 */
@Getter
public class Transition {
    private final State source;
    private final State target;
    private final Symbol symbol;

    /**
     * Constructs a new Transition.
     *
     * @param source the state from which the transition originates
     * @param target the state to which the transition leads
     * @param symbol the symbol that enables this transition
     */
    public Transition(State source, State target, Symbol symbol) {
        this.source = source;
        this.target = target;
        this.symbol = symbol;
    }
}
