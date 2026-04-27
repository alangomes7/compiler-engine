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
 * Main lexical analyzer that converts an input string into a stream of tokens.
 *
 * <p>The lexer is built from a list of {@link Rule} definitions. It constructs a deterministic
 * finite automaton (DFA) via the following pipeline:
 *
 * <ol>
 *   <li>Parse each rule into an ε‑NFA (Thompson's construction).
 *   <li>Separate normal tokens from skip patterns (comments, whitespace).
 *   <li>Combine all rule ε‑NFAs (including skip patterns) into a single master ε‑NFA.
 *   <li>Convert the master ε‑NFA to a standard NFA (remove ε‑transitions).
 *   <li>Convert the NFA to a DFA (subset construction with BitSet).
 *   <li>Minimize the DFA (Hopcroft's algorithm).
 *   <li>Build a fast O(1) transition table for ASCII and a lookup map for Unicode.
 * </ol>
 *
 * <p>The resulting DFA is then used to scan input text, producing a {@link SymbolTable} with the
 * full token stream. During scanning, skip patterns are silently discarded and never reach the
 * parser.
 *
 * @author Generated
 * @version 2.1
 */
public class Lexer {

    private static final Logger log = LoggerFactory.getLogger(Lexer.class);

    /** The minimized master DFA used for all scanning operations. */
    private DFA masterAutomaton;

    /** The original list of lexical rules that define the language's tokens. */
    private final List<Rule> rules;

    /** Accumulated symbol table where recognised tokens are stored during a scan. */
    private final SymbolTable symbolTable;

    /**
     * Set of token types that contain regex metacharacters and must be handled by the DFA
     * dynamically.
     */
    private final Set<String> dynamicTokens;

    /**
     * Set of token types that are considered skip‑patterns and will be discarded during scanning.
     */
    private final Set<String> skipTokens;

    // ---------- Fast scanning structures ----------

    /** Ordered list of states of the minimized DFA (index corresponds to transition table row). */
    private List<State> dfaStates;

    /** Map from DFA state to its index in {@link #dfaStates}. */
    private Map<State, Integer> dfaStateToIdx;

    /** Fast transition table for ASCII characters (0–255). {@code -1} indicates dead state. */
    private int[][] transitionTable;

    /** Transition map for non‑ASCII characters (Unicode code points ≥ 256). */
    private Map<Character, Integer>[] extendedTransitionTable;

    /** Index of the unique start state in {@link #dfaStates}. */
    private int startStateIdx;

    /**
     * Constructs a lexer from a list of token rules. The rules are categorised and the scanning
     * automaton (DFA) is built immediately.
     *
     * @param rules the list of lexical rules (token definitions) to be compiled into the scanner
     */
    public Lexer(List<Rule> rules) {
        this.rules = rules;
        log.info("--- Building Scanner For: {} Tokens ---", rules.size());

        this.symbolTable = new SymbolTable();
        this.dynamicTokens = new HashSet<>();
        this.skipTokens = new HashSet<>();

        categorizeRules();
        buildScanner();
    }

    /**
     * Categorises the rules into skip tokens and dynamic tokens, and ensures that the generic
     * skip‑pattern token type ({@link Constants#SKIP_TOKEN}) is always treated as a skip token.
     */
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
        // Ensure the artificial token type for skip patterns is recognised
        skipTokens.add(Constants.SKIP_TOKEN);
    }

    /**
     * Builds the scanning automaton through the full conversion pipeline, handling skip patterns
     * separately so they are merged into the master DFA but never produce parser‑visible tokens.
     */
    private void buildScanner() {
        log.info("--- Building Scanner Pipeline ---");
        long totalStart = System.nanoTime();
        long start, end;

        start = System.nanoTime();
        ReToNFAE nfaeGenerator = new ReToNFAE();
        RuleParser parser = new RuleParser(nfaeGenerator);
        List<NFAE> nfaeList = new ArrayList<>(rules.size()); // normal (non‑skip) rules only

        for (Rule rule : rules) {
            NFAE nfa = parser.parse(rule); // parse rule as usual (returns NFAE with token name)
            if (rule.isSkip()) {
                // Register as a skip pattern – the generator stores it with token type __SKIP__
                nfaeGenerator.addSkipPattern(rule.getTokenType(), nfa);
                // Do NOT add to nfaeList; skip patterns will be merged later
            } else {
                nfaeList.add(nfa);
            }
        }
        end = System.nanoTime();
        log.info(
                "0. Rules parsed into NFAEs (skip patterns separated). Time: {} ms",
                (end - start) / 1_000_000);

        // Merge skip patterns into the main list before building master automaton
        List<NFAE> allPatterns = new ArrayList<>(nfaeList);
        allPatterns.addAll(nfaeGenerator.getSkipPatterns());

        start = System.nanoTime();
        NFAE masterNFAE = nfaeGenerator.buildMasterScanner(allPatterns);
        end = System.nanoTime();
        log.info(
                "1. Master NFAE built (including skip patterns). Time: {} ms",
                (end - start) / 1_000_000);

        start = System.nanoTime();
        NFAEtoNFA eConverter = new NFAEtoNFA();
        NFA masterNFA = eConverter.convert(masterNFAE);
        end = System.nanoTime();
        log.info("2. NFAE Converted to NFA. Time: {} ms", (end - start) / 1_000_000);

        start = System.nanoTime();
        NFAtoDFA dfaConverter = new NFAtoDFA();
        DFA masterDFA = dfaConverter.convert(masterNFA);
        end = System.nanoTime();
        log.info(
                "3. NFA converted to strictly deterministic DFA. Time: {} ms",
                (end - start) / 1_000_000);

        start = System.nanoTime();
        DFAMinimizer minimizer = new DFAMinimizer();
        this.masterAutomaton = minimizer.minimize(masterDFA);
        end = System.nanoTime();
        log.info(
                "4. DFA Minimized (Hopcroft) successfully. Time: {} ms", (end - start) / 1_000_000);

        // Build fast scanning structures
        buildFastTables();

        log.info("Total pipeline time: {} ms\n", (System.nanoTime() - totalStart) / 1_000_000);
    }

    /**
     * Prepares the fast scanning tables from the minimized DFA. ASCII transitions (0–255) are
     * stored in a direct‑index array, while higher Unicode code points are handled via a hash map
     * per state.
     *
     * @throws IllegalStateException if the minimized DFA has no initial state
     */
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
        State startState = initialStates.iterator().next();
        this.startStateIdx = dfaStateToIdx.get(startState);
    }

    /**
     * Returns the minimal DFA that drives the scanning engine.
     *
     * @return the master automaton
     */
    public DFA getMasterAutomaton() {
        return masterAutomaton;
    }

    /**
     * Returns the symbol table that will be populated during a successful scan.
     *
     * @return the symbol table
     */
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * Scans the entire input string and populates the symbol table with recognised tokens.
     *
     * <p>Uses the maximal munch (longest match) rule: at each position the longest possible token
     * that leads to an accepting state is selected. Skip patterns (whitespace, comments) are
     * automatically discarded and do not appear in the symbol table. If any lexical error occurs,
     * the symbol table is cleared and the method returns an error description.
     *
     * @param input the source code to scan
     * @return a status message ("Scanning complete." or an error description)
     */
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

        // Ensure scanning tables are ready
        if (transitionTable == null || dfaStates == null) {
            log.error("Fatal: Scanning tables not built.");
            return "Scanning failed: Automaton not initialised.";
        }

        while (currentIndex < inputLength) {
            int lastAcceptingIndex = -1;
            String lastAcceptingToken = null;
            int stateIdx = startStateIdx;

            // Simulate the DFA using the fast transition tables
            for (int i = currentIndex; i < inputLength; i++) {
                char ch = chars[i];

                // Hybrid: O(1) for ASCII, O(1) map lookup for Unicode
                if (ch < 256) {
                    stateIdx = transitionTable[stateIdx][ch];
                } else {
                    stateIdx = extendedTransitionTable[stateIdx].getOrDefault(ch, -1);
                }

                if (stateIdx == -1) {
                    break;
                }
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

                if (log.isDebugEnabled()) {
                    String displayLexeme =
                            lexeme.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
                    log.debug(
                            "[{}, {}]: Found Token: <{}, '{}'>",
                            currentLine,
                            currentCol,
                            lastAcceptingToken,
                            displayLexeme);
                }

                if (!isSkipToken) {
                    pendingSymbols.add(
                            new PendingSymbol(lexeme, lastAcceptingToken, currentLine, currentCol));
                }

                // Update line/column counters
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

                // Force consume at least the first invalid character to prevent infinite loops
                if (chars[currentIndex] == '\n') {
                    currentLine++;
                    currentCol = 1;
                } else {
                    currentCol++;
                }
                currentIndex++;

                // Continue consuming the rest of the unrecognised chunk until whitespace
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

    /**
     * Lightweight record used internally to postpone symbol table insertion until the entire input
     * has been verified error‑free.
     */
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
