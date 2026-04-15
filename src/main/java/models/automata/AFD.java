package models.automata;

import java.util.*;
import models.atomic.State;
import models.atomic.Transition;

public class AFD {
    private final String tokenName;
    private final State startState;
    private final Set<State> finalStates;
    
    // Cache de transições para acesso O(1)
    // Map<ID_do_Estado, Map<Simbolo, Estado_Destino>>
    private final Map<Integer, Map<String, State>> fastTransitionTable = new HashMap<>();

    public AFD(String tokenName, State startState, Set<State> finalStates) {
        this.tokenName = tokenName;
        this.startState = startState;
        this.finalStates = finalStates;
        precomputeTransitions();
    }

    /**
     * Pré-processa as transições de todos os estados alcançáveis
     * para garantir que getNextState seja O(1).
     */
    private void precomputeTransitions() {
        Queue<State> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(startState);
        visited.add(startState.getId());

        while (!queue.isEmpty()) {
            State current = queue.poll();
            Map<String, State> stateMap = new HashMap<>();
            
            for (Transition t : current.getTransitions()) {
                stateMap.put(t.getSymbol(), t.getTarget());
                
                if (visited.add(t.getTarget().getId())) {
                    queue.add(t.getTarget());
                }
            }
            fastTransitionTable.put(current.getId(), stateMap);
        }
    }

    // Otimizado: Busca no mapa em vez de iterar na lista
    public State getNextState(State current, char c) {
        Map<String, State> transitions = fastTransitionTable.get(current.getId());
        if (transitions != null) {
            return transitions.get(String.valueOf(c));
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AFD: ").append(tokenName).append(" ===\n");
        sb.append("Start State: q").append(startState.getId()).append("\n");
        
        // Handle final states display
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

        // BFS traversal to print states cleanly
        Queue<State> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(startState);
        visited.add(startState.getId());

        while (!queue.isEmpty()) {
            State current = queue.poll();
            
            sb.append("  q").append(current.getId());
            
            // Check if current state is natively marked as final or in the final states set
            if (current.isFinal() || (finalStates != null && finalStates.contains(current))) {
                sb.append(" [FINAL]");
            }
            sb.append(":\n");

            if (current.getTransitions().isEmpty()) {
                sb.append("    (no transitions)\n");
            }

            for (Transition t : current.getTransitions()) {
                sb.append("    --(").append(t.getSymbol()).append(")--> q").append(t.getTarget().getId()).append("\n");
                
                // Add unvisited target states to the queue
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