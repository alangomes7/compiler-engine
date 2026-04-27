package core.lexer.models.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Transition;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Nondeterministic Finite Automaton (NFA). From a given state and symbol, multiple transitions may
 * exist.
 *
 * @author Generated
 * @version 1.0
 */
public class NFA extends Automaton {

    /**
     * Constructs an NFA for a specific token name.
     *
     * @param tokenName the name of the token recognised by this NFA
     */
    public NFA(String tokenName) {
        super(tokenName);
    }

    /**
     * Returns the set of states reachable from the current state on the given character.
     *
     * @param current the current state
     * @param c the input character
     * @return a set of next states (may be empty)
     */
    public Set<State> getNextStates(State current, char c) {
        String symbolStr = String.valueOf(c);

        return transitions.stream()
                .filter(
                        t ->
                                t.getSource().equals(current)
                                        && t.getSymbol().getValue().equals(symbolStr))
                .map(Transition::getTarget)
                .collect(Collectors.toSet());
    }

    /**
     * Returns a string representation of this NFA.
     *
     * @return a formatted string containing alphabet, start/final states, and transitions
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AFND: ").append(tokenName).append(" ===\n");
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
