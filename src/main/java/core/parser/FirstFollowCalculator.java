package core.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Grammar;

public class FirstFollowCalculator {
    public static final String EPSILON = "ε";
    public static final String EOF = "$";

    private final Grammar grammar;
    private final Map<String, Set<String>> firstSets = new HashMap<>();
    private final Map<String, Set<String>> followSets = new HashMap<>();

    public FirstFollowCalculator(Grammar grammar) {
        this.grammar = grammar;
        initializeSets();
    }

    public Map<String, Set<String>> getFirstSets() {
        return firstSets;
    }

    public Map<String, Set<String>> getFollowSets() {
        return followSets;
    }

    private void initializeSets() {
        for (String nt : grammar.getNonTerminals()) {
            firstSets.put(nt, new HashSet<>());
            followSets.put(nt, new HashSet<>());
        }
        // Identify terminals implicitly (symbols that are not non-terminals and not epsilon)
        for (List<List<String>> rhsList : grammar.getRules().values()) {
            for (List<String> rhs : rhsList) {
                for (String symbol : rhs) {
                    if (!grammar.getNonTerminals().contains(symbol) && !symbol.equals(EPSILON)) {
                        grammar.getTerminals().add(symbol);
                        firstSets.computeIfAbsent(symbol, k -> new HashSet<>()).add(symbol);
                    }
                }
            }
        }
    }

    public void computeSets() {
        computeFirstSets();
        computeFollowSets();
    }

    private void computeFirstSets() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String lhs : grammar.getNonTerminals()) {
                for (List<String> rhs : grammar.getRules().get(lhs)) {
                    int i = 0;
                    boolean allEpsilon = true;
                    
                    for (String symbol : rhs) {
                        Set<String> symbolFirst = firstSets.getOrDefault(symbol, new HashSet<>(Collections.singleton(symbol)));
                        
                        // Add everything except epsilon
                        for (String f : symbolFirst) {
                            if (!f.equals(EPSILON) && firstSets.get(lhs).add(f)) {
                                changed = true;
                            }
                        }

                        if (!symbolFirst.contains(EPSILON)) {
                            allEpsilon = false;
                            break;
                        }
                        i++;
                    }

                    // If all symbols in RHS can derive Epsilon, add Epsilon to LHS
                    if (allEpsilon && firstSets.get(lhs).add(EPSILON)) {
                        changed = true;
                    }
                }
            }
        }
    }

    private void computeFollowSets() {
        // Rule 1: Place $ in Follow(Start)
        followSets.get(grammar.getStartSymbol()).add(EOF);

        boolean changed = true;
        while (changed) {
            changed = false;

            for (String lhs : grammar.getNonTerminals()) {
                for (List<String> rhs : grammar.getRules().get(lhs)) {
                    for (int i = 0; i < rhs.size(); i++) {
                        String currentSymbol = rhs.get(i);
                        
                        if (!grammar.getNonTerminals().contains(currentSymbol)) continue;

                        Set<String> currentFollow = followSets.get(currentSymbol);
                        boolean nextDerivesEpsilon = true;

                        // Rule 2: If A -> α B β, then everything in FIRST(β) except ε is in FOLLOW(B)
                        for (int j = i + 1; j < rhs.size(); j++) {
                            String nextSymbol = rhs.get(j);
                            Set<String> nextFirst = firstSets.getOrDefault(nextSymbol, new HashSet<>(Collections.singleton(nextSymbol)));
                            
                            for (String f : nextFirst) {
                                if (!f.equals(EPSILON) && currentFollow.add(f)) {
                                    changed = true;
                                }
                            }
                            
                            if (!nextFirst.contains(EPSILON)) {
                                nextDerivesEpsilon = false;
                                break;
                            }
                        }

                        // Rule 3: If A -> α B, or A -> α B β where FIRST(β) contains ε, add FOLLOW(A) to FOLLOW(B)
                        if (nextDerivesEpsilon) {
                            for (String f : followSets.get(lhs)) {
                                if (currentFollow.add(f)) {
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void printTables() {
        System.out.println(String.format("%-25s | %-40s | %s", "NON-TERMINAL", "FIRST", "FOLLOW"));
        System.out.println("-----------------------------------------------------------------------------------------");
        for (String nt : grammar.getNonTerminals()) {
            System.out.println(String.format("%-25s | %-40s | %s", 
                nt, 
                firstSets.get(nt), 
                followSets.get(nt)));
        }
    }
}