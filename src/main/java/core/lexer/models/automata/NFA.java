package core.lexer.models.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Transition;
import java.util.Set;
import java.util.stream.Collectors;

public class NFA extends Automaton {

    public NFA(String tokenName) {
        super(tokenName);
    }

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
