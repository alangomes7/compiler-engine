package core.lexer.core.conversors;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.NFAE;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import models.atomic.Constants;

/**
 * Builds ε‑NFA fragments from regular expression operations. Implements Thompson's construction:
 * each regular expression operator (concatenation, union, Kleene star, optional, one‑or‑more) is
 * translated into an ε‑NFA using standard patterns.
 *
 * <p>All fragments are built incrementally and can be merged to form a complete lexical scanner
 * automaton.
 *
 * @author Generated
 * @version 1.0
 */
public class ReToNFAE {

    private static final String CONCAT = "Concat";
    private static final String UNION = "Union";
    private static final String STAR = "Star";
    private static final String OPTIONAL = "Optional";
    private static final String PLUS = "Plus";
    private static final String MASTER = "MasterLexer";

    private final Symbol EPSILON = new Symbol(Constants.EPSILON);
    private int stateCounter = 0;
    private final List<NFAE> skipPatterns = new ArrayList<>();

    /**
     * Merges all states and transitions from a source ε‑NFA into a target ε‑NFA.
     *
     * @param target the automaton that will receive the elements
     * @param source the automaton whose elements will be added
     */
    private void mergeInto(NFAE target, NFAE source) {
        source.getStates().forEach(target::addState);
        source.getTransitions().forEach(target::addTransition);
    }

    /**
     * Creates an ε‑NFA that accepts a single literal symbol (character or string).
     *
     * @param sym the symbol (as a string) to accept
     * @return an ε‑NFA for the symbol
     */
    public NFAE symbol(String sym) {
        NFAE nfa = new NFAE(sym);
        State start = new State(stateCounter++, true, false);
        State accept = new State(stateCounter++, false, true);

        nfa.addState(start);
        nfa.addState(accept);
        nfa.addTransition(new Transition(start, accept, new Symbol(sym)));
        return nfa;
    }

    /**
     * Builds an ε‑NFA for the concatenation of two ε‑NFA fragments. The final states of the first
     * are connected via ε‑transitions to the initial states of the second.
     *
     * @param a left operand
     * @param b right operand
     * @return ε‑NFA for {@code a} followed by {@code b}
     */
    public NFAE concat(NFAE a, NFAE b) {
        NFAE result = new NFAE(CONCAT);
        mergeInto(result, a);
        mergeInto(result, b);

        Set<State> aFinals = a.getFinalStates();
        Set<State> bInitials = b.getInitialStates();

        aFinals.forEach(s -> s.setFinalState(false));
        bInitials.forEach(s -> s.setInitial(false));

        for (State af : aFinals) {
            for (State bi : bInitials) {
                result.addTransition(new Transition(af, bi, EPSILON));
            }
        }
        return result;
    }

    /**
     * Builds an ε‑NFA for the union (alternative) of two ε‑NFA fragments. A new start state is
     * added with ε‑transitions to the starts of both fragments, and a new final state is added with
     * ε‑transitions from both fragments' finals.
     *
     * @param a first alternative
     * @param b second alternative
     * @return ε‑NFA for {@code a|b}
     */
    public NFAE union(NFAE a, NFAE b) {
        NFAE result = new NFAE(UNION);
        mergeInto(result, a);
        mergeInto(result, b);

        State start = new State(stateCounter++, true, false);
        State accept = new State(stateCounter++, false, true);
        result.addState(start);
        result.addState(accept);

        for (State ai : a.getInitialStates()) {
            ai.setInitial(false);
            result.addTransition(new Transition(start, ai, EPSILON));
        }
        for (State bi : b.getInitialStates()) {
            bi.setInitial(false);
            result.addTransition(new Transition(start, bi, EPSILON));
        }

        for (State af : a.getFinalStates()) {
            af.setFinalState(false);
            result.addTransition(new Transition(af, accept, EPSILON));
        }
        for (State bf : b.getFinalStates()) {
            bf.setFinalState(false);
            result.addTransition(new Transition(bf, accept, EPSILON));
        }
        return result;
    }

    /**
     * Builds an ε‑NFA for the Kleene star (zero or more repetitions). Adds ε‑transitions from the
     * start to the fragment's start, from the fragment's final back to its start, and directly from
     * the new start to the new final state.
     *
     * @param a the fragment to repeat
     * @return ε‑NFA for {@code a*}
     */
    public NFAE kleeneStar(NFAE a) {
        NFAE result = new NFAE(STAR);
        mergeInto(result, a);

        State start = new State(stateCounter++, true, false);
        State accept = new State(stateCounter++, false, true);
        result.addState(start);
        result.addState(accept);

        for (State ai : a.getInitialStates()) {
            ai.setInitial(false);
            result.addTransition(new Transition(start, ai, EPSILON));
            for (State af : a.getFinalStates()) {
                result.addTransition(new Transition(af, ai, EPSILON));
            }
        }

        for (State af : a.getFinalStates()) {
            af.setFinalState(false);
            result.addTransition(new Transition(af, accept, EPSILON));
        }

        result.addTransition(new Transition(start, accept, EPSILON));
        return result;
    }

