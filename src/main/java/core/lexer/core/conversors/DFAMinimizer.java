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

public class DFAMinimizer {

    private int minStateCounter = 0;

    public DFA minimize(DFA dfa) {
        minStateCounter = 0;

        List<State> states = new ArrayList<>(dfa.getStates());
        int n = states.size();
        Map<State, Integer> stateToIdx = new HashMap<>(n);
        for (int i = 0; i < n; i++) stateToIdx.put(states.get(i), i);

        List<Symbol> alphabet = new ArrayList<>(dfa.getAlphabet().getSymbols());
        int k = alphabet.size();

        int[][] trans = new int[n][k];
        for (int i = 0; i < n; i++) Arrays.fill(trans[i], -1);
        for (Transition t : dfa.getTransitions()) {
            int src = stateToIdx.get(t.getSource());
            int dst = stateToIdx.get(t.getTarget());
            int sym = alphabet.indexOf(t.getSymbol());
            trans[src][sym] = dst;
        }

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

        int[] block = new int[n];
        Map<String, Integer> tokenBlock = new HashMap<>();
        int blockCount = 1;
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

        List<BitSet> blocks = new ArrayList<>(blockCount);
        for (int i = 0; i < blockCount; i++) blocks.add(new BitSet(n));
        for (int i = 0; i < n; i++) blocks.get(block[i]).set(i);

        Queue<Integer> worklist = new ArrayDeque<>();
        for (int i = 0; i < blockCount; i++) worklist.add(i);

        while (!worklist.isEmpty()) {
            int a = worklist.poll();
            BitSet blockA = blocks.get(a);
            for (int sym = 0; sym < k; sym++) {
                boolean[] inX = new boolean[n];
                for (int s = blockA.nextSetBit(0); s >= 0; s = blockA.nextSetBit(s + 1)) {
                    for (int src : rev[sym][s]) {
                        inX[src] = true;
                    }
                }
                int currentBlockCount = blocks.size();
                for (int b = 0; b < currentBlockCount; b++) {
                    BitSet blockB = blocks.get(b);
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
                        BitSet B_in = new BitSet(n);
                        BitSet B_out = new BitSet(n);
                        for (int s = blockB.nextSetBit(0); s >= 0; s = blockB.nextSetBit(s + 1)) {
                            if (inX[s]) {
                                B_in.set(s);
                                block[s] = blocks.size();
                            } else {
                                B_out.set(s);
                                block[s] = b;
                            }
                        }
                        blocks.set(b, B_out);
                        blocks.add(B_in);
                        if (B_out.cardinality() <= B_in.cardinality()) {
                            worklist.add(b);
                        } else {
                            worklist.add(blocks.size() - 1);
                        }
                    }
                }
            }
        }

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

    public DFA trimUnreachableStates(DFA dfa) {
        Set<State> reachableStates = new HashSet<>();
        Queue<State> queue = new ArrayDeque<>(dfa.getInitialStates());
        reachableStates.addAll(dfa.getInitialStates());

        Map<State, List<Transition>> transitionsBySource =
                dfa.getTransitions().stream().collect(Collectors.groupingBy(Transition::getSource));

        while (!queue.isEmpty()) {
            State current = queue.poll();

            List<Transition> outgoingTransitions =
                    transitionsBySource.getOrDefault(current, List.of());
            for (Transition t : outgoingTransitions) {
                State target = t.getTarget();
                if (reachableStates.add(target)) {
                    queue.add(target);
                }
            }
        }

        DFA trimmedDFA = new DFA(dfa.getTokenName() + "_TRIMMED");

        for (State state : reachableStates) {
            trimmedDFA.addState(state);
        }

        for (Transition t : dfa.getTransitions()) {
            if (reachableStates.contains(t.getSource())) {
                trimmedDFA.addTransition(t);
            }
        }

        return trimmedDFA;
    }
}
