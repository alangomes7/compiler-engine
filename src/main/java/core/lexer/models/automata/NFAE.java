package core.lexer.models.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Transition;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import models.atomic.Constants;

/**
 * Nondeterministic Finite Automaton with Epsilon Transitions (ε-NFA). Supports transitions that
 * consume no input symbol (epsilon).
 *
 * @author Generated
 * @version 1.0
 */
public class NFAE extends Automaton {

    /** The symbol used to represent an epsilon transition. */
    private static final String EPSILON = Constants.EPSILON;

    /**
     * Constructs an ε-NFA for a specific token name.
     *
     * @param tokenName the name of the token recognised by this ε-NFA
     */
    public NFAE(String tokenName) {
        super(tokenName);
    }

    /**
     * Computes the epsilon-closure of a given state. The epsilon-closure is the set of all states
     * reachable from the given state using zero or more epsilon transitions.
     *
     * @param current the starting state
     * @return a set containing the closure (always includes {@code current})
     */
    public Set<State> getEpsilonClosure(State current) {
        Set<State> closure = new HashSet<>();
        closure.add(current);

        boolean added;
        do {
            added = false;
            Set<State> newStates = new HashSet<>();
            for (State s : closure) {
                Set<State> reachableViaEpsilon =
                        transitions.stream()
                                .filter(
                                        t ->
                                                t.getSource().equals(s)
                                                        && t.getSymbol().getValue().equals(EPSILON))
                                .map(Transition::getTarget)
                                .collect(Collectors.toSet());

                for (State reachable : reachableViaEpsilon) {
                    if (!closure.contains(reachable)) {
                        newStates.add(reachable);
                        added = true;
                    }
                }
            }
            closure.addAll(newStates);
        } while (added);

        return closure;
    }

    /**
     * Returns a string representation of this ε-NFA.
     *
     * @return a formatted string containing alphabet, start/final states, and transitions
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AFNDE: ").append(tokenName).append(" ===\n");
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
