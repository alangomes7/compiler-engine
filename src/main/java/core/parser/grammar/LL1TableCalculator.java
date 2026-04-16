package core.parser.grammar;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import core.parser.models.FirstFollowRow;
import core.parser.models.FirstFollowTable;
import core.parser.models.Grammar;
import core.parser.models.atomic.Symbol;
import models.atomic.Constants;
import models.tables.LL1Table;

public class LL1TableCalculator {

    private final Grammar grammar;
    private final FirstFollowTable ffTable;
    private final LL1Table ll1Table;

    public LL1TableCalculator(Grammar grammar, FirstFollowTable ffTable) {
        this.grammar = grammar;
        this.ffTable = ffTable;
        this.ll1Table = new LL1Table();
    }

    public LL1Table getLL1Table() {
        return ll1Table;
    }

    public void computeTable() {
        // Iterate through all non-terminals in the grammar
        for (Symbol lhs : grammar.getNonTerminals()) {
            List<List<Symbol>> rules = grammar.getRules().get(lhs);
            if (rules == null) continue;

            FirstFollowRow lhsFFRow = ffTable.getRow(lhs);
            if (lhsFFRow == null) continue;

            // Iterate through every production A -> α
            for (List<Symbol> rhs : rules) {
                Set<Symbol> firstOfRhs = getFirstOfSequence(rhs);

                // Rule 1: For each terminal 'a' in FIRST(α), add A -> α to M[A, a]
                for (Symbol terminal : firstOfRhs) {
                    if (!terminal.getLexeme().equals(Constants.EPSILON)) {
                        ll1Table.addRule(lhs, terminal, rhs);
                    }
                }

                // Rule 2 & 3: If ε is in FIRST(α), add A -> α to M[A, b] for each 'b' in FOLLOW(A)
                if (containsEpsilon(firstOfRhs)) {
                    for (Symbol followTerminal : lhsFFRow.getFollowSet()) {
                        ll1Table.addRule(lhs, followTerminal, rhs);
                    }
                }
            }
        }
    }

    /**
     * Computes the FIRST set of a sequence of symbols (α = X1 X2 ... Xn).
     */
    private Set<Symbol> getFirstOfSequence(List<Symbol> rhs) {
        Set<Symbol> sequenceFirst = new HashSet<>();
        boolean allDeriveEpsilon = true;

        for (Symbol sym : rhs) {
            Set<Symbol> symFirst = getFirstOfSymbol(sym);
            
            // Add everything from FIRST(X) to FIRST(α) except ε
            for (Symbol f : symFirst) {
                if (!f.getLexeme().equals(Constants.EPSILON)) {
                    sequenceFirst.add(f);
                }
            }

            // If this symbol does not derive epsilon, we stop evaluating the sequence
            if (!containsEpsilon(symFirst)) {
                allDeriveEpsilon = false;
                break;
            }
        }

        // If all symbols in the sequence derive epsilon (or the sequence is empty/epsilon), 
        // add epsilon to the FIRST set of the sequence
        if (allDeriveEpsilon) {
            sequenceFirst.add(new Symbol(Constants.EPSILON, "SPECIAL", 0, 0));
        }

        return sequenceFirst;
    }

    /**
     * Resolves the FIRST set for a single symbol.
     */
    private Set<Symbol> getFirstOfSymbol(Symbol symbol) {
        Set<Symbol> first = new HashSet<>();
        if (grammar.getNonTerminals().contains(symbol)) {
            FirstFollowRow row = ffTable.getRow(symbol);
            if (row != null) {
                first.addAll(row.getFirstSet());
            }
        } else {
            // It's a terminal or EPSILON/EOF
            first.add(symbol);
        }
        return first;
    }

    private boolean containsEpsilon(Set<Symbol> set) {
        return set.stream().anyMatch(s -> s.getLexeme().equals(Constants.EPSILON));
    }
}