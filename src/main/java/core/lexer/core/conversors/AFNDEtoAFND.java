package core.lexer.core.conversors;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Symbol;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.AFND;
import core.lexer.models.automata.AFNDE;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import models.atomic.Constants;

public class AFNDEtoAFND {

    private final Map<State, Set<State>> closureCache = new HashMap<>();

    public AFND convert(AFNDE afnde) {
        closureCache.clear();

        Set<State> allOldStates = getAllReachableStates(afnde.getStartState());

        Map<State, State> oldToNew = new HashMap<>(allOldStates.size());
        for (State oldState : allOldStates) {
            oldToNew.put(oldState, new State(oldState.getId()));
            getEpsilonClosure(oldState);
        }

        for (State oldState : allOldStates) {
            State newState = oldToNew.get(oldState);
            Set<State> closureQ = closureCache.get(oldState);

            Map<Symbol, Set<State>> transitionsBySymbol = new HashMap<>();

            boolean isFinal = false;
            String acceptedToken = null;

            for (State cState : closureQ) {
                if (cState.isFinal()) {
                    isFinal = true;
                    if (acceptedToken == null) acceptedToken = cState.getAcceptedToken();
                }

                for (Transition t : cState.getTransitions()) {
                    Symbol symbol = t.getSymbol();
                    if (!symbol.getValue().equals(Constants.EPSILON)) {
                        transitionsBySymbol
                                .computeIfAbsent(symbol, k -> new HashSet<>())
                                .add(t.getTarget());
                    }
                }
            }

            newState.setFinal(isFinal);
            newState.setAcceptedToken(acceptedToken);

            for (Map.Entry<Symbol, Set<State>> entry : transitionsBySymbol.entrySet()) {
                Symbol symbol = entry.getKey();
                Set<State> targets = entry.getValue();

                Set<State> fullTargetClosure = new HashSet<>();
                for (State t : targets) {
                    fullTargetClosure.addAll(closureCache.get(t));
                }

                for (State targetOld : fullTargetClosure) {
                    State newTarget = oldToNew.get(targetOld);
                    if (newTarget != null) {
                        newState.addTransition(symbol, newTarget);
                    }
                }
            }
        }

        State newStart = oldToNew.get(afnde.getStartState());

        Set<State> finalStates = new HashSet<>();
        for (State s : oldToNew.values()) {
            if (s.isFinal()) finalStates.add(s);
        }

        return new AFND(afnde.getTokenName() + "_AFND", newStart, finalStates);
    }

    private Set<State> getAllReachableStates(State start) {
        Set<State> visited = new LinkedHashSet<>();
        ArrayDeque<State> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            State current = queue.poll();
            for (Transition t : current.getTransitions()) {
                State target = t.getTarget();
                if (target != null && visited.add(target)) {
                    queue.add(target);
                }
            }
        }
        return visited;
    }

    private Set<State> getEpsilonClosure(State state) {
        if (closureCache.containsKey(state)) return closureCache.get(state);

        Set<State> closure = new HashSet<>();
        ArrayDeque<State> queue = new ArrayDeque<>();
        closure.add(state);
        queue.add(state);

        while (!queue.isEmpty()) {
            State current = queue.poll();
            for (Transition t : current.getTransitions()) {
                if (t.getSymbol().getValue().equals(Constants.EPSILON)) {
                    if (closure.add(t.getTarget())) {
                        queue.add(t.getTarget());
                    }
                }
            }
        }
        closureCache.put(state, closure);
        return closure;
    }
}
