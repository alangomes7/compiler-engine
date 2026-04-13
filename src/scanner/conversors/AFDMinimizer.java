package scanner.conversors;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.AFD;
import models.State;
import models.Transition;

public class AFDMinimizer {

    private int minStateCounter = 0;

    public AFD minimize(AFD afd) {
        minStateCounter = 0;

        // 1. Collect states
        Set<State> allStates = getAllStates(afd.getStartState());

        // Build alphabet index (String -> int)
        Map<String, Integer> symbolIndex = new HashMap<>();
        List<String> alphabet = new ArrayList<>();

        for (State s : allStates) {
            for (Transition t : s.getTransitions()) {
                String sym = t.getSymbol();
                if (!symbolIndex.containsKey(sym)) {
                    symbolIndex.put(sym, alphabet.size());
                    alphabet.add(sym);
                }
            }
        }

        int alphabetSize = alphabet.size();

        // Build indexed transition table: State -> State[]
        Map<State, State[]> transitionTable = new HashMap<>(allStates.size());

        for (State s : allStates) {
            State[] row = new State[alphabetSize];

            for (Transition t : s.getTransitions()) {
                int idx = symbolIndex.get(t.getSymbol());
                row[idx] = t.getTarget();
            }

            transitionTable.put(s, row);
        }

        // 2. Initial partitions
        List<List<State>> partitions = new ArrayList<>();
        Map<State, Integer> stateToPartition = new HashMap<>(allStates.size());

        Map<String, List<State>> finalGroups = new HashMap<>();
        List<State> nonFinal = new ArrayList<>();

        for (State s : allStates) {
            if (isStateFinal(s, afd)) {
                String token = (s.getAcceptedToken() != null)
                        ? s.getAcceptedToken()
                        : "FINAL_NULL";

                finalGroups.computeIfAbsent(token, k -> new ArrayList<>()).add(s);
            } else {
                nonFinal.add(s);
            }
        }

        int pid = 0;

        if (!nonFinal.isEmpty()) {
            partitions.add(nonFinal);
            for (State s : nonFinal) stateToPartition.put(s, pid);
            pid++;
        }

        for (List<State> group : finalGroups.values()) {
            partitions.add(group);
            for (State s : group) stateToPartition.put(s, pid);
            pid++;
        }

        // 3. Refinement loop
        boolean changed = true;

        while (changed) {
            changed = false;

            List<List<State>> newPartitions = new ArrayList<>();
            Map<State, Integer> newStateToPartition = new HashMap<>(allStates.size());

            int newPid = 0;

            for (List<State> group : partitions) {

                Map<Long, List<State>> splits = new HashMap<>();

                for (State s : group) {
                    State[] trans = transitionTable.get(s);

                    // Faster hash (long reduces collisions)
                    long hash = 1;

                    for (int i = 0; i < alphabetSize; i++) {
                        State target = trans[i];

                        int targetPid = (target == null)
                                ? -1
                                : stateToPartition.get(target);

                        hash = hash * 31 + targetPid;
                    }

                    splits.computeIfAbsent(hash, k -> new ArrayList<>()).add(s);
                }

                for (List<State> sub : splits.values()) {
                    newPartitions.add(sub);

                    for (State s : sub) {
                        newStateToPartition.put(s, newPid);
                    }
                    newPid++;
                }

                if (splits.size() > 1) {
                    changed = true;
                }
            }

            partitions = newPartitions;
            stateToPartition = newStateToPartition;
        }

        // 4. Build minimized DFA
        Map<Integer, State> newStates = new HashMap<>(partitions.size());
        State newStart = null;
        Set<State> newFinals = new HashSet<>();

        for (int i = 0; i < partitions.size(); i++) {
            List<State> group = partitions.get(i);

            State newState = new State(minStateCounter++);
            State rep = group.get(0);

            boolean isFinal = isStateFinal(rep, afd);
            newState.setFinal(isFinal);

            if (isFinal) {
                newState.setAcceptedToken(rep.getAcceptedToken());
                newFinals.add(newState);
            }

            newStates.put(i, newState);

            if (group.contains(afd.getStartState())) {
                newStart = newState;
            }
        }

        // 5. Rebuild transitions (indexed = fast)
        for (int i = 0; i < partitions.size(); i++) {
            State newSource = newStates.get(i);
            State rep = partitions.get(i).get(0);

            State[] trans = transitionTable.get(rep);

            for (int j = 0; j < alphabetSize; j++) {
                State oldTarget = trans[j];

                if (oldTarget != null) {
                    int targetPid = stateToPartition.get(oldTarget);
                    newSource.addTransition(alphabet.get(j), newStates.get(targetPid));
                }
            }
        }

        return new AFD(afd.getTokenName() + "_MIN", newStart, newFinals);
    }

    // BFS
    private Set<State> getAllStates(State start) {
        Set<State> visited = new HashSet<>();
        ArrayDeque<State> queue = new ArrayDeque<>();

        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            State s = queue.poll();

            for (Transition t : s.getTransitions()) {
                if (visited.add(t.getTarget())) {
                    queue.add(t.getTarget());
                }
            }
        }

        return visited;
    }

    private boolean isStateFinal(State s, AFD afd) {
        return s.isFinal() ||
                (afd.getFinalStates() != null && afd.getFinalStates().contains(s));
    }
}