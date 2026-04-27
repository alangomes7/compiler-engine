package core.parser.core;

import core.parser.models.FirstFollowTable;
import core.parser.models.Grammar;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import java.util.*;

/**
 * Computes the FIRST and FOLLOW sets for a given LL(1) Context-Free Grammar. Uses fixed-point
 * iteration to ensure sets propagate correctly through recursive and out-of-order non-terminal
 * definitions.
 */
public class FirstFollowTableBuilder {

    private final Grammar grammar;
    private final Map<Symbol, Set<Symbol>> firstSets;
    private final Map<Symbol, Set<Symbol>> followSets;

    public FirstFollowTableBuilder(Grammar grammar) {
        this.grammar = grammar;
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();

        // Initialize sets for all non-terminals
        for (Symbol nonTerminal : grammar.getNonTerminals()) {
            firstSets.put(nonTerminal, new HashSet<>());
            followSets.put(nonTerminal, new HashSet<>());
        }
    }

    /**
     * Executes the building process for both sets and returns the populated table.
     *
     * @return A completed FirstFollowTable.
     */
    public FirstFollowTable build() {
        computeFirstSets();
        computeFollowSets();
        return new FirstFollowTable(firstSets, followSets);
    }

    // ==========================================
    // 1. FIRST Set Computation
    // ==========================================
    private void computeFirstSets() {
        boolean changed = true;

        while (changed) {
            changed = false;

            for (Production production : grammar.getProductions()) {
                Symbol lhs = production.getLhs();
                List<Symbol> rhs = production.getRhs();

                // Safety check: skip if LHS is somehow not initialized
                if (!firstSets.containsKey(lhs)) continue;

                Set<Symbol> currentFirstSet = firstSets.get(lhs);
                int previousSize = currentFirstSet.size();

                // Check for Epsilon rule (e.g., A -> ε)
                if (rhs.isEmpty() || (rhs.size() == 1 && isEpsilon(rhs.get(0)))) {
                    currentFirstSet.add(Symbol.EPSILON);
                } else {
                    boolean allDeriveEpsilon = true;

                    for (Symbol rhsSymbol : rhs) {
                        Set<Symbol> rhsSymbolFirst = getFirstOfSymbol(rhsSymbol);

                        for (Symbol s : rhsSymbolFirst) {
                            if (!isEpsilon(s)) {
                                currentFirstSet.add(s);
                            }
                        }

                        // If this symbol CANNOT derive epsilon, stop evaluating the sequence
                        if (!setContainsEpsilon(rhsSymbolFirst)) {
                            allDeriveEpsilon = false;
                            break;
                        }
                    }

                    if (allDeriveEpsilon) {
                        currentFirstSet.add(Symbol.EPSILON);
                    }
                }

                if (currentFirstSet.size() > previousSize) {
                    changed = true;
                }
            }
        }
    }

    // ==========================================
    // 2. FOLLOW Set Computation
    // ==========================================
    private void computeFollowSets() {
        // Rule 1: Place EOF in the FOLLOW set of the start symbol
        if (grammar.getStartSymbol() != null && followSets.containsKey(grammar.getStartSymbol())) {
            followSets.get(grammar.getStartSymbol()).add(Symbol.EOF);
        }

        boolean changed = true;

        while (changed) {
            changed = false;

            for (Production production : grammar.getProductions()) {
                Symbol lhs = production.getLhs();
                List<Symbol> rhs = production.getRhs();

                for (int i = 0; i < rhs.size(); i++) {
                    Symbol currentSymbol = rhs.get(i);

                    // Only calculate FOLLOW sets for Non-Terminals
                    if (!currentSymbol.isTerminal() && !isEpsilon(currentSymbol)) {

                        // Safety check
                        if (!followSets.containsKey(currentSymbol)) continue;

                        Set<Symbol> currentFollowSet = followSets.get(currentSymbol);
                        int previousSize = currentFollowSet.size();

                        List<Symbol> remainingSequence = rhs.subList(i + 1, rhs.size());
                        Set<Symbol> firstOfRemaining = getFirstOfSequence(remainingSequence);

                        // Rule 2: Add FIRST(remaining) to FOLLOW(current)
                        for (Symbol s : firstOfRemaining) {
                            if (!isEpsilon(s)) {
                                currentFollowSet.add(s);
                            }
                        }

                        // Rule 3: If remaining derives Epsilon, add FOLLOW(LHS) to FOLLOW(current)
                        if (setContainsEpsilon(firstOfRemaining) || remainingSequence.isEmpty()) {
                            if (followSets.containsKey(lhs)) {
                                currentFollowSet.addAll(followSets.get(lhs));
                            }
                        }

                        if (currentFollowSet.size() > previousSize) {
                            changed = true;
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // Utility Methods
    // ==========================================
    private Set<Symbol> getFirstOfSymbol(Symbol symbol) {
        Set<Symbol> first = new HashSet<>();

        if (isEpsilon(symbol)) {
            first.add(Symbol.EPSILON);
        } else if (symbol.isTerminal() || isEOF(symbol)) {
            first.add(symbol);
        } else {
            // Non-Terminal: Fetch its calculated FIRST set
            first.addAll(firstSets.getOrDefault(symbol, new HashSet<>()));
        }
        return first;
    }

    private Set<Symbol> getFirstOfSequence(List<Symbol> sequence) {
        Set<Symbol> first = new HashSet<>();

        if (sequence.isEmpty()) {
            first.add(Symbol.EPSILON);
            return first;
        }

        boolean allDeriveEpsilon = true;

        for (Symbol symbol : sequence) {
            Set<Symbol> symbolFirst = getFirstOfSymbol(symbol);

            for (Symbol s : symbolFirst) {
                if (!isEpsilon(s)) {
                    first.add(s);
                }
            }

            if (!setContainsEpsilon(symbolFirst)) {
                allDeriveEpsilon = false;
                break;
            }
        }

        if (allDeriveEpsilon) {
            first.add(Symbol.EPSILON);
        }

        return first;
    }

    // --- Safe Comparison Helpers ---
    // These methods prevent bugs caused by memory-reference mismatches or missing equals() methods

    private boolean isEpsilon(Symbol symbol) {
        if (symbol == null) return false;
        return symbol.equals(Symbol.EPSILON)
                || "ε".equals(symbol.getName())
                || "EPSILON".equals(symbol.getName());
    }

    private boolean isEOF(Symbol symbol) {
        if (symbol == null) return false;
        return symbol.equals(Symbol.EOF)
                || "$".equals(symbol.getName())
                || "EOF".equals(symbol.getName());
    }

    private boolean setContainsEpsilon(Set<Symbol> set) {
        for (Symbol s : set) {
            if (isEpsilon(s)) return true;
        }
        return false;
    }
}
