package core.lexer.core.conversors;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.AFD;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AFDMinimizer {

    private int minStateCounter = 0;

    public AFD minimize(AFD afd) {
        minStateCounter = 0;
        Set<State> allStatesSet = getAllStates(afd.getStartState());
        int numStates = allStatesSet.size();
        State[] states = allStatesSet.toArray(State[]::new);

        Map<State, Integer> stateToIndex = new HashMap<>(numStates);
        for (int i = 0; i < numStates; i++) stateToIndex.put(states[i], i);

        Map<Symbol, Integer> symbolToIndex = new HashMap<>();
        List<Symbol> alphabet = new ArrayList<>();

        for (State s : states) {
            for (Transition t : s.getTransitions()) {
                if (!symbolToIndex.containsKey(t.getSymbol())) {
                    symbolToIndex.put(t.getSymbol(), alphabet.size());
                    alphabet.add(t.getSymbol());
                }
            }
        }
        int alphabetSize = alphabet.size();

        int[][] transitions = new int[numStates][alphabetSize];
        for (int i = 0; i < numStates; i++) {
            Arrays.fill(transitions[i], -1);
            for (Transition t : states[i].getTransitions()) {
                transitions[i][symbolToIndex.get(t.getSymbol())] = stateToIndex.get(t.getTarget());
            }
        }

        int[] partition = new int[numStates];
        Map<String, Integer> finalGroups = new HashMap<>();
        int pidCounter = 0;

        for (int i = 0; i < numStates; i++) {
            if (isStateFinal(states[i], afd)) {
                String token =
                        states[i].getAcceptedToken() != null
                                ? states[i].getAcceptedToken()
                                : "FINAL_DEFAULT";
                if (!finalGroups.containsKey(token)) {
                    finalGroups.put(token, ++pidCounter);
                }
                partition[i] = finalGroups.get(token);
            } else {
                partition[i] = 0;
            }
        }
        int numPartitions = pidCounter + 1;

        boolean changed = true;
        while (changed) {
            changed = false;
            int[] newPartition = new int[numStates];
            Map<Signature, Integer> signatureToId = new HashMap<>(numPartitions);
            int nextPid = 0;

            for (int i = 0; i < numStates; i++) {
                Signature sig = new Signature(partition[i], transitions[i], partition);
                Integer existingPid = signatureToId.get(sig);

                if (existingPid == null) {
                    signatureToId.put(sig, nextPid);
                    newPartition[i] = nextPid++;
                } else {
                    newPartition[i] = existingPid;
                }
            }

            if (nextPid != numPartitions) {
                partition = newPartition;
                numPartitions = nextPid;
                changed = true;
            }
        }

        State[] representative = new State[numPartitions];
        int[] repIndices = new int[numPartitions];
        Arrays.fill(repIndices, -1);

        for (int i = 0; i < numStates; i++) {
            int p = partition[i];
            if (repIndices[p] == -1) {
                repIndices[p] = i;
                representative[p] = new State(minStateCounter++);
                representative[p].setFinal(isStateFinal(states[i], afd));
                representative[p].setAcceptedToken(states[i].getAcceptedToken());
            }
        }

        Set<State> minimizedFinals = new HashSet<>();
        State minimizedStart = representative[partition[stateToIndex.get(afd.getStartState())]];

        for (int p = 0; p < numPartitions; p++) {
            State newSource = representative[p];
            if (newSource.isFinal()) minimizedFinals.add(newSource);

            int oldIdx = repIndices[p];
            for (int sIdx = 0; sIdx < alphabetSize; sIdx++) {
                int targetOldIdx = transitions[oldIdx][sIdx];
                if (targetOldIdx != -1) {
                    newSource.addTransition(
                            alphabet.get(sIdx), representative[partition[targetOldIdx]]);
                }
            }
        }

        return new AFD(afd.getTokenName() + "_MIN", minimizedStart, minimizedFinals);
    }

    private Set<State> getAllStates(State start) {
        Set<State> visited = new LinkedHashSet<>();
        ArrayDeque<State> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            State s = queue.poll();
            for (Transition t : s.getTransitions()) {
                if (visited.add(t.getTarget())) queue.add(t.getTarget());
            }
        }
        return visited;
    }

    private boolean isStateFinal(State s, AFD afd) {
        return s.isFinal() || (afd.getFinalStates() != null && afd.getFinalStates().contains(s));
    }
}
