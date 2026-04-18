package core.lexer.core.conversors;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.AFD;
import core.lexer.models.automata.AFND;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AFNDtoAFD {

    private int dfaStateCounter = 0;

    private static class NfaStateData {
        final int id;
        final boolean isFinal;
        final String token;
        final Map<Symbol, int[]> transitions;

        NfaStateData(int id, boolean isFinal, String token, Map<Symbol, List<Integer>> tempTrans) {
            this.id = id;
            this.isFinal = isFinal;
            this.token = token;
            this.transitions = new HashMap<>();
            tempTrans.forEach(
                    (sym, list) -> transitions.put(sym, list.stream().mapToInt(i -> i).toArray()));
        }
    }

    public AFD convert(AFND afnd) {
        dfaStateCounter = 0;

        Set<State> allStates = getAllStates(afnd.getStartState());
        int maxId = allStates.stream().mapToInt(State::getId).max().orElse(0);
        NfaStateData[] nfaData = new NfaStateData[maxId + 1];

        for (State s : allStates) {
            Map<Symbol, List<Integer>> tempTrans = new HashMap<>();
            for (Transition t : s.getTransitions()) {
                tempTrans
                        .computeIfAbsent(t.getSymbol(), k -> new ArrayList<>())
                        .add(t.getTarget().getId());
            }
            nfaData[s.getId()] =
                    new NfaStateData(
                            s.getId(),
                            s.isFinal()
                                    || (afnd.getFinalStates() != null
                                            && afnd.getFinalStates().contains(s)),
                            s.getAcceptedToken(),
                            tempTrans);
        }

        Map<BitSet, State> dfaStateMap = new HashMap<>();
        ArrayDeque<BitSet> queue = new ArrayDeque<>();
        Set<State> dfaFinalStates = new HashSet<>();

        BitSet startSubset = new BitSet();
        startSubset.set(afnd.getStartState().getId());

        State dfaStart = createDfaState(startSubset, nfaData);
        dfaStateMap.put(startSubset, dfaStart);
        queue.add(startSubset);
        if (dfaStart.isFinal()) dfaFinalStates.add(dfaStart);

        while (!queue.isEmpty()) {
            BitSet currentSubset = queue.poll();
            State currentDfaState = dfaStateMap.get(currentSubset);

            Map<Symbol, BitSet> nextSubsets = new HashMap<>();

            for (int i = currentSubset.nextSetBit(0); i >= 0; i = currentSubset.nextSetBit(i + 1)) {
                NfaStateData data = nfaData[i];
                if (data == null) continue;

                for (Map.Entry<Symbol, int[]> entry : data.transitions.entrySet()) {
                    BitSet targetBitSet =
                            nextSubsets.computeIfAbsent(entry.getKey(), k -> new BitSet());
                    for (int targetId : entry.getValue()) {
                        targetBitSet.set(targetId);
                    }
                }
            }

            for (Map.Entry<Symbol, BitSet> entry : nextSubsets.entrySet()) {
                Symbol symbol = entry.getKey();
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
