package core.lexer.models.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class AFD {
    private final String tokenName;
    private final State startState;
    private final Set<State> finalStates;
    private final Alphabet alphabet;

    private final Map<Integer, Map<Symbol, State>> transitionTable = new HashMap<>();

    public AFD(String tokenName, State startState, Set<State> finalStates) {
        this.tokenName = tokenName;
        this.startState = startState;
        this.finalStates = finalStates;
        this.alphabet = new Alphabet();

        precomputeTransitions();
    }

    private void precomputeTransitions() {
        Queue<State> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(startState);
        visited.add(startState.getId());

        while (!queue.isEmpty()) {
            State current = queue.poll();
            Map<Symbol, State> stateMap = new HashMap<>();

            for (Transition t : current.getTransitions()) {

                this.alphabet.addSymbol(t.getSymbol());
                stateMap.put(t.getSymbol(), t.getTarget());

                if (visited.add(t.getTarget().getId())) {
                    queue.add(t.getTarget());
                }
            }
            transitionTable.put(current.getId(), stateMap);
        }
    }

    public State getNextState(State current, char c) {
        Symbol inputSymbol = new Symbol(String.valueOf(c));

        if (!alphabet.getSymbols().contains(inputSymbol)) {
            return null;
        }

        Map<Symbol, State> transitions = transitionTable.get(current.getId());
        if (transitions != null) {
            return transitions.get(inputSymbol);
        }
        return null;
    }

    public String getTokenName() {
        return tokenName;
    }

    public State getStartState() {
        return startState;
    }

    public Set<State> getFinalStates() {
        return finalStates;
    }

    public Alphabet getAlphabet() {
        return alphabet;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AFD: ").append(tokenName).append(" ===\n");
        sb.append("Alphabet: ").append(alphabet.getSymbols()).append("\n");
        sb.append("Start State: q").append(startState.getId()).append("\n");

        if (finalStates != null && !finalStates.isEmpty()) {
            sb.append("Final States: ");
            for (State s : finalStates) {
                sb.append("q").append(s.getId()).append(" ");
            }
            sb.append("\n");
        } else {
            sb.append("Final States: None/Multiple\n");
        }

        sb.append("Transitions:\n");

        Queue<State> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(startState);
        visited.add(startState.getId());

        while (!queue.isEmpty()) {
            State current = queue.poll();
            sb.append("  q").append(current.getId());
            if (current.isFinal() || (finalStates != null && finalStates.contains(current))) {
                sb.append(" [FINAL]");
            }
            sb.append(":\n");

            if (current.getTransitions().isEmpty()) {
                sb.append("    (no transitions)\n");
            }

            for (Transition t : current.getTransitions()) {
                sb.append("    --(")
                        .append(t.getSymbol().getValue())
                        .append(")--> q")
                        .append(t.getTarget().getId())
                        .append("\n");

                if (!visited.contains(t.getTarget().getId())) {
                    visited.add(t.getTarget().getId());
                    queue.add(t.getTarget());
                }
            }
        }
        sb.append("=======================\n");
        return sb.toString();
    }
}
