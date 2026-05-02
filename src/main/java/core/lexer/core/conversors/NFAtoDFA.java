package core.lexer.core.conversors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.DFA;

public class NFAtoDFA {
    private static final Logger log = LoggerFactory.getLogger(NFAtoDFA.class);

    private int dfaStateCounter = 0;

    public DFA convertFromDisk(String filepath) {
        long startTime = System.currentTimeMillis();
        dfaStateCounter = 0;
        String tokenName = "MASTER";
        List<Symbol> alphabet = new ArrayList<>();
        List<State> nfaStatesList = new ArrayList<>();
        Map<Integer, Map<Integer, BitSet>> diskTransNFA = new HashMap<>();

        log.info("Starting NFA disk read and DFA conversion from: {}", filepath);

        try (BufferedReader reader =
                new BufferedReader(new FileReader(filepath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty() || trimmedLine.startsWith("//")) {
                    continue;
                }

                if (trimmedLine.startsWith("TOKEN:")) {
                    tokenName = trimmedLine.substring(6).trim();
                    log.debug("Parsed TOKEN: {}", tokenName);
                } else if (trimmedLine.startsWith("ALPHABET:")) {
                    String[] symbols = trimmedLine.substring(9).split(",");
                    for (String s : symbols) {
                        String cleanS = s.trim();
                        if (!cleanS.isEmpty()) {
                            String decodedSym =
                                    new String(
                                            Base64.getDecoder().decode(cleanS),
                                            StandardCharsets.UTF_8);
                            alphabet.add(new Symbol(decodedSym));
                        }
                    }
                    log.debug("Parsed ALPHABET with {} symbols", alphabet.size());
                } else if (trimmedLine.startsWith("STATE:")) {
                    String[] parts = trimmedLine.substring(6).split(",", -1);
                    int id = Integer.parseInt(parts[0].trim());
                    boolean isInitial = Boolean.parseBoolean(parts[1].trim());
                    boolean isFinal = Boolean.parseBoolean(parts[2].trim());

                    String token = null;
                    if (parts.length > 3 && !parts[3].trim().isEmpty()) {
                        token =
                                new String(
                                        Base64.getDecoder().decode(parts[3].trim()),
                                        StandardCharsets.UTF_8);
                    }

                    State state = new State(id, isInitial, isFinal);
                    state.setAcceptedToken(token);
                    nfaStatesList.add(state);
                } else if (trimmedLine.startsWith("TRANS:")) {
                    String[] parts = trimmedLine.substring(6).split(",");
                    int srcId = Integer.parseInt(parts[0].trim());
                    int tgtId = Integer.parseInt(parts[1].trim());
                    int symIdx = Integer.parseInt(parts[2].trim());

                    diskTransNFA
                            .computeIfAbsent(srcId, k -> new HashMap<>())
                            .computeIfAbsent(symIdx, k -> new BitSet())
                            .set(tgtId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse NFA disk file '{}': ", filepath, e);
        }

        int n = nfaStatesList.size();
        int k = alphabet.size();

        log.info("Finished disk read. NFA loaded with {} states and {} alphabet symbols.", n, k);
        log.debug("Starting subset construction for DFA...");

        DFA dfa = new DFA(tokenName + "_DFA");

        Map<Integer, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idToIndex.put(nfaStatesList.get(i).getId(), i);
        }

        Map<BitSet, State> dfaStateMap = new HashMap<>();
        Queue<BitSet> queue = new ArrayDeque<>();

        BitSet initialSet = new BitSet(n);
        for (State s : nfaStatesList) {
            if (s.isInitial()) {
                initialSet.set(idToIndex.get(s.getId()));
            }
        }

        State dfaStart = createDfaState(initialSet, nfaStatesList);
        dfa.addState(dfaStart);
        dfaStateMap.put(initialSet, dfaStart);
        queue.add(initialSet);

        while (!queue.isEmpty()) {
            BitSet currentSet = queue.poll();
            State currentDfaState = dfaStateMap.get(currentSet);

            for (int symIdx = 0; symIdx < k; symIdx++) {
                BitSet targetSet = new BitSet(n);

                for (int sIdx = currentSet.nextSetBit(0);
                        sIdx >= 0;
                        sIdx = currentSet.nextSetBit(sIdx + 1)) {
                    int srcId = nfaStatesList.get(sIdx).getId();
                    Map<Integer, BitSet> srcTrans = diskTransNFA.get(srcId);
                    if (srcTrans != null) {
                        BitSet tgts = srcTrans.get(symIdx);
                        if (tgts != null) {
                            for (int tId = tgts.nextSetBit(0);
                                    tId >= 0;
                                    tId = tgts.nextSetBit(tId + 1)) {
                                targetSet.set(idToIndex.get(tId));
                            }
                        }
                    }
                }

                if (targetSet.isEmpty()) continue;

                State dfaTarget = dfaStateMap.get(targetSet);
                if (dfaTarget == null) {
                    dfaTarget = createDfaState(targetSet, nfaStatesList);
                    dfa.addState(dfaTarget);
                    dfaStateMap.put(targetSet, dfaTarget);
                    queue.add(targetSet);
                    log.debug("Created new DFA State ID {} mapped from NFA subset: {}", dfaTarget.getId(), targetSet);
                }
                dfa.addTransition(new Transition(currentDfaState, dfaTarget, alphabet.get(symIdx)));
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Conversion completed in {} ms. Generated DFA '{}' with {} states.", 
                 duration, dfa.getTokenName(), dfaStateCounter);

        return dfa;
    }

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