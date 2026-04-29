package core.lexer;

import core.lexer.core.conversors.DFAMinimizer;
import core.lexer.core.conversors.NFAEtoNFA;
import core.lexer.core.conversors.NFAtoDFA;
import core.lexer.core.conversors.ReToNFAE;
import core.lexer.core.translators.RuleParser;
import core.lexer.models.SymbolTable;
import core.lexer.models.atomic.Rule;
import core.lexer.models.atomic.State;
import core.lexer.models.automata.DFA;
import core.lexer.models.automata.NFA;
import core.lexer.models.automata.NFAE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import models.atomic.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main lexical analyzer that converts an input string into a stream of tokens. Includes dynamic
 * context-sensitive token resolution.
 */
public class Lexer {

    private static final Logger log = LoggerFactory.getLogger(Lexer.class);

    private DFA masterAutomaton;
    private final List<Rule> rules;
    private final SymbolTable symbolTable;
    private final Set<String> dynamicTokens;
    private final Set<String> skipTokens;

    // ---------- Fast scanning structures ----------
    private List<State> dfaStates;
    private Map<State, Integer> dfaStateToIdx;
    private int[][] transitionTable;
    private Map<Character, Integer>[] extendedTransitionTable;
    private int startStateIdx;

    // ---------- Context-Sensitive Rules -----------
    private final Map<String, ContextRule> contextRules = new HashMap<>();

    public Lexer(List<Rule> rules) {
        this.rules = rules;
        log.info("--- Building Scanner For: {} Tokens ---", rules.size());

        this.symbolTable = new SymbolTable();
        this.dynamicTokens = new HashSet<>();
        this.skipTokens = new HashSet<>();

        categorizeRules();
        buildScanner();
    }

    private void categorizeRules() {
        String metacharacterPattern = ".*[\\\\\\[\\]\\(\\)\\*\\+\\?\\|].*";
        for (Rule rule : rules) {
            if (rule.isSkip()) {
                skipTokens.add(rule.getTokenType());
            }
            if (rule.getRegex().matches(metacharacterPattern)) {
                dynamicTokens.add(rule.getTokenType());
            }
        }
        skipTokens.add(Constants.SKIP_TOKEN);
    }

    private void buildScanner() {
        log.info("--- Building Scanner Pipeline ---");
        long totalStart = System.nanoTime();
        long start, end;

        start = System.nanoTime();
        ReToNFAE nfaeGenerator = new ReToNFAE();
        RuleParser parser = new RuleParser(nfaeGenerator);
        List<NFAE> nfaeList = new ArrayList<>(rules.size());

        for (Rule rule : rules) {
            NFAE nfa = parser.parse(rule);
            if (rule.isSkip()) {
                nfaeGenerator.addSkipPattern(rule.getTokenType(), nfa);
            } else {
                nfaeList.add(nfa);
            }
        }
        end = System.nanoTime();
        log.info("0. Rules parsed into NFAEs. Time: {} ms", (end - start) / 1_000_000);

        List<NFAE> allPatterns = new ArrayList<>(nfaeList);
        allPatterns.addAll(nfaeGenerator.getSkipPatterns());

        start = System.nanoTime();
        NFAE masterNFAE = nfaeGenerator.buildMasterScanner(allPatterns);
        end = System.nanoTime();
        log.info("1. Master NFAE built. Time: {} ms", (end - start) / 1_000_000);

        start = System.nanoTime();
        NFAEtoNFA eConverter = new NFAEtoNFA();
        NFA masterNFA = eConverter.convert(masterNFAE);
        end = System.nanoTime();
        log.info("2. NFAE Converted to NFA. Time: {} ms", (end - start) / 1_000_000);

        start = System.nanoTime();
        NFAtoDFA dfaConverter = new NFAtoDFA();
        DFA masterDFA = dfaConverter.convert(masterNFA);
        end = System.nanoTime();
        log.info("3. NFA converted to DFA. Time: {} ms", (end - start) / 1_000_000);

        start = System.nanoTime();
        DFAMinimizer minimizer = new DFAMinimizer();
        this.masterAutomaton = minimizer.minimize(masterDFA);
        end = System.nanoTime();
        log.info("4. DFA Minimized successfully. Time: {} ms", (end - start) / 1_000_000);

        buildFastTables();
        log.info("Total pipeline time: {} ms\n", (System.nanoTime() - totalStart) / 1_000_000);
    }

    private void buildFastTables() {
        this.dfaStates = new ArrayList<>(masterAutomaton.getStates());
        this.dfaStateToIdx = new HashMap<>();
        for (int i = 0; i < dfaStates.size(); i++) {
            dfaStateToIdx.put(dfaStates.get(i), i);
        }

        int numStates = dfaStates.size();
        this.transitionTable = new int[numStates][256];
        this.extendedTransitionTable = new HashMap[numStates];

        for (int i = 0; i < numStates; i++) {
            Arrays.fill(transitionTable[i], -1);
            extendedTransitionTable[i] = new HashMap<>();
        }

        for (core.lexer.models.atomic.Transition t : masterAutomaton.getTransitions()) {
            int src = dfaStateToIdx.get(t.getSource());
            int dst = dfaStateToIdx.get(t.getTarget());
            String symVal = t.getSymbol().getValue();

            if (symVal.length() == 1) {
                char c = symVal.charAt(0);
                if (c < 256) {
                    transitionTable[src][c] = dst;
                } else {
                    extendedTransitionTable[src].put(c, dst);
                }
            }
        }

        Set<State> initialStates = masterAutomaton.getInitialStates();
        if (initialStates.isEmpty()) {
            throw new IllegalStateException("Minimized DFA has no initial state.");
        }
        this.startStateIdx = dfaStateToIdx.get(initialStates.iterator().next());
    }

