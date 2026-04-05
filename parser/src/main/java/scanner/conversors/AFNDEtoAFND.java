package scanner.conversors;

import models.AFND;
import models.AFNDE;
import models.State;
import models.Transition;

import java.util.*;

public class AFNDEtoAFND {

    public static final String EPSILON = "ε";

    /**
     * Converts an AFNDE (NFA with epsilon transitions) into an AFND (NFA without epsilon transitions).
     * * @param afnde The original automaton containing epsilon transitions.
     * @return A new AFND instance representing the equivalent automaton without epsilon transitions.
     */
    public AFND convert(AFNDE afnde) {
        // 1. Gather all reachable states from the original AFNDE
        Set<State> allOldStates = getAllReachableStates(afnde.getStartState());

        // 2. Gather all alphabet symbols (excluding EPSILON)
        Set<String> alphabet = new HashSet<>();
        for (State s : allOldStates) {
            for (Transition t : s.getTransitions()) {
                if (!t.getSymbol().equals(EPSILON)) {
                    alphabet.add(t.getSymbol());
                }
            }
        }

        // 3. Create a mapping from old States to new States
        Map<State, State> oldToNew = new HashMap<>();
        for (State oldState : allOldStates) {
            oldToNew.put(oldState, new State(oldState.getId()));
        }

        // 4. Compute new transitions and final states
        for (State oldState : allOldStates) {
            State newState = oldToNew.get(oldState);
            
            // Get all states reachable from 'oldState' using only EPSILON
            Set<State> closureQ = getEpsilonClosure(oldState);

            // A new state is final if its epsilon closure contains at least one final state
            boolean isFinal = false;
            for (State cState : closureQ) {
                if (cState.isFinal()) {
                    isFinal = true;
                    break;
                }
            }
            newState.setFinal(isFinal);

            // Compute new transitions for each symbol in the alphabet
            for (String symbol : alphabet) {
                // Where can we go by reading 'symbol' from anywhere inside the epsilon closure?
                Set<State> moveResult = getMove(closureQ, symbol);
                
                // After reading 'symbol', where can we go using EPSILON?
                Set<State> targetClosure = new HashSet<>();
                for (State ms : moveResult) {
                    targetClosure.addAll(getEpsilonClosure(ms));
                }

                // Add direct transitions to the new state, avoiding duplicates
                for (State targetOld : targetClosure) {
                    State targetNew = oldToNew.get(targetOld);
                    if (!hasTransition(newState, symbol, targetNew)) {
                        newState.addTransition(symbol, targetNew);
                    }
                }
            }
        }

        // 5. Construct and return the new AFND
        State newStart = oldToNew.get(afnde.getStartState());

        // Filter out states that became unreachable after removing EPSILON
        Set<State> reachableInNew = getAllReachableStates(newStart);

        Set<State> finalStates = new HashSet<>();
        for (State state : reachableInNew) {
            if (state.isFinal()) {
                finalStates.add(state);
            }
        }

        return new AFND(afnde.getTokenName() + "_AFND", newStart, finalStates);
    }

    /**
     * Traverses the graph to find all states reachable from the start state.
     */
    private Set<State> getAllReachableStates(State start) {
        Set<State> visited = new HashSet<>();
        Queue<State> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            State current = queue.poll();
            for (Transition t : current.getTransitions()) {
                if (!visited.contains(t.getTarget())) {
                    visited.add(t.getTarget());
                    queue.add(t.getTarget());
                }
            }
        }
        return visited;
    }

    /**
     * Computes the epsilon closure (Fecho-Epsilon) for a given state.
     */
    private Set<State> getEpsilonClosure(State state) {
        Set<State> closure = new HashSet<>();
        Queue<State> queue = new LinkedList<>();
        queue.add(state);
        closure.add(state);

        while (!queue.isEmpty()) {
            State current = queue.poll();
            for (Transition t : current.getTransitions()) {
                if (t.getSymbol().equals(EPSILON) && !closure.contains(t.getTarget())) {
                    closure.add(t.getTarget());
                    queue.add(t.getTarget());
                }
            }
        }
        return closure;
    }

    /**
     * Returns the set of states reachable from a given set of states by consuming a specific symbol.
     */
    private Set<State> getMove(Set<State> states, String symbol) {
        Set<State> result = new HashSet<>();
        for (State s : states) {
            for (Transition t : s.getTransitions()) {
                if (t.getSymbol().equals(symbol)) {
                    result.add(t.getTarget());
                }
            }
        }
        return result;
    }

    /**
     * Checks if a transition already exists to prevent duplicate parallel edges.
     */
    private boolean hasTransition(State state, String symbol, State target) {
        for (Transition t : state.getTransitions()) {
            if (t.getSymbol().equals(symbol) && t.getTarget().getId() == target.getId()) {
                return true;
            }
        }
        return false;
    }
}