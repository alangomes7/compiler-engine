package core.lexer.core.conversors;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.NFA;
import core.lexer.models.automata.NFAE;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import models.atomic.Constants;

/**
 * Converts an ε‑NFA (NFA with epsilon transitions) to an equivalent NFA without epsilon transitions
 * using a highly optimised, allocation‑free algorithm.
 *
 * <p>The conversion computes epsilon‑closures using primitive arrays and {@link BitSet} for minimal
 * object allocation and maximal performance. Final state information is propagated across
 * epsilon‑reachable states.
 *
 * @author Generated
 * @version 1.0
 */
public class NFAEtoNFA {

    /**
     * Converts an ε‑NFA into an equivalent ε‑free NFA.
     *
     * <p>The algorithm proceeds as follows:
     *
     * <ol>
     *   <li>Map states to indices for O(1) array access.
     *   <li>Extract the non‑epsilon alphabet and map symbols to indices.
     *   <li>Build primitive adjacency arrays for epsilon and non‑epsilon transitions.
     *   <li>Compute epsilon closures for all states using an iterative DFS stack.
     *   <li>Create new NFA states, marking final status based on the closure.
     *   <li>Compute real transitions by applying the closure to each symbol's targets.
     * </ol>
     *
     * @param nfae the ε‑NFA to convert
     * @return a new NFA with no epsilon transitions (still non‑deterministic)
     */
    public NFA convert(NFAE nfae) {
        // 1. Array-ify states for O(1) index access
        State[] oldStates = nfae.getStates().toArray(State[]::new);
        int n = oldStates.length;

        Map<State, Integer> stateToIdx = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            stateToIdx.put(oldStates[i], i);
        }

        // 2. Extract strictly non-epsilon alphabet for direct array indexing
        Symbol[] alphabet =
                nfae.getAlphabet().getSymbols().stream()
                        .filter(sym -> !sym.getValue().equals(Constants.EPSILON))
                        .toArray(Symbol[]::new);
        int a = alphabet.length;

        Map<Symbol, Integer> symToIdx = new HashMap<>(a);
        for (int i = 0; i < a; i++) {
            symToIdx.put(alphabet[i], i);
        }

        // 3. Build perfectly sized primitive adjacency arrays (Zero Unboxing & Zero Hashing)
        int[][] epsilons = new int[n][];
        int[] epsCounts = new int[n];

        int[][][] symTrans = new int[n][a][];
        int[][] symCounts = new int[n][a];

        // Pass A: Count outgoing transitions to allocate exact array sizes
        for (Transition t : nfae.getTransitions()) {
            int src = stateToIdx.get(t.getSource());
            Symbol sym = t.getSymbol();

            if (sym.getValue().equals(Constants.EPSILON)) {
                epsCounts[src]++;
            } else {
                Integer sIdx = symToIdx.get(sym);
                if (sIdx != null) {
                    symCounts[src][sIdx]++;
                }
            }
        }

        // Allocate exact primitive arrays (no ArrayList.grow() overhead)
        for (int i = 0; i < n; i++) {
            epsilons[i] = new int[epsCounts[i]];
            for (int s = 0; s < a; s++) {
                if (symCounts[i][s] > 0) {
                    symTrans[i][s] = new int[symCounts[i][s]];
                }
            }
        }

        // Reset counters to use as insertion pointers
        Arrays.fill(epsCounts, 0);
        for (int i = 0; i < n; i++) Arrays.fill(symCounts[i], 0);

        // Pass B: Populate the primitive arrays
        for (Transition t : nfae.getTransitions()) {
            int src = stateToIdx.get(t.getSource());
            int tgt = stateToIdx.get(t.getTarget());
            Symbol sym = t.getSymbol();

            if (sym.getValue().equals(Constants.EPSILON)) {
                epsilons[src][epsCounts[src]++] = tgt;
            } else {
                Integer sIdx = symToIdx.get(sym);
                if (sIdx != null) {
                    symTrans[src][sIdx][symCounts[src][sIdx]++] = tgt;
                }
            }
        }

        // 4. Compute Epsilon Closures iteratively (Zero-Object Allocation DFS)
        BitSet[] closures = new BitSet[n];
        int[] stack = new int[n]; // Primitive stack

        for (int i = 0; i < n; i++) {
            BitSet closure = new BitSet(n);
            closure.set(i);
            closures[i] = closure;

            int top = 0;
            stack[top++] = i;

            while (top > 0) {
                int curr = stack[--top];
                int[] eTargets = epsilons[curr];

                for (int j = 0; j < eTargets.length; j++) {
                    int next = eTargets[j];
                    if (!closure.get(next)) {
                        closure.set(next);
                        stack[top++] = next;
                    }
                }
            }
        }

        // 5. Build New NFA & Core States
        NFA nfa = new NFA(nfae.getTokenName() + "_NFA");
        State[] newStates = new State[n];

        for (int i = 0; i < n; i++) {
            State old = oldStates[i];
            boolean isFinal = false;
            String token = null;

            BitSet closure = closures[i];
            for (int s = closure.nextSetBit(0); s >= 0; s = closure.nextSetBit(s + 1)) {
                State st = oldStates[s];
                if (st.isFinal()) {
                    isFinal = true;
                    if (token == null && st.getAcceptedToken() != null) {
                        token = st.getAcceptedToken();
                    }
                }
            }

            State ns = new State(old.getId(), old.isInitial(), isFinal);
            ns.setAcceptedToken(token);
            newStates[i] = ns;
            nfa.addState(ns);
        }

        // 6. Compute Real Transitions (Ultra-fast contiguous memory access)
        BitSet nfaTargets = new BitSet(n);

        for (int i = 0; i < n; i++) {
            State src = newStates[i];
            BitSet closureI = closures[i];

            // Loop over array index instead of an Iterator or HashMap
            for (int sIdx = 0; sIdx < a; sIdx++) {
                nfaTargets.clear();

                for (int j = closureI.nextSetBit(0); j >= 0; j = closureI.nextSetBit(j + 1)) {
                    int[] jTargets = symTrans[j][sIdx];

                    if (jTargets != null) {
                        for (int k = 0; k < jTargets.length; k++) {
                            // Apply ε-closure of the target using native BitSet OR
                            nfaTargets.or(closures[jTargets[k]]);
                        }
                    }
                }

                // Append the calculated transitions to the new NFA
                Symbol sym = alphabet[sIdx];
                for (int targetIdx = nfaTargets.nextSetBit(0);
                        targetIdx >= 0;
                        targetIdx = nfaTargets.nextSetBit(targetIdx + 1)) {
                    nfa.addTransition(new Transition(src, newStates[targetIdx], sym));
                }
            }
        }

        return nfa;
    }
}
