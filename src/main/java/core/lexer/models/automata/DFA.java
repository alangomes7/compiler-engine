package core.lexer.models.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DFA extends Automaton {

    private final Map<Integer, Map<Symbol, State>> transitionTable;

    public DFA(String tokenName) {
        super(tokenName);
        this.transitionTable = new HashMap<>();
    }

    @Override
    public void addTransition(Transition transition) {
        super.addTransition(transition);
        transitionTable
                .computeIfAbsent(transition.getSource().getId(), k -> new HashMap<>())
                .put(transition.getSymbol(), transition.getTarget());
    }

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
