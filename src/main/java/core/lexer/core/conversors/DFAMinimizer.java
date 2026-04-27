package core.lexer.core.conversors;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.DFA;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Minimizes a DFA using Hopcroft's partition refinement algorithm. Time complexity O(n log n) where
 * n is the number of states. The algorithm iteratively refines a partition of states until no
 * further splitting is possible, producing an equivalent DFA with the minimum number of states.
 *
 * <p>The implementation uses efficient data structures: BitSets for block representation and
 * reverse transition lists to speed up the preimage calculation.
 *
 * @author Generated
 * @version 2.0
 */
public class DFAMinimizer {

    private int minStateCounter = 0;

    /**
     * Minimises the given DFA and returns an equivalent DFA with the fewest states. The returned
     * DFA is also trimmed of any unreachable states.
     *
     * @param dfa the deterministic finite automaton to minimise
     * @return a new minimal DFA (with newly created states)
     */
    public DFA minimize(DFA dfa) {
        minStateCounter = 0;

        List<State> states = new ArrayList<>(dfa.getStates());
        int n = states.size();
        Map<State, Integer> stateToIdx = new HashMap<>(n);
        for (int i = 0; i < n; i++) stateToIdx.put(states.get(i), i);

        List<Symbol> alphabet = new ArrayList<>(dfa.getAlphabet().getSymbols());
        int k = alphabet.size();

        // Forward transitions: trans[state][symbol] = target or -1
        int[][] trans = new int[n][k];
        for (int i = 0; i < n; i++) Arrays.fill(trans[i], -1);
        for (Transition t : dfa.getTransitions()) {
            int src = stateToIdx.get(t.getSource());
            int dst = stateToIdx.get(t.getTarget());
            int sym = alphabet.indexOf(t.getSymbol());
            trans[src][sym] = dst;
        }

        // Reverse transitions: rev[symbol][target] = list of sources
        @SuppressWarnings("unchecked")
        List<Integer>[][] rev = new List[k][n];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < n; j++) rev[i][j] = new ArrayList<>();
        }
        for (int s = 0; s < n; s++) {
            for (int sym = 0; sym < k; sym++) {
                int t = trans[s][sym];
                if (t != -1) rev[sym][t].add(s);
            }
        }

        // Initial partition: separate final states by their token, non‑final states together
        int[] block = new int[n];
        Map<String, Integer> tokenBlock = new HashMap<>();
        int blockCount = 1; // block 0 = non‑final
        for (int i = 0; i < n; i++) {
            State s = states.get(i);
            if (s.isFinal()) {
                String token = s.getAcceptedToken() != null ? s.getAcceptedToken() : "";
                if (!tokenBlock.containsKey(token)) {
                    tokenBlock.put(token, blockCount++);
                }
                block[i] = tokenBlock.get(token);
            } else {
                block[i] = 0;
            }
        }

        // Build the block sets
        List<BitSet> blocks = new ArrayList<>(blockCount);
        for (int i = 0; i < blockCount; i++) blocks.add(new BitSet(n));
        for (int i = 0; i < n; i++) blocks.get(block[i]).set(i);

        // Worklist for Hopcroft – initially all blocks (except possibly non‑final if final blocks
        // exist)
        Queue<Integer> worklist = new ArrayDeque<>();
        for (int i = 0; i < blockCount; i++) worklist.add(i);

        // Main refinement loop
        while (!worklist.isEmpty()) {
            int a = worklist.poll();
            BitSet blockA = blocks.get(a);
            for (int sym = 0; sym < k; sym++) {
                // Compute X = preimage of blockA under symbol sym
                boolean[] inX = new boolean[n];
                for (int s = blockA.nextSetBit(0); s >= 0; s = blockA.nextSetBit(s + 1)) {
                    for (int src : rev[sym][s]) {
                        inX[src] = true;
                    }
                }
                // Split blocks using X
                int currentBlockCount = blocks.size();
                for (int b = 0; b < currentBlockCount; b++) {
                    BitSet blockB = blocks.get(b);
                    // Quick test: does blockB contain both states in X and not in X?
                    int first = blockB.nextSetBit(0);
                    if (first == -1) continue;
                    boolean hasIn = inX[first];
                    boolean hasOut = !hasIn;
                    if (hasOut) {
                        for (int s = blockB.nextSetBit(first + 1);
                                s >= 0 && (!hasIn || !hasOut);
                                s = blockB.nextSetBit(s + 1)) {
                            if (inX[s]) hasIn = true;
                            else hasOut = true;
                        }
                    }
                    if (hasIn && hasOut) {
                        // Split blockB into B_in (states in X) and B_out (rest)
                        BitSet B_in = new BitSet(n);
                        BitSet B_out = new BitSet(n);
                        for (int s = blockB.nextSetBit(0); s >= 0; s = blockB.nextSetBit(s + 1)) {
                            if (inX[s]) {
                                B_in.set(s);
                                block[s] = blocks.size(); // new block id will be current size
                            } else {
                                B_out.set(s);
                                block[s] = b;
                            }
                        }
                        blocks.set(b, B_out);
                        blocks.add(B_in);
                        // Add the smaller part to the worklist
                        if (B_out.cardinality() <= B_in.cardinality()) {
                            worklist.add(b);
                        } else {
                            worklist.add(blocks.size() - 1);
                        }
                    }
                }
            }
        }

        // Build the minimal DFA from the final blocks
        int numBlocks = blocks.size();
        State[] rep = new State[numBlocks];
        int[] repIdx = new int[numBlocks];
        Arrays.fill(repIdx, -1);

        DFA minDfa = new DFA(dfa.getTokenName() + "_MIN");

        for (int i = 0; i < n; i++) {
            int b = block[i];
            if (repIdx[b] == -1) {
                repIdx[b] = i;
                State orig = states.get(i);
                rep[b] = new State(minStateCounter++);
                rep[b].setFinalState(orig.isFinal());
                rep[b].setInitial(orig.isInitial());
                rep[b].setAcceptedToken(orig.getAcceptedToken());
                minDfa.addState(rep[b]);
            }
        }

        for (int b = 0; b < numBlocks; b++) {
            State srcState = rep[b];
            int origIdx = repIdx[b];
            for (int sym = 0; sym < k; sym++) {
                int t = trans[origIdx][sym];
                if (t != -1) {
                    minDfa.addTransition(
                            new Transition(srcState, rep[block[t]], alphabet.get(sym)));
                }
            }
        }

        DFA trimedDfa = trimUnreachableStates(minDfa);
        return trimedDfa;
    }

    /**
     * Trims unreachable states from a DFA using Breadth‑First Search (BFS). Only states reachable
     * from any initial state are kept.
     *
     * @param dfa the original DFA (may contain unreachable states)
     * @return a new DFA containing only states reachable from the initial states
     */
    public DFA trimUnreachableStates(DFA dfa) {
        Set<State> reachableStates = new HashSet<>();
        Queue<State> queue = new ArrayDeque<>(dfa.getInitialStates());
        reachableStates.addAll(dfa.getInitialStates());

        // 1. Pre-group transitions by source state for O(1) adjacency lookups
        Map<State, List<Transition>> transitionsBySource =
                dfa.getTransitions().stream().collect(Collectors.groupingBy(Transition::getSource));

        // 2. Execute Breadth-First Search
        while (!queue.isEmpty()) {
            State current = queue.poll();

            List<Transition> outgoingTransitions =
                    transitionsBySource.getOrDefault(current, List.of());
            for (Transition t : outgoingTransitions) {
                State target = t.getTarget();
                // add() returns true if the state was not already in the set
                if (reachableStates.add(target)) {
                    queue.add(target);
                }
            }
        }

        // 3. Build the new "Cut" DFA
        DFA trimmedDFA = new DFA(dfa.getTokenName() + "_TRIMMED");

        // Add all discovered states
        for (State state : reachableStates) {
            trimmedDFA.addState(state);
        }

        // Add transitions belonging exclusively to reachable states
        for (Transition t : dfa.getTransitions()) {
            if (reachableStates.contains(t.getSource())) {
                trimmedDFA.addTransition(t);
            }
        }

        return trimmedDFA;
    }
}
