package core.lexer.models.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Deterministic Finite Automaton (DFA). For each state and input symbol, there is at most one
 * transition. Uses a transition table for O(1) lookup.
 *
 * @author Generated
 * @version 1.0
 */
public class DFA extends Automaton {

    private final Map<Integer, Map<Symbol, State>> transitionTable;

    /**
     * Constructs a DFA for a specific token name.
     *
     * @param tokenName the name of the token recognised by this DFA
     */
    public DFA(String tokenName) {
        super(tokenName);
        this.transitionTable = new HashMap<>();
    }

    /**
     * Adds a transition to the automaton and updates the transition table.
     *
     * @param transition the transition to add
     */
    @Override
    public void addTransition(Transition transition) {
        super.addTransition(transition);
        transitionTable
                .computeIfAbsent(transition.getSource().getId(), k -> new HashMap<>())
                .put(transition.getSymbol(), transition.getTarget());
    }

    /**
     * Returns the next state reached from the given state on input character.
     *
     * @param current the current state
     * @param c the input character (must belong to the alphabet)
     * @return the next state, or null if no transition exists or symbol not in alphabet
     */
    public State getNextState(State current, char c) {
        Symbol inputSymbol = new Symbol(String.valueOf(c));

        if (!alphabet.getSymbols().contains(inputSymbol)) {
            return null;
        }

        Map<Symbol, State> stateTransitions = transitionTable.get(current.getId());
        if (stateTransitions != null) {
            return stateTransitions.get(inputSymbol);
        }
        return null;
    }

    /**
     * Returns a string representation of this DFA.
     *
     * @return a formatted string containing alphabet, start/final states, and transitions
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AFD: ").append(tokenName).append(" ===\n");
        sb.append("Alphabet: ").append(alphabet.getSymbols()).append("\n");

        sb.append("Start States: ").append(getInitialStates()).append("\n");
        sb.append("Final States: ").append(getFinalStates()).append("\n");
        sb.append("Transitions:\n");

        for (State s : states) {
            sb.append("  ").append(s).append(":\n");

            var stateTransitions =
                    transitions.stream()
                            .filter(t -> t.getSource().equals(s))
                            .collect(Collectors.toList());

            if (stateTransitions.isEmpty()) {
                sb.append("    (no transitions)\n");
            } else {
                for (Transition t : stateTransitions) {
                    sb.append("    --(")
                            .append(t.getSymbol().getValue())
                            .append(")--> q")
                            .append(t.getTarget().getId())
                            .append("\n");
                }
            }
        }
        sb.append("=======================\n");
        return sb.toString();
    }
}
