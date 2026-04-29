package core.lexer.models.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Automaton {
    protected final String tokenName;

    protected final Set<State> states;
    protected final Set<Transition> transitions;
    protected final Alphabet alphabet;

    public Automaton(String tokenName) {
        this.tokenName = tokenName;
        this.states = new HashSet<>();
        this.transitions = new HashSet<>();
        this.alphabet = new Alphabet();
    }

    public void addState(State state) {
        this.states.add(state);
    }

    public boolean removeState(State state) {
        if (state == null || !states.contains(state)) {
            return false;
        }
        transitions.removeIf(t -> t.getSource().equals(state) || t.getTarget().equals(state));
        states.remove(state);
        cleanupAlphabet();
        return true;
    }

    private void cleanupAlphabet() {
        Set<Symbol> usedSymbols =
                transitions.stream().map(Transition::getSymbol).collect(Collectors.toSet());
        alphabet.getSymbols().retainAll(usedSymbols);
    }

    public void addTransition(Transition transition) {
        this.transitions.add(transition);
        this.states.add(transition.getSource());
        this.states.add(transition.getTarget());
        this.alphabet.addSymbol(transition.getSymbol());
    }

    public Set<State> getInitialStates() {
        return states.stream().filter(State::isInitial).collect(Collectors.toSet());
    }

    public Set<State> getFinalStates() {
        return states.stream().filter(State::isFinal).collect(Collectors.toSet());
    }

    public Set<State> getStates() {
        return Collections.unmodifiableSet(states);
    }

    public Set<Transition> getTransitions() {
        return Collections.unmodifiableSet(transitions);
    }

    public Alphabet getAlphabet() {
        return alphabet;
    }

    public String getTokenName() {
        return tokenName;
    }
}