    public DFA getMasterAutomaton() {
        return masterAutomaton;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void addContextRule(String originalToken, String targetToken, Set<String> triggers) {
        this.contextRules.put(originalToken, new ContextRule(targetToken, triggers));
    }

    public String scan(String input) {
        this.symbolTable.clearTable();
        log.info("Scanning input...");

        char[] chars = input.toCharArray();
        int inputLength = chars.length;

        int currentIndex = 0;
        int currentLine = 1;
        int currentCol = 1;

        boolean hasErrors = false;
        List<PendingSymbol> pendingSymbols = new ArrayList<>();

        if (transitionTable == null || dfaStates == null) {
            log.error("Fatal: Scanning tables not built.");
            return "Scanning failed: Automaton not initialised.";
        }

        while (currentIndex < inputLength) {
            int lastAcceptingIndex = -1;
            String lastAcceptingToken = null;
            int stateIdx = startStateIdx;

            for (int i = currentIndex; i < inputLength; i++) {
                char ch = chars[i];
                if (ch < 256) {
                    stateIdx = transitionTable[stateIdx][ch];
                } else {
                    stateIdx = extendedTransitionTable[stateIdx].getOrDefault(ch, -1);
                }

                if (stateIdx == -1) break;

                State currentState = dfaStates.get(stateIdx);
                if (currentState.isFinal()) {
                    lastAcceptingIndex = i;
                    lastAcceptingToken = currentState.getAcceptedToken();
                }
            }

            if (lastAcceptingIndex != -1) {
                int length = lastAcceptingIndex - currentIndex + 1;
                String lexeme = new String(chars, currentIndex, length);
                boolean isSkipToken = skipTokens.contains(lastAcceptingToken);

                if (!isSkipToken) {
                    String resolvedTokenType =
                            resolveContextToken(lastAcceptingToken, pendingSymbols);
                    pendingSymbols.add(
                            new PendingSymbol(lexeme, resolvedTokenType, currentLine, currentCol));
                }

                for (int i = currentIndex; i <= lastAcceptingIndex; i++) {
                    if (chars[i] == '\n') {
                        currentLine++;
                        currentCol = 1;
                    } else {
                        currentCol++;
                    }
                }
                currentIndex = lastAcceptingIndex + 1;

            } else {
                hasErrors = true;
                int errorStart = currentIndex;
                int errorLine = currentLine;
                int errorCol = currentCol;

                if (chars[currentIndex] == '\n') {
                    currentLine++;
                    currentCol = 1;
                } else {
                    currentCol++;
                }
                currentIndex++;

                while (currentIndex < inputLength && !Character.isWhitespace(chars[currentIndex])) {
                    if (chars[currentIndex] == '\n') {
                        currentLine++;
                        currentCol = 1;
                    } else {
                        currentCol++;
                    }
                    currentIndex++;
                }

                String malformedChunk = new String(chars, errorStart, currentIndex - errorStart);
                log.error(
                        "[{}, {}]: Lexical Error: Invalid sequence '{}'",
                        errorLine,
                        errorCol,
                        malformedChunk);
            }
        }

        if (hasErrors) {
            log.warn("Scanning finished with errors. Symbol table was NOT populated.");
            symbolTable.clearTable();
            return "Scanning failed with lexical errors.";
        } else {
            for (PendingSymbol ps : pendingSymbols) {
                symbolTable.insert(ps.tokenType, ps.lexeme, ps.line, ps.col);
            }
            log.info("Scanning complete. Symbol table populated successfully.");
            return "Scanning complete.";
        }
    }

    private String resolveContextToken(String rawTokenType, List<PendingSymbol> pendingSymbols) {
        if (!contextRules.containsKey(rawTokenType) || pendingSymbols.isEmpty()) {
            return rawTokenType;
        }

        PendingSymbol previous = pendingSymbols.get(pendingSymbols.size() - 1);
        ContextRule rule = contextRules.get(rawTokenType);

        if (rule.triggers.contains(previous.tokenType) || rule.triggers.contains(previous.lexeme)) {
            return rule.targetTokenType;
        }

        return rawTokenType;
    }

    public static class ContextRule {
        public final String targetTokenType;
        public final Set<String> triggers;

        public ContextRule(String targetTokenType, Set<String> triggers) {
            this.targetTokenType = targetTokenType;
            this.triggers = triggers;
        }
    }

    private static class PendingSymbol {
        final String lexeme;
        final String tokenType;
        final int line;
        final int col;

        PendingSymbol(String lexeme, String tokenType, int line, int col) {
            this.lexeme = lexeme;
            this.tokenType = tokenType;
            this.line = line;
            this.col = col;
        }
    }
}
