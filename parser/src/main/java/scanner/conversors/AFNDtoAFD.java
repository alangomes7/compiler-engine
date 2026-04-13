package scanner.conversors;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.AFD;
import models.AFND;
import models.State;
import models.Transition;


public class AFNDtoAFD {

    private int dfaStateCounter = 0;

    public AFD convert(AFND afnd) {
        dfaStateCounter = 0;

        // 1. Precompute transition table
        Map<Integer, Map<String, Set<Integer>>> transitionTable = new HashMap<>();
        Set<String> alphabet = new HashSet<>();
        Set<Integer> nfaFinalStates = new HashSet<>();
        Map<Integer, String> stateAcceptedTokens = new HashMap<>();

        Set<State> allStates = getAllStates(afnd.getStartState());

        for (State s : allStates) {
            int id = s.getId();

            if (s.isFinal() || (afnd.getFinalStates() != null && afnd.getFinalStates().contains(s))) {
                nfaFinalStates.add(id);
                String token = s.getAcceptedToken();
                if (token != null) {
                    stateAcceptedTokens.put(id, token);
                }
            }

            Map<String, Set<Integer>> transMap = new HashMap<>();
            for (Transition t : s.getTransitions()) {
                String sym = t.getSymbol();
                alphabet.add(sym);

                transMap.computeIfAbsent(sym, k -> new HashSet<>(2))
                        .add(t.getTarget().getId());
            }

            transitionTable.put(id, transMap);
        }

        // 2. DFA structures
        Map<Set<Integer>, State> dfaStateMap = new HashMap<>();
        ArrayDeque<Set<Integer>> queue = new ArrayDeque<>();

        Set<Integer> startSubset = new HashSet<>(1);
        startSubset.add(afnd.getStartState().getId());

        State dfaStart = new State(dfaStateCounter++);
        boolean isStartFinal = nfaFinalStates.contains(afnd.getStartState().getId());

        dfaStart.setFinal(isStartFinal);
        if (isStartFinal) {
            dfaStart.setAcceptedToken(
                stateAcceptedTokens.get(afnd.getStartState().getId())
            );
        }

        dfaStateMap.put(startSubset, dfaStart);
        queue.add(startSubset);

        Set<State> dfaFinalStates = new HashSet<>();
        if (isStartFinal) {
            dfaFinalStates.add(dfaStart);
        }

        // Reusable set to reduce allocations
        Set<Integer> nextSubset = new HashSet<>();

        // 3. Subset construction
        while (!queue.isEmpty()) {
            Set<Integer> currentSubset = queue.poll();
            State currentDfaState = dfaStateMap.get(currentSubset);

            for (String symbol : alphabet) {
                nextSubset.clear();

                for (Integer stateId : currentSubset) {
                    Map<String, Set<Integer>> trans = transitionTable.get(stateId);
                    if (trans != null) {
                        Set<Integer> targets = trans.get(symbol);
                        if (targets != null) {
                            nextSubset.addAll(targets);
                        }
                    }
                }

                if (!nextSubset.isEmpty()) {
                    // IMPORTANT: must copy because we reuse nextSubset
                    Set<Integer> subsetKey = new HashSet<>(nextSubset);

                    State target = dfaStateMap.get(subsetKey);

                    if (target == null) {
                        target = new State(dfaStateCounter++);

                        boolean isFinal = false;
                        String token = null;
                        int minId = Integer.MAX_VALUE; // Track lowest ID for priority

                        for (Integer id : subsetKey) {
                            if (nfaFinalStates.contains(id)) {
                                isFinal = true;
                                // Priority Tie-breaker: Lower state ID means the rule was defined earlier
                                if (id < minId) {
                                    minId = id;
                                    token = stateAcceptedTokens.get(id);
                                }
                            }
                        }

                        target.setFinal(isFinal);
                        target.setAcceptedToken(token);

                        if (isFinal) {
                            dfaFinalStates.add(target);
                        }

                        dfaStateMap.put(subsetKey, target);
                        queue.add(subsetKey);
                    }

                    currentDfaState.addTransition(symbol, target);
                }
            }
        }

        return new AFD(afnd.getTokenName() + "_to_AFD", dfaStart, dfaFinalStates);
    }

    // ========================================================================
    // FAST BFS
    // ========================================================================
    private Set<State> getAllStates(State start) {
        Set<State> visited = new HashSet<>();
        ArrayDeque<State> queue = new ArrayDeque<>();

        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            State current = queue.poll();

            for (Transition t : current.getTransitions()) {
                State target = t.getTarget();
                if (visited.add(target)) {
                    queue.add(target);
                }
            }
        }

        return visited;
    }
}