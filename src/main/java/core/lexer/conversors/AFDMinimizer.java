package core.lexer.conversors;

import java.util.*;
import models.atomic.State;
import models.atomic.Transition;
import models.automata.AFD;

public class AFDMinimizer {

    private int minStateCounter = 0;

    public AFD minimize(AFD afd) {
        minStateCounter = 0;
        Set<State> allStatesSet = getAllStates(afd.getStartState());
        int numStates = allStatesSet.size();
        State[] states = allStatesSet.toArray(new State[0]);
        
        // 1. Mapeamento de IDs para índices 0...N
        Map<State, Integer> stateToIndex = new HashMap<>(numStates);
        for (int i = 0; i < numStates; i++) stateToIndex.put(states[i], i);

        // 2. Alfabeto indexado
        Map<String, Integer> symbolToIndex = new HashMap<>();
        List<String> alphabet = new ArrayList<>();
        for (State s : states) {
            for (Transition t : s.getTransitions()) {
                if (!symbolToIndex.containsKey(t.getSymbol())) {
                    symbolToIndex.put(t.getSymbol(), alphabet.size());
                    alphabet.add(t.getSymbol());
                }
            }
        }
        int alphabetSize = alphabet.size();

        // 3. Tabela de transição usando IDs primitivos (O(1) access)
        // -1 indica transição para estado de erro
        int[][] transitions = new int[numStates][alphabetSize];
        for (int i = 0; i < numStates; i++) {
            Arrays.fill(transitions[i], -1);
            for (Transition t : states[i].getTransitions()) {
                transitions[i][symbolToIndex.get(t.getSymbol())] = stateToIndex.get(t.getTarget());
            }
        }

        // 4. Particionamento Inicial
        int[] partition = new int[numStates];
        Map<String, Integer> finalGroups = new HashMap<>();
        int pidCounter = 0;

        // Grupo 0 reservado para não-finais
        boolean hasNonFinal = false;
        for (int i = 0; i < numStates; i++) {
            if (isStateFinal(states[i], afd)) {
                String token = states[i].getAcceptedToken() != null ? states[i].getAcceptedToken() : "FINAL_DEFAULT";
                if (!finalGroups.containsKey(token)) {
                    finalGroups.put(token, ++pidCounter);
                }
                partition[i] = finalGroups.get(token);
            } else {
                partition[i] = 0;
                hasNonFinal = true;
            }
        }
        int numPartitions = pidCounter + 1;

        // 5. Refinamento Iterativo (Algoritmo de Moore O(n^2) ou O(kn log n))
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

        // 6. Reconstrução do AFD
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
                    newSource.addTransition(alphabet.get(sIdx), representative[partition[targetOldIdx]]);
                }
            }
        }

        return new AFD(afd.getTokenName() + "_MIN", minimizedStart, minimizedFinals);
    }

    // Classe leve para identificar grupos de equivalência
    private static class Signature {
        final int currentPartition;
        final int[] targets;
        final int hashCode;

        Signature(int currentPartition, int[] stateTransitions, int[] partitionMap) {
            this.currentPartition = currentPartition;
            this.targets = new int[stateTransitions.length];
            int h = currentPartition;
            for (int i = 0; i < stateTransitions.length; i++) {
                int t = stateTransitions[i];
                int targetPid = (t == -1) ? -1 : partitionMap[t];
                this.targets[i] = targetPid;
                h = h * 31 + targetPid;
            }
            this.hashCode = h;
        }

        @Override
        public boolean equals(Object o) {
            Signature other = (Signature) o;
            if (currentPartition != other.currentPartition) return false;
            return Arrays.equals(targets, other.targets);
        }

        @Override
        public int hashCode() { return hashCode; }
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