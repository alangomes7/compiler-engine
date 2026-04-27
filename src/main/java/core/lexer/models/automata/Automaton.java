package core.lexer.models.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract base class for finite automata (DFA, NFA, ε-NFA). Provides common state and transition
 * management, as well as alphabet handling.
 *
 * @author Generated
 * @version 1.0
 */
public abstract class Automaton {
    protected final String tokenName;

    protected final Set<State> states;
    protected final Set<Transition> transitions;
    protected final Alphabet alphabet;

    /**
     * Constructs an automaton for a specific token name.
     *
     * @param tokenName the name of the token recognised by this automaton
     */
    public Automaton(String tokenName) {
        this.tokenName = tokenName;
        this.states = new HashSet<>();
        this.transitions = new HashSet<>();
        this.alphabet = new Alphabet();
    }

    /**
     * Adds a state to the automaton.
     *
     * @param state the state to add
     */
    public void addState(State state) {
        this.states.add(state);
    }

    /**
     * Removes a state from the automaton. All transitions that involve this state (as source or
     * target) are also removed. After removal, the alphabet is cleaned up: any symbol that no
     * longer appears on any transition is removed from the alphabet.
     *
     * @param state the state to remove
     * @return true if the state was present and removed, false otherwise
     */
    public boolean removeState(State state) {
        if (state == null || !states.contains(state)) {
            return false;
        }
        // Remove all transitions where this state is source or target
        transitions.removeIf(t -> t.getSource().equals(state) || t.getTarget().equals(state));
        // Remove the state itself
        states.remove(state);
        // Clean up alphabet symbols that are no longer used
        cleanupAlphabet();
        return true;
    }

    /** Removes from the alphabet any symbol that is not used by any remaining transition. */
    private void cleanupAlphabet() {
        Set<Symbol> usedSymbols =
                transitions.stream().map(Transition::getSymbol).collect(Collectors.toSet());
        alphabet.getSymbols().retainAll(usedSymbols);
    }

    /**
     * Adds a transition to the automaton. Also adds the source and target states, and the
     * transition's symbol to the alphabet.
     *
     * @param transition the transition to add
     */
    public void addTransition(Transition transition) {
        this.transitions.add(transition);
        this.states.add(transition.getSource());
        this.states.add(transition.getTarget());
        this.alphabet.addSymbol(transition.getSymbol());
    }

    /**
     * Returns the set of initial states.
     *
     * @return a set of states that have the initial flag set
     */
    public Set<State> getInitialStates() {
        return states.stream().filter(State::isInitial).collect(Collectors.toSet());
    }

    /**
     * Returns the set of final (accepting) states.
     *
     * @return a set of states that have the final flag set
     */
    public Set<State> getFinalStates() {
        return states.stream().filter(State::isFinal).collect(Collectors.toSet());
    }

    /**
     * Returns all states in the automaton.
     *
     * @return the set of states
     */
    public Set<State> getStates() {
        return Collections.unmodifiableSet(states);
    }

    /**
     * Returns all transitions in the automaton.
     *
     * @return the set of transitions
     */
    public Set<Transition> getTransitions() {
        return Collections.unmodifiableSet(transitions);
    }

    /**
     * Returns the alphabet of this automaton.
     *
     * @return the alphabet object
     */
    public Alphabet getAlphabet() {
        return alphabet;
    }

    /**
     * Returns the token name associated with this automaton.
     *
     * @return the token name
     */
    public String getTokenName() {
        return tokenName;
    }
}
