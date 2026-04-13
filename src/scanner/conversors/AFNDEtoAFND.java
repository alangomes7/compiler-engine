package scanner.conversors;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.AFND;
import models.AFNDE;
import models.State;
import models.Transition;

public class AFNDEtoAFND {

    public static final String EPSILON = "ε";

    private final Map<State, Set<State>> closureCache = new HashMap<>();

    public AFND convert(AFNDE afnde) {
        closureCache.clear();

        // 1. Reachable states
        Set<State> allOldStates = getAllReachableStates(afnde.getStartState());

        // 2. Mapping
        Map<State, State> oldToNew = new HashMap<>(allOldStates.size());
        for (State oldState : allOldStates) {
            oldToNew.put(oldState, new State(oldState.getId()));
        }

        // Reusable sets (avoid GC pressure)
        Set<String> activeSymbols = new HashSet<>();
        Set<State> moveResult = new HashSet<>();
        Set<State> targetClosure = new HashSet<>();

        // 3. Build transitions
        for (State oldState : allOldStates) {
            State newState = oldToNew.get(oldState);

            Set<State> closureQ = getEpsilonClosure(oldState);

            boolean isFinal = false;
            String acceptedToken = null;

            activeSymbols.clear();

            for (State cState : closureQ) {
                if (cState.isFinal()) {
                    isFinal = true;
                    if (acceptedToken == null) {
                        acceptedToken = cState.getAcceptedToken();
                    }
                }

                for (Transition t : cState.getTransitions()) {
                    String symbol = t.getSymbol();
                    if (symbol != EPSILON) { // faster than equals
                        activeSymbols.add(symbol);
                    }
                }
            }

            newState.setFinal(isFinal);
            newState.setAcceptedToken(acceptedToken);

            for (String symbol : activeSymbols) {

                moveResult.clear();

                // inline getMove (faster)
                for (State s : closureQ) {
                    for (Transition t : s.getTransitions()) {
                        if (t.getSymbol().equals(symbol)) {
                            moveResult.add(t.getTarget());
                        }
                    }
                }

                targetClosure.clear();

                for (State ms : moveResult) {
                    targetClosure.addAll(getEpsilonClosure(ms));
                }

                for (State targetOld : targetClosure) {
                    newState.addTransition(symbol, oldToNew.get(targetOld));
                }
            }
        }

        // 4. Build final AFND
        State newStart = oldToNew.get(afnde.getStartState());

        Set<State> reachableInNew = getAllReachableStates(newStart);

        Set<State> finalStates = new HashSet<>();
        for (State s : reachableInNew) {
            if (s.isFinal()) {
                finalStates.add(s);
            }
        }

        return new AFND(afnde.getTokenName() + "_AFND", newStart, finalStates);
    }

    // ========================================================================
    // BFS (optimized)
    // ========================================================================
    private Set<State> getAllReachableStates(State start) {
        Set<State> visited = new HashSet<>();
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

    // ========================================================================
    // EPSILON CLOSURE (cached)
    // ========================================================================
    private Set<State> getEpsilonClosure(State state) {
        Set<State> cached = closureCache.get(state);
        if (cached != null) return cached;

        Set<State> closure = new HashSet<>();
        ArrayDeque<State> queue = new ArrayDeque<>();

        closure.add(state);
        queue.add(state);

        while (!queue.isEmpty()) {
            State current = queue.poll();

            for (Transition t : current.getTransitions()) {
                if (t.getSymbol() == EPSILON) { // reference compare
                    State target = t.getTarget();
                    if (closure.add(target)) {
                        queue.add(target);
                    }
                }
            }
        }

        closureCache.put(state, closure);
        return closure;
    }
}