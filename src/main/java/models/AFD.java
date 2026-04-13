package models;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class AFD {
    private final String tokenName;
    private final State startState;
    private final Set<State> finalStates;

    public AFD(String tokenName, State startState, Set<State> finalStates) {
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

    public State getNextState(State current, char c) {
        String symbol = String.valueOf(c);
        for (Transition t : current.getTransitions()) {
            if (t.getSymbol().equals(symbol)) {
                return t.getTarget();
            }
        }
        return null;
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