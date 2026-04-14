package models.automata;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import models.atomic.State;
import models.atomic.Transition;

public class AFND {
    private String tokenName;
    private State startState;
    private Set<State> finalStates;

    public AFND(String tokenName, State startState, Set<State> finalStates) {
        this.tokenName = tokenName;
        this.startState = startState;
        this.finalStates = finalStates;
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
        sb.append("=== AFND: ").append(tokenName).append(" ===\n");
        sb.append("Start State: q").append(startState.getId()).append("\n");
        
        // Handle final states display
        if (finalStates != null && !finalStates.isEmpty()) {
            sb.append("Final States: ");
            for (State s : finalStates) {
                sb.append("q").append(s.getId()).append(" ");
            }
            sb.append("\n");
        } else {
            sb.append("Final States: Multiple (Master Scanner)\n");
        }
        
        sb.append("Transitions:\n");

        // BFS traversal to prevent infinite loops from cyclic transitions
        Queue<State> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(startState);
        visited.add(startState.getId());

        while (!queue.isEmpty()) {
            State current = queue.poll();
            
            sb.append("  q").append(current.getId());
            
            // Check if current state is in the set of final states or natively marked as final
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