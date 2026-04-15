package core.lexer.conversors;

import java.util.*;
import models.atomic.State;
import models.atomic.Transition;
import models.automata.AFD;
import models.automata.AFND;

public class AFNDtoAFD {

    private int dfaStateCounter = 0;

    // Classe auxiliar para armazenar dados pré-processados do NFA
    private static class NfaStateData {
        final int id;
        final boolean isFinal;
        final String token;
        final Map<String, int[]> transitions;

        NfaStateData(int id, boolean isFinal, String token, Map<String, List<Integer>> tempTrans) {
            this.id = id;
            this.isFinal = isFinal;
            this.token = token;
            this.transitions = new HashMap<>();
            tempTrans.forEach((sym, list) -> 
                transitions.put(sym, list.stream().mapToInt(i -> i).toArray())
            );
        }
    }

    public AFD convert(AFND afnd) {
        dfaStateCounter = 0;
        
        // 1. Pré-processamento: Array para acesso O(1) por ID
        Set<State> allStates = getAllStates(afnd.getStartState());
        int maxId = allStates.stream().mapToInt(State::getId).max().orElse(0);
        NfaStateData[] nfaData = new NfaStateData[maxId + 1];
        Set<String> alphabet = new HashSet<>();

        for (State s : allStates) {
            Map<String, List<Integer>> tempTrans = new HashMap<>();
            for (Transition t : s.getTransitions()) {
                alphabet.add(t.getSymbol());
                tempTrans.computeIfAbsent(t.getSymbol(), k -> new ArrayList<>()).add(t.getTarget().getId());
            }
            nfaData[s.getId()] = new NfaStateData(
                s.getId(), 
                s.isFinal() || (afnd.getFinalStates() != null && afnd.getFinalStates().contains(s)),
                s.getAcceptedToken(),
                tempTrans
            );
        }

        // 2. Estruturas do DFA usando BitSet como chave (muito mais rápido que Set<Integer>)
        Map<BitSet, State> dfaStateMap = new HashMap<>();
        ArrayDeque<BitSet> queue = new ArrayDeque<>();
        Set<State> dfaFinalStates = new HashSet<>();

        BitSet startSubset = new BitSet();
        startSubset.set(afnd.getStartState().getId());

        State dfaStart = createDfaState(startSubset, nfaData);
        dfaStateMap.put(startSubset, dfaStart);
        queue.add(startSubset);
        if (dfaStart.isFinal()) dfaFinalStates.add(dfaStart);

        // 3. Construção por subconjuntos
        while (!queue.isEmpty()) {
            BitSet currentSubset = queue.poll();
            State currentDfaState = dfaStateMap.get(currentSubset);

            // Determina quais símbolos são relevantes para este subconjunto específico
            Map<String, BitSet> nextSubsets = new HashMap<>();
            
            for (int i = currentSubset.nextSetBit(0); i >= 0; i = currentSubset.nextSetBit(i + 1)) {
                NfaStateData data = nfaData[i];
                if (data == null) continue;
                
                for (Map.Entry<String, int[]> entry : data.transitions.entrySet()) {
                    BitSet targetBitSet = nextSubsets.computeIfAbsent(entry.getKey(), k -> new BitSet());
                    for (int targetId : entry.getValue()) {
                        targetBitSet.set(targetId);
                    }
                }
            }

            // Cria as transições no AFD
            for (Map.Entry<String, BitSet> entry : nextSubsets.entrySet()) {
                String symbol = entry.getKey();
                BitSet targetBits = entry.getValue();

                State dfaTarget = dfaStateMap.get(targetBits);
                if (dfaTarget == null) {
                    dfaTarget = createDfaState(targetBits, nfaData);
                    dfaStateMap.put(targetBits, dfaTarget);
                    queue.add(targetBits);
                    if (dfaTarget.isFinal()) dfaFinalStates.add(dfaTarget);
                }
                currentDfaState.addTransition(symbol, dfaTarget);
            }
        }

        return new AFD(afnd.getTokenName() + "_to_AFD", dfaStart, dfaFinalStates);
    }

    private State createDfaState(BitSet subset, NfaStateData[] nfaData) {
        State newState = new State(dfaStateCounter++);
        boolean isFinal = false;
        String bestToken = null;
        int minId = Integer.MAX_VALUE;

        for (int i = subset.nextSetBit(0); i >= 0; i = subset.nextSetBit(i + 1)) {
            NfaStateData data = nfaData[i];
            if (data != null && data.isFinal) {
                isFinal = true;
                // Mantém a lógica de prioridade (menor ID original ganha)
                if (data.id < minId) {
                    minId = data.id;
                    bestToken = data.token;
                }
            }
        }

        newState.setFinal(isFinal);
        newState.setAcceptedToken(bestToken);
        return newState;
    }

    private Set<State> getAllStates(State start) {
        Set<State> visited = new LinkedHashSet<>();
        ArrayDeque<State> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            State current = queue.poll();
            for (Transition t : current.getTransitions()) {
                if (visited.add(t.getTarget())) {
                    queue.add(t.getTarget());
                }
            }
        }
        return visited;
    }
}