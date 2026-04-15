package core.lexer.conversors;

import java.util.*;
import models.atomic.Constants;
import models.atomic.State;
import models.atomic.Transition;
import models.automata.AFND;
import models.automata.AFNDE;

public class AFNDEtoAFND {

    // Cache local para evitar hit no Map global se houver reuso da classe
    private final Map<State, Set<State>> closureCache = new HashMap<>();

    public AFND convert(AFNDE afnde) {
        closureCache.clear();
        
        // 1. Coleta estados originais
        Set<State> allOldStates = getAllReachableStates(afnde.getStartState());
        
        // 2. Mapeamento e Pré-cálculo de Closures (Crucial para performance)
        Map<State, State> oldToNew = new HashMap<>(allOldStates.size());
        for (State oldState : allOldStates) {
            oldToNew.put(oldState, new State(oldState.getId()));
            getEpsilonClosure(oldState); 
        }

        // 3. Construção de Transições Otimizada
        for (State oldState : allOldStates) {
            State newState = oldToNew.get(oldState);
            Set<State> closureQ = closureCache.get(oldState);

            // Mapa temporário para agrupar destinos por símbolo: evita loops repetitivos
            Map<String, Set<State>> transitionsBySymbol = new HashMap<>();
            
            boolean isFinal = false;
            String acceptedToken = null;

            for (State cState : closureQ) {
                // Define se o novo estado é final
                if (cState.isFinal()) {
                    isFinal = true;
                    if (acceptedToken == null) acceptedToken = cState.getAcceptedToken();
                }

                // Agrupa transições que não são Epsilon
                for (Transition t : cState.getTransitions()) {
                    String symbol = t.getSymbol();
                    if (symbol != Constants.EPSILON && !symbol.equals(Constants.EPSILON)) {
                        transitionsBySymbol
                            .computeIfAbsent(symbol, k -> new HashSet<>())
                            .add(t.getTarget());
                    }
                }
            }

            newState.setFinal(isFinal);
            newState.setAcceptedToken(acceptedToken);

            // Adiciona transições ao novo estado
            for (Map.Entry<String, Set<State>> entry : transitionsBySymbol.entrySet()) {
                String symbol = entry.getKey();
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
        
        // Coleta estados finais apenas dos que restaram no novo mapeamento
        Set<State> finalStates = new HashSet<>();
        for (State s : oldToNew.values()) {
            if (s.isFinal()) finalStates.add(s);
        }

        return new AFND(afnde.getTokenName() + "_AFND", newStart, finalStates);
    }

    private Set<State> getAllReachableStates(State start) {
        Set<State> visited = new LinkedHashSet<>(); // Linked garante ordem de inserção se necessário
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
                // Otimização: comparação de referência primeiro
                if (t.getSymbol() == Constants.EPSILON || t.getSymbol().equals(Constants.EPSILON)) {
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