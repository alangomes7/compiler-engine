package models.automata;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import models.atomic.State;
import models.atomic.Transition;

public class AFND {
    private final String tokenName;
    private final State startState;
    private final Set<State> finalStates;

    public AFND(String tokenName, State startState, Set<State> finalStates) {
        this.tokenName = tokenName;
        this.startState = startState;
        // Otimização: Garante que o set nunca seja nulo, simplificando checagens futuras
        this.finalStates = finalStates != null ? finalStates : Collections.emptySet();
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

    /**
     * Retorna todos os estados alcançáveis.
     * Facilita a vida das classes de conversão (AFNDtoAFD).
     */
    public Set<State> getAllStates() {
        Set<State> visited = new LinkedHashSet<>(); // Mantém previsibilidade na ordem
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
        // Capacidade inicial para evitar cópias de array interno do StringBuilder
        StringBuilder sb = new StringBuilder(1024);
        sb.append("=== AFND: ").append(tokenName).append(" ===\n");
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

        // Uso de State no lugar de Integer evita Autoboxing
        Set<State> visited = new HashSet<>();
        Deque<State> queue = new ArrayDeque<>();

        queue.add(startState);
        visited.add(startState);

        while (!queue.isEmpty()) {
            State current = queue.poll();
            
            sb.append("  q").append(current.getId());
            
            // Checagem simplificada (finalStates nunca é nulo aqui)
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
                    sb.append("    --(").append(t.getSymbol()).append(")--> q").append(target.getId()).append("\n");
                    
                    // Condicional combinada com a inserção no Set (operação O(1))
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