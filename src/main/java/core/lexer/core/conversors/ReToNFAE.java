package core.lexer.core.conversors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.NFAE;
import models.atomic.Constants;

public class ReToNFAE {
    private static final Logger log = LoggerFactory.getLogger(ReToNFAE.class);

    private static final String CONCAT = "Concat";
    private static final String UNION = "Union";
    private static final String STAR = "Star";
    private static final String OPTIONAL = "Optional";
    private static final String PLUS = "Plus";
    private static final String MASTER = "MasterLexer";

    private final Symbol EPSILON = new Symbol(Constants.EPSILON);
    private int stateCounter = 0;
    private final List<NFAE> skipPatterns = new ArrayList<>();

    private void mergeInto(NFAE target, NFAE source) {
        source.getStates().forEach(target::addState);
        source.getTransitions().forEach(target::addTransition);
    }

    public NFAE symbol(String sym) {
        NFAE nfa = new NFAE(sym);
        State start = new State(stateCounter++, true, false);
        State accept = new State(stateCounter++, false, true);

        nfa.addState(start);
        nfa.addState(accept);
        nfa.addTransition(new Transition(start, accept, new Symbol(sym)));
        
        log.debug("Created symbol NFAE for '{}' (States: {} -> {})", sym, start.getId(), accept.getId());
        return nfa;
    }

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
        
        log.debug("Concatenated NFAEs. Resulting state count: {}", result.getStates().size());
        return result;
    }

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
        
        log.debug("Unioned NFAEs. Resulting state count: {}", result.getStates().size());
        return result;
    }

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
        
        log.debug("Applied Kleene Star (*). Resulting state count: {}", result.getStates().size());
        return result;
    }

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
        
        log.debug("Applied Optional (?). Resulting state count: {}", result.getStates().size());
        return result;
    }

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

        log.debug("Applied One-or-More (+). Resulting state count: {}", result.getStates().size());
        return result;
    }

    public NFAE nameToken(String tokenName, NFAE nfa) {
        NFAE result = new NFAE(tokenName);
        mergeInto(result, nfa);
        for (State f : result.getFinalStates()) {
            f.setAcceptedToken(tokenName);
        }
        log.info("Registered Token NFAE '{}' with {} states.", tokenName, result.getStates().size());
        return result;
    }

    public NFAE buildMasterScanner(List<NFAE> tokenRules) {
        log.info("Building Master Scanner NFAE from {} token rules...", tokenRules.size());
        long startTime = System.currentTimeMillis();

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

        long duration = System.currentTimeMillis() - startTime;
        log.info("Master Scanner built in {} ms. Total NFAE States: {}", duration, master.getStates().size());
        return master;
    }

    public void addSkipPattern(String tokenName, NFAE nfa) {
        NFAE named = nameToken(Constants.SKIP_TOKEN, nfa);
        skipPatterns.add(named);
        log.debug("Added skip pattern for original definition '{}'. Total skip patterns: {}", tokenName, skipPatterns.size());
    }

    public List<NFAE> getSkipPatterns() {
        return Collections.unmodifiableList(skipPatterns);
    }
}