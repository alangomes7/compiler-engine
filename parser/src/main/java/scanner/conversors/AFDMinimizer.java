package scanner.conversors;

import models.AFD;
import models.State;
import models.Transition;

import java.util.*;

public class AFDMinimizer {

    private int minStateCounter = 0;

    /**
     * Minimizes a deterministic finite automaton (AFD) using Moore's Partition Refinement Algorithm.
     * @param afd The deterministic automaton to minimize.
     * @return A new AFD object representing the minimized DFA.
     */
    public AFD minimize(AFD afd) {
        minStateCounter = 0;

        // 1. Gather all reachable states and the alphabet
        Set<State> allStates = getAllStates(afd.getStartState());
        Set<String> alphabet = new HashSet<>();
        for (State s : allStates) {
            for (Transition t : s.getTransitions()) {
                alphabet.add(t.getSymbol());
            }
        }

        // 2. Initial Partition: P0 = { Final States, Non-Final States }
        List<Set<State>> partitions = new ArrayList<>();
        Set<State> finalStatesSet = new HashSet<>();
        Set<State> nonFinalStatesSet = new HashSet<>();

        for (State s : allStates) {
            if (isStateFinal(s, afd)) {
                finalStatesSet.add(s);
            } else {
                nonFinalStatesSet.add(s);
            }
        }

        if (!finalStatesSet.isEmpty()) partitions.add(finalStatesSet);
        if (!nonFinalStatesSet.isEmpty()) partitions.add(nonFinalStatesSet);

        // 3. Partition Refinement Loop
        boolean changed = true;
        while (changed) {
            changed = false;
            List<Set<State>> newPartitions = new ArrayList<>();

            for (Set<State> group : partitions) {
                // Group states by their behavior (where their transitions lead)
                // The "signature" maps a Symbol -> The Index of the target Partition
                Map<Map<String, Integer>, Set<State>> splits = new HashMap<>();

                for (State s : group) {
                    Map<String, Integer> signature = new HashMap<>();
                    for (String sym : alphabet) {
                        State target = getTarget(s, sym);
                        int targetPartitionIdx = getPartitionIndex(partitions, target);
                        signature.put(sym, targetPartitionIdx);
                    }

                    splits.putIfAbsent(signature, new HashSet<>());
                    splits.get(signature).add(s);
                }

                // Add the newly formed sub-groups to our next iteration
                newPartitions.addAll(splits.values());

                // If a group was split into more than one piece, we must loop again
                if (splits.size() > 1) {
                    changed = true;
                }
            }
            partitions = newPartitions;
        }

        // 4. Construct the Minimized AFD
        Map<Set<State>, State> partitionToNewState = new HashMap<>();
        State minStart = null;
        Set<State> minFinalStates = new HashSet<>();

        // Create new states for each partition
        for (Set<State> partition : partitions) {
            State newState = new State(minStateCounter++);
            
            // A partition is final if it contains final states 
            // (since we separated them in step 2, if one is final, all are)
            boolean isFinal = false;
            for (State s : partition) {
                if (isStateFinal(s, afd)) {
                    isFinal = true;
                    break;
                }
            }
            
            newState.setFinal(isFinal);
            if (isFinal) {
                minFinalStates.add(newState);
            }

            partitionToNewState.put(partition, newState);

            // Check if this partition contains the original start state
            if (partition.contains(afd.getStartState())) {
                minStart = newState;
            }
        }

        // 5. Reconstruct Transitions
        for (Set<State> partition : partitions) {
            State newSource = partitionToNewState.get(partition);
            // Pick any representative state from the partition (they all behave the same)
            State representative = partition.iterator().next();

            for (String sym : alphabet) {
                State oldTarget = getTarget(representative, sym);
                if (oldTarget != null) {
                    // Find which partition the target belongs to
                    for (Set<State> targetPartition : partitions) {
                        if (targetPartition.contains(oldTarget)) {
                            State newTarget = partitionToNewState.get(targetPartition);
                            newSource.addTransition(sym, newTarget);
                            break;
                        }
                    }
                }
            }
        }

        return new AFD(afd.getTokenName() + "_MIN", minStart, minFinalStates);
    }

    // --- Helper Methods ---

    /**
     * Traverses the graph to find all states reachable from the start state.
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
     * Checks if a state is final, either natively or via the AFD final states set.
     */
    private boolean isStateFinal(State state, AFD afd) {
        return state.isFinal() || (afd.getFinalStates() != null && afd.getFinalStates().contains(state));
    }

    /**
     * Returns the target state for a given symbol, or null if no transition exists.
     */
    private State getTarget(State state, String symbol) {
        for (Transition t : state.getTransitions()) {
            if (t.getSymbol().equals(symbol)) {
                return t.getTarget();
            }
        }
        return null;
    }

    /**
     * Finds the index of the partition that contains the target state.
     * Returns -1 if the target is null (representing a "dead state" or missing transition).
     */
    private int getPartitionIndex(List<Set<State>> partitions, State target) {
        if (target == null) return -1;
        for (int i = 0; i < partitions.size(); i++) {
            if (partitions.get(i).contains(target)) {
                return i;
            }
        }
        return -1;
    }
}