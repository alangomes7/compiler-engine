package scanner.conversors;

import models.AFD;
import models.AFND;
import models.State;
import models.Transition;

import java.util.*;

public class AFNDtoAFD {

    private int dfaStateCounter = 0;

    /**
     * Converts an AFND (NFA) into an AFD (DFA) using the Subset Construction algorithm.
     * @param afnd The Non-Deterministic automaton.
     * @return A new AFD object that is strictly deterministic.
     */
    public AFD convert(AFND afnd) {
        dfaStateCounter = 0;
        
        // 1. Map all original states by ID and gather the alphabet
        Map<Integer, State> originalStates = new HashMap<>();
        Set<String> alphabet = new HashSet<>();
        
        Set<State> allAfndStates = getAllStates(afnd.getStartState());
        for (State s : allAfndStates) {
            originalStates.put(s.getId(), s);
            for (Transition t : s.getTransitions()) {
                alphabet.add(t.getSymbol());
            }
        }

        // 2. Initialize DFA construction structures
        // We use Set<Integer> of state IDs to uniquely identify new DFA states
        Map<Set<Integer>, State> dfaStateMap = new HashMap<>();
        Queue<Set<Integer>> unprocessedSubsets = new LinkedList<>();
        
        // The start state of the DFA is just the subset containing the start state of the AFND
        Set<Integer> startSubset = new HashSet<>();
        startSubset.add(afnd.getStartState().getId());
        
        State dfaStart = new State(dfaStateCounter++);
        dfaStart.setFinal(isStateFinal(afnd.getStartState(), afnd));
                         
        dfaStateMap.put(startSubset, dfaStart);
        unprocessedSubsets.add(startSubset);

        Set<State> dfaFinalStates = new HashSet<>();
        if (dfaStart.isFinal()) {
            dfaFinalStates.add(dfaStart);
        }

        // 3. Process subsets (Subset Construction Algorithm)
        while (!unprocessedSubsets.isEmpty()) {
            Set<Integer> currentSubset = unprocessedSubsets.poll();
            State currentDfaState = dfaStateMap.get(currentSubset);

            // Check where we can go with each symbol of the alphabet
            for (String symbol : alphabet) {
                Set<Integer> nextSubset = new HashSet<>();
                
                // Find all reachable AFND states from any state in currentSubset using this symbol
                for (Integer stateId : currentSubset) {
                    State s = originalStates.get(stateId);
                    for (Transition t : s.getTransitions()) {
                        if (t.getSymbol().equals(symbol)) {
                            nextSubset.add(t.getTarget().getId());
                        }
                    }
                }

                // If the transition leads somewhere
                if (!nextSubset.isEmpty()) {
                    State targetDfaState;
                    
                    // If we haven't seen this subset before, create a new DFA state
                    if (!dfaStateMap.containsKey(nextSubset)) {
                        targetDfaState = new State(dfaStateCounter++);
                        
                        // It is final if ANY of its internal AFND states are final
                        boolean isFinal = false;
                        for (Integer id : nextSubset) {
                            if (isStateFinal(originalStates.get(id), afnd)) {
                                isFinal = true;
                                break; // Once final, always final
                            }
                        }
                        
                        targetDfaState.setFinal(isFinal);
                        if (isFinal) {
                            dfaFinalStates.add(targetDfaState);
                        }
                        
                        dfaStateMap.put(nextSubset, targetDfaState);
                        unprocessedSubsets.add(nextSubset);
                    } else {
                        // Subset already exists, just link to it
                        targetDfaState = dfaStateMap.get(nextSubset);
                    }
                    
                    // Add deterministic transition
                    currentDfaState.addTransition(symbol, targetDfaState);
                }
            }
        }

        // 4. Return the new AFD instance
        return new AFD(afnd.getTokenName() + "_to_AFD", dfaStart, dfaFinalStates);
    }

    /**
     * Helper to get all states connected in the graph.
     */
    private Set<State> getAllStates(State start) {
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
     * Helper to check if a state is final, either by its internal flag or the AFND final list.
     */
    private boolean isStateFinal(State state, AFND afnd) {
        return state.isFinal() || (afnd.getFinalStates() != null && afnd.getFinalStates().contains(state));
    }
}