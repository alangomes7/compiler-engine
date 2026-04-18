package core.lexer.models.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Transition;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AFND {
    private final String tokenName;
    private final State startState;
    private final Set<State> finalStates;
    private final Alphabet alphabet;

    public AFND(String tokenName, State startState, Set<State> finalStates) {
        this.tokenName = tokenName;
        this.startState = startState;
        this.finalStates = finalStates != null ? finalStates : Collections.emptySet();

        this.alphabet = new Alphabet();
        computeAlphabet();
    }

    private void computeAlphabet() {
        for (State state : getAllStates()) {
            for (Transition t : state.getTransitions()) {
                this.alphabet.addSymbol(t.getSymbol());
            }
        }
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

    public Set<State> getAllStates() {
        Set<State> visited = new LinkedHashSet<>();
        Deque<State> queue = new ArrayDeque<>();

        queue.add(startState);
        visited.add(startState);

        while (!queue.isEmpty()) {
            State current = queue.poll();
            for (Transition t : current.getTransitions()) {
                if (visited.add(t.getTarget())) {
                    queue.add(t.getTarget());
                }
            }
        }
        return visited;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("=== AFND: ").append(tokenName).append(" ===\n");
        sb.append("Alphabet: ").append(alphabet.getSymbols()).append("\n");
        sb.append("Start State: q").append(startState.getId()).append("\n");

        if (!finalStates.isEmpty()) {
            sb.append("Final States: ");
            for (State s : finalStates) {
                sb.append("q").append(s.getId()).append(" ");
            }
            sb.append("\n");
        } else {
            sb.append("Final States: Multiple (Master Scanner)\n");
        }

        sb.append("Transitions:\n");

        Set<State> visited = new HashSet<>();
        Deque<State> queue = new ArrayDeque<>();

        queue.add(startState);
        visited.add(startState);

        while (!queue.isEmpty()) {
            State current = queue.poll();

            sb.append("  q").append(current.getId());

            if (current.isFinal() || finalStates.contains(current)) {
                sb.append(" [FINAL]");
            }
            sb.append(":\n");

            List<Transition> transitions = current.getTransitions();
            if (transitions.isEmpty()) {
                sb.append("    (no transitions)\n");
            } else {
                for (Transition t : transitions) {
                    State target = t.getTarget();
                    sb.append("    --(")
                            .append(t.getSymbol().getValue())
                            .append(")--> q")
                            .append(target.getId())
                            .append("\n");

                    if (visited.add(target)) {
                        queue.add(target);
                    }
                }
            }
        }
        sb.append("=======================\n");
        return sb.toString();
    }
}
