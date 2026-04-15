package models.automata;

import java.util.*;
import models.atomic.State;
import models.atomic.Transition;

public class AFNDE {
    private final String tokenName;
    private final State startState;
    private final State finalState; // Pode ser nulo em scanners mestres

    public AFNDE(String tokenName, State startState, State finalState) {
        this.tokenName = tokenName;
        this.startState = startState;
        this.finalState = finalState;
    }

    // Getters básicos
    public String getTokenName() { return tokenName; }
    public State getStartState() { return startState; }
    public State getFinalState() { return finalState; }

    /**
     * Retorna todos os estados do autômato de forma eficiente.
     * Útil para conversores e minimizadores.
     */
    public Set<State> getAllStates() {
        Set<State> visited = new LinkedHashSet<>();
        Deque<State> stack = new ArrayDeque<>();
        
        stack.push(startState);
        visited.add(startState);

        while (!stack.isEmpty()) {
            State current = stack.pop();
            for (Transition t : current.getTransitions()) {
                if (visited.add(t.getTarget())) {
                    stack.push(t.getTarget());
                }
            }
        }
        return visited;
    }

    @Override
    public String toString() {
        // Capacidade inicial estimada para evitar redimensionamento do buffer
        StringBuilder sb = new StringBuilder(1024);
        sb.append("=== AFNDE: ").append(tokenName).append(" ===\n");
        sb.append("Start State: q").append(startState.getId()).append("\n");
        
        sb.append("Final State: ");
        if (finalState != null) {
            sb.append("q").append(finalState.getId()).append("\n");
        } else {
            sb.append("Multiple (Master Scanner)\n");
        }
        
        sb.append("Transitions:\n");

        // Usar Set de objetos evita o autoboxing de Integer id
        Set<State> visited = new HashSet<>();
        Deque<State> queue = new ArrayDeque<>();

        queue.add(startState);
        visited.add(startState);

        while (!queue.isEmpty()) {
            State current = queue.poll();
            
            sb.append("  q").append(current.getId());
            if (current.isFinal() || current == finalState) {
                sb.append(" [FINAL]");
            }
            sb.append(":\n");

            List<Transition> transitions = current.getTransitions();
            if (transitions.isEmpty()) {
                sb.append("    (no transitions)\n");
            } else {
                for (Transition t : transitions) {
                    sb.append("    --(").append(t.getSymbol()).append(")--> q")
                      .append(t.getTarget().getId()).append("\n");
                    
                    if (visited.add(t.getTarget())) {
                        queue.add(t.getTarget());
                    }
                }
            }
        }
        sb.append("=======================\n");
        return sb.toString();
    }
}