package core.parser.core;

import core.parser.models.FirstFollowTable;
import core.parser.models.Grammar;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import java.util.List;
import java.util.Set;

public class FirstFollowTableBuilder {

    /**
     * Computes and builds the First and Follow sets (the FirstFollow table) for a given Grammar.
     */
    public static FirstFollowTable build(Grammar grammar) {
        FirstFollowTable table = new FirstFollowTable();

        computeFirstSets(grammar, table);
        computeFollowSets(grammar, table);

        return table;
    }

    private static void computeFirstSets(Grammar grammar, FirstFollowTable table) {
        boolean changed;

        // Initialize FIRST sets for terminals: FIRST(a) = {a}
        for (Symbol terminal : grammar.getTerminals()) {
            table.addFirst(terminal, terminal);
        }
        table.addFirst(Symbol.EPSILON, Symbol.EPSILON);
        table.addFirst(Symbol.EOF, Symbol.EOF);

        // Iteratively compute FIRST sets for non-terminals
        do {
            changed = false;

            for (Production production : grammar.getProductions()) {
                Symbol lhs = production.getLhs();
                List<Symbol> rhs = production.getRhs();

                Set<Symbol> currentFirstLhs = table.getFirst(lhs);
                int originalSize = currentFirstLhs.size();

                if (production.isEpsilonProduction()) {
                    table.addFirst(lhs, Symbol.EPSILON);
                } else {
                    boolean allNullable = true;

                    for (Symbol rhsSymbol : rhs) {
                        Set<Symbol> firstOfRhsSymbol = table.getFirst(rhsSymbol);

                        // Add everything except EPSILON
                        for (Symbol s : firstOfRhsSymbol) {
                            if (!s.equals(Symbol.EPSILON)) {
                                table.addFirst(lhs, s);
                            }
                        }

                        // If this symbol doesn't derive EPSILON, stop checking the sequence
                        if (!firstOfRhsSymbol.contains(Symbol.EPSILON)) {
                            allNullable = false;
                            break;
                        }
                    }

                    // If all symbols in RHS can derive EPSILON, then LHS can derive EPSILON
                    if (allNullable) {
                        table.addFirst(lhs, Symbol.EPSILON);
                    }
                }

                if (table.getFirst(lhs).size() > originalSize) {
                    changed = true;
                }
            }
        } while (changed);
    }

    private static void computeFollowSets(Grammar grammar, FirstFollowTable table) {
        boolean changed;

        // Rule 1: Place EOF in FOLLOW(StartSymbol)
        table.addFollow(grammar.getStartSymbol(), Symbol.EOF);

        // Iteratively compute FOLLOW sets
        do {
            changed = false;

            for (Production production : grammar.getProductions()) {
                Symbol lhs = production.getLhs();
                List<Symbol> rhs = production.getRhs();

                for (int i = 0; i < rhs.size(); i++) {
                    Symbol currentSymbol = rhs.get(i);

                    if (currentSymbol.isTerminal()) {
                        continue; // FOLLOW sets are only for Non-Terminals
                    }

                    Set<Symbol> currentFollow = table.getFollow(currentSymbol);
                    int originalSize = currentFollow.size();

                    boolean nextDerivesEpsilon = true;

                    // Rule 2: If A -> α B β, then everything in FIRST(β) except EPSILON is in
                    // FOLLOW(B)
                    for (int j = i + 1; j < rhs.size(); j++) {
                        Symbol nextSymbol = rhs.get(j);
                        Set<Symbol> firstOfNext = table.getFirst(nextSymbol);

                        for (Symbol s : firstOfNext) {
                            if (!s.equals(Symbol.EPSILON)) {
                                table.addFollow(currentSymbol, s);
                            }
                        }

                        if (!firstOfNext.contains(Symbol.EPSILON)) {
                            nextDerivesEpsilon = false;
                            break;
                        }
                    }

                    // Rule 3: If A -> α B, or A -> α B β where FIRST(β) contains EPSILON,
                    // then everything in FOLLOW(A) is in FOLLOW(B)
                    if (nextDerivesEpsilon) {
                        Set<Symbol> followOfLhs = table.getFollow(lhs);
                        for (Symbol s : followOfLhs) {
                            table.addFollow(currentSymbol, s);
                        }
                    }

                    if (table.getFollow(currentSymbol).size() > originalSize) {
                        changed = true;
                    }
                }
            }
        } while (changed);
    }
}
