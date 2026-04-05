package models;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class AFNDE {
    private String tokenName;
    private State startState;
    private State finalState;

    public AFNDE(String tokenName, State startState, State finalState) {
        this.tokenName = tokenName;
        this.startState = startState;
        this.finalState = finalState;
    }

    public String getTokenName() {
        return tokenName;
    }

    public State getStartState() {
        return startState;
    }

    public State getFinalState() {
        return finalState;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AFNDE: ").append(tokenName).append(" ===\n");
        sb.append("Start State: q").append(startState.getId()).append("\n");
        
        // Handle the master scanner which passes 'null' for the final state
        if (finalState != null) {
            sb.append("Final State: q").append(finalState.getId()).append("\n");
        } else {
            sb.append("Final State: Multiple (Master Scanner)\n");
        }
        
        sb.append("Transitions:\n");

        // BFS traversal to prevent infinite loops from cyclic transitions (like Kleene Star)
        Queue<State> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(startState);
        visited.add(startState.getId());

        while (!queue.isEmpty()) {
            State current = queue.poll();
            
            sb.append("  q").append(current.getId());
            if (current.isFinal()) {
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