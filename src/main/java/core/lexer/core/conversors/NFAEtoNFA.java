package core.lexer.core.conversors;

import Utils.Utils;
import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.NFAE;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import models.atomic.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NFAEtoNFA {
    private static final Logger log = LoggerFactory.getLogger(NFAEtoNFA.class);

    public String convertAndSaveToDisk(NFAE nfae, String filepath) {
        State[] oldStates = nfae.getStates().toArray(State[]::new);
        int n = oldStates.length;
        log.info("old states: " + n);
        Map<State, Integer> stateToIdx = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            stateToIdx.put(oldStates[i], i);
        }

        Symbol[] alphabet =
                nfae.getAlphabet().getSymbols().stream()
                        .filter(sym -> !sym.getValue().equals(Constants.EPSILON))
                        .toArray(Symbol[]::new);
        int a = alphabet.length;
        Map<Symbol, Integer> symToIdx = new HashMap<>(a);
        for (int i = 0; i < a; i++) {
            symToIdx.put(alphabet[i], i);
        }

        int[][] epsilons = new int[n][];
        int[] epsCounts = new int[n];
        int[][][] symTrans = new int[n][a][];
        int[][] symCounts = new int[n][a];

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

        for (int i = 0; i < n; i++) {
            epsilons[i] = new int[epsCounts[i]];
            for (int s = 0; s < a; s++) {
                if (symCounts[i][s] > 0) {
                    symTrans[i][s] = new int[symCounts[i][s]];
                }
            }
        }

        Arrays.fill(epsCounts, 0);
        for (int i = 0; i < n; i++) Arrays.fill(symCounts[i], 0);

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

        BitSet[] closures = new BitSet[n];
        int[] stack = new int[n];
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

        log.info("total new states writing on disk: {} => {} | alphabet size: {}", n, n * a, a);

        try {
            Utils.createDirectories(filepath);

            try (BufferedWriter writer =
                    new BufferedWriter(new FileWriter(filepath, StandardCharsets.UTF_8))) {
                writer.write("TOKEN:" + nfae.getTokenName() + "\n");

                writer.write("ALPHABET:");
                for (int i = 0; i < a; i++) {
                    String safeSym =
                            Base64.getEncoder()
                                    .encodeToString(
                                            alphabet[i]
                                                    .getValue()
                                                    .getBytes(StandardCharsets.UTF_8));
                    writer.write(safeSym + (i == a - 1 ? "" : ","));
                }
                writer.write("\n");

                writer.write("// FORMAT: STATE:ID,isInitial,isFinal,Base64_Token\n");
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

                    String safeToken =
                            (token == null)
                                    ? ""
                                    : Base64.getEncoder()
                                            .encodeToString(token.getBytes(StandardCharsets.UTF_8));

                    writer.write(
                            "STATE:"
                                    + ns.getId()
                                    + ","
                                    + ns.isInitial()
                                    + ","
                                    + ns.isFinal()
                                    + ","
                                    + safeToken
                                    + "\n");
                }

                BitSet nfaTargets = new BitSet(n);
                for (int i = 0; i < n; i++) {
                    State src = newStates[i];
                    BitSet closureI = closures[i];
                    for (int sIdx = 0; sIdx < a; sIdx++) {
                        nfaTargets.clear();
                        for (int j = closureI.nextSetBit(0);
                                j >= 0;
                                j = closureI.nextSetBit(j + 1)) {
                            int[] jTargets = symTrans[j][sIdx];
                            if (jTargets != null) {
                                for (int k = 0; k < jTargets.length; k++) {
                                    nfaTargets.or(closures[jTargets[k]]);
                                }
                            }
                        }
                        for (int targetIdx = nfaTargets.nextSetBit(0);
                                targetIdx >= 0;
                                targetIdx = nfaTargets.nextSetBit(targetIdx + 1)) {
                            writer.write(
                                    "TRANS:"
                                            + src.getId()
                                            + ","
                                            + newStates[targetIdx].getId()
                                            + ","
                                            + sIdx
                                            + "\n");
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to save NFA to disk: ", e);
        }

        return filepath;
    }
}
