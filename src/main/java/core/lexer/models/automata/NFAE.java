package core.lexer.models.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Transition;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import models.atomic.Constants;

public class NFAE extends Automaton {

    private static final String EPSILON = Constants.EPSILON;

    public NFAE(String tokenName) {
        super(tokenName);
    }

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
