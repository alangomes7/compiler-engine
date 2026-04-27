package core.lexer.core.conversors;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.DFA;
import core.lexer.models.automata.NFA;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Converts an NFA to a DFA using the subset construction. Subsets are represented as {@link BitSet}
 * for fast hashing and equality.
 *
 * @author Generated
 * @version 2.0
 */
public class NFAtoDFA {

    private int dfaStateCounter = 0;

    /**
     * Converts the given NFA into an equivalent DFA.
     *
     * @param nfa the nondeterministic automaton
     * @return a new DFA
     */
    public DFA convert(NFA nfa) {
        dfaStateCounter = 0;
        DFA dfa = new DFA(nfa.getTokenName() + "_DFA");

        // Assign integer IDs to NFA states for BitSet indexing
        List<State> nfaStates = new ArrayList<>(nfa.getStates());
        Map<State, Integer> nfaStateIdx = new HashMap<>();
        for (int i = 0; i < nfaStates.size(); i++) {
            nfaStateIdx.put(nfaStates.get(i), i);
        }
        int n = nfaStates.size();

        // Pre‑compute transitions: for each NFA state, per symbol, target states (as BitSet)
        List<Symbol> alphabet = new ArrayList<>(nfa.getAlphabet().getSymbols());
        int k = alphabet.size();
        Map<Symbol, Integer> symToIdx = new HashMap<>();
        for (int i = 0; i < k; i++) symToIdx.put(alphabet.get(i), i);

        // transNFA[state][symbol] = BitSet of target states
        BitSet[][] transNFA = new BitSet[n][k];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < k; j++) transNFA[i][j] = new BitSet(n);
        }
        for (Transition t : nfa.getTransitions()) {
            int src = nfaStateIdx.get(t.getSource());
            int sym = symToIdx.get(t.getSymbol());
            int dst = nfaStateIdx.get(t.getTarget());
            transNFA[src][sym].set(dst);
        }

        // DFA construction: map BitSet (subset) -> DFA state
        Map<BitSet, State> dfaStateMap = new HashMap<>();
        Queue<BitSet> queue = new ArrayDeque<>();

        BitSet initialSet = new BitSet(n);
        for (State s : nfa.getInitialStates()) {
            initialSet.set(nfaStateIdx.get(s));
        }
        State dfaStart = createDfaState(initialSet, nfaStates);
        dfa.addState(dfaStart);
        dfaStateMap.put(initialSet, dfaStart);
        queue.add(initialSet);

        while (!queue.isEmpty()) {
            BitSet currentSet = queue.poll();
            State currentDfaState = dfaStateMap.get(currentSet);

            // For each symbol, compute the union of target sets
            for (int sym = 0; sym < k; sym++) {
                BitSet targetSet = new BitSet(n);
                for (int s = currentSet.nextSetBit(0); s >= 0; s = currentSet.nextSetBit(s + 1)) {
                    targetSet.or(transNFA[s][sym]);
                }
                if (targetSet.isEmpty()) continue;

                State dfaTarget = dfaStateMap.get(targetSet);
                if (dfaTarget == null) {
                    dfaTarget = createDfaState(targetSet, nfaStates);
                    dfa.addState(dfaTarget);
                    dfaStateMap.put(targetSet, dfaTarget);
                    queue.add(targetSet);
                }
                dfa.addTransition(new Transition(currentDfaState, dfaTarget, alphabet.get(sym)));
            }
        }

        return dfa;
    }

    /**
     * Creates a DFA state from a BitSet of NFA state indices.
     *
     * @param subset the subset of NFA states
     * @param nfaStates list of all NFA states (for lookups)
     * @return a new DFA state
     */
    private State createDfaState(BitSet subset, List<State> nfaStates) {
        State newState = new State(dfaStateCounter++);
        boolean isFinal = false;
        String bestToken = null;
        int minId = Integer.MAX_VALUE;

        for (int idx = subset.nextSetBit(0); idx >= 0; idx = subset.nextSetBit(idx + 1)) {
            State s = nfaStates.get(idx);
            if (s.isFinal()) {
                isFinal = true;
                if (s.getId() < minId && s.getAcceptedToken() != null) {
                    minId = s.getId();
                    bestToken = s.getAcceptedToken();
                }
            }
            if (s.isInitial()) {
                newState.setInitial(true);
            }
        }
        newState.setFinalState(isFinal);
        newState.setAcceptedToken(bestToken);
        return newState;
    }
}