    /**
     * Builds an ε‑NFA for the optional operator (zero or one occurrence).
     *
     * @param a the fragment to make optional
     * @return ε‑NFA for {@code a?}
     */
    public NFAE optional(NFAE a) {
        NFAE result = new NFAE(OPTIONAL);
        mergeInto(result, a);

        State start = new State(stateCounter++, true, false);
        State accept = new State(stateCounter++, false, true);
        result.addState(start);
        result.addState(accept);

        for (State ai : a.getInitialStates()) {
            ai.setInitial(false);
            result.addTransition(new Transition(start, ai, EPSILON));
        }

        for (State af : a.getFinalStates()) {
            af.setFinalState(false);
            result.addTransition(new Transition(af, accept, EPSILON));
        }

        result.addTransition(new Transition(start, accept, EPSILON));
        return result;
    }

    /**
     * Builds an ε‑NFA for the one‑or‑more operator (positive closure). Connects the fragment's
     * final states back to its initial states via ε‑transitions and adds a new final state
     * reachable from the fragment's finals.
     *
     * @param a the fragment to repeat one or more times
     * @return ε‑NFA for {@code a+}
     */
    public NFAE oneOrMore(NFAE a) {
        NFAE result = new NFAE(PLUS);
        mergeInto(result, a);

        State accept = new State(stateCounter++, false, true);
        result.addState(accept);

        for (State ai : a.getInitialStates()) {
            for (State af : a.getFinalStates()) {
                result.addTransition(new Transition(af, ai, EPSILON));
            }
        }

        for (State af : a.getFinalStates()) {
            af.setFinalState(false);
            result.addTransition(new Transition(af, accept, EPSILON));
        }

        return result;
    }

    /**
     * Assigns a token name to an ε‑NFA fragment. Sets the given token on all final states of the
     * fragment.
     *
     * @param tokenName the token type to assign
     * @param nfa the ε‑NFA fragment
     * @return the same ε‑NFA with token name assigned to final states
     */
    public NFAE nameToken(String tokenName, NFAE nfa) {
        NFAE result = new NFAE(tokenName);
        mergeInto(result, nfa);
        for (State f : result.getFinalStates()) {
            f.setAcceptedToken(tokenName);
        }
        return result;
    }

    /**
     * Builds a master ε‑NFA that combines multiple token rule ε‑NFAs. A single start state is
     * created with ε‑transitions to each token rule's start state.
     *
     * @param tokenRules a list of ε‑NFA fragments, each representing a token
     * @return an ε‑NFA that accepts any of the token patterns
     */
    public NFAE buildMasterScanner(List<NFAE> tokenRules) {
        NFAE master = new NFAE(MASTER);
        State masterStart = new State(stateCounter++, true, false);
        master.addState(masterStart);

        for (NFAE rule : tokenRules) {
            mergeInto(master, rule);
            for (State init : rule.getInitialStates()) {
                init.setInitial(false);
                master.addTransition(new Transition(masterStart, init, EPSILON));
            }
        }
        return master;
    }

    /**
     * Registers a pattern as a skip rule. During lexical analysis, any longest match that
     * corresponds to a skip pattern will be silently consumed and no token will be emitted to the
     * parser.
     *
     * @param tokenName a user‑friendly name for the skip pattern (e.g., "COMMENT")
     * @param nfa the ε‑NFA that recognises the pattern to be skipped
     */
    public void addSkipPattern(String tokenName, NFAE nfa) {
        // The skip pattern is stored with a special token type; the scanner
        // checks this token type later to decide whether to skip the matched
        // lexeme without returning it to the parser.
        NFAE named = nameToken(Constants.SKIP_TOKEN, nfa);
        skipPatterns.add(named);
    }

    /**
     * Returns all currently registered skip patterns. The caller (typically the lexer builder)
     * should merge these fragments together with the normal token rules when constructing the
     * master scanner automaton.
     *
     * @return a list of ε‑NFA fragments tagged as skip patterns
     */
    public List<NFAE> getSkipPatterns() {
        return Collections.unmodifiableList(skipPatterns);
    }
}
