package core.parser.grammar;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import core.parser.models.FirstFollowRow;
import core.parser.models.FirstFollowTable;
import core.parser.models.Grammar;
import core.parser.models.atomic.Symbol;
import models.atomic.Constants;

public class FirstFollowCalculator {

    private final Grammar grammar;
    private final FirstFollowTable resultsTable;
    // Map terminals to their own FIRST sets (containing only themselves) for recursion logic
    private final Map<Symbol, Set<Symbol>> terminalFirstSets = new java.util.HashMap<>();

    public FirstFollowCalculator(Grammar grammar) {
        this.grammar = grammar;
        this.resultsTable = new FirstFollowTable();
        initializeSets();
    }

    public FirstFollowTable getResultsTable() {
        return resultsTable;
    }

    private void initializeSets() {
        // Iterate over Symbols directly!
        for (Symbol ntSymbol : grammar.getNonTerminals()) {
            resultsTable.initializeRow(ntSymbol);
        }

        // Identify and initialize terminals
        for (List<List<Symbol>> rhsList : grammar.getRules().values()) {
            for (List<Symbol> rhs : rhsList) {
                for (Symbol currentSymbol : rhs) {
                    if (!grammar.getNonTerminals().contains(currentSymbol) && !currentSymbol.getLexeme().equals(Constants.EPSILON)) {
                        grammar.getTerminals().add(currentSymbol);
                        // Make sure to mark it as a terminal
                        currentSymbol.setTokenType("TERMINAL"); 
                        terminalFirstSets.computeIfAbsent(currentSymbol, k -> new HashSet<>(Collections.singleton(currentSymbol)));
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
            
            // Loop over Symbols instead of Strings
            for (Symbol lhsSymbol : grammar.getNonTerminals()) {
                FirstFollowRow row = resultsTable.getRow(lhsSymbol);

                for (List<Symbol> rhs : grammar.getRules().get(lhsSymbol)) {
                    boolean allEpsilon = true;
                    
                    for (Symbol rhsSymbol : rhs) {
                        Set<Symbol> symbolFirst = getFirstOfSymbol(rhsSymbol);
                        
                        for (Symbol f : symbolFirst) {
                            if (!f.getLexeme().equals(Constants.EPSILON)) {
                                int sizeBefore = row.getFirstSet().size();
                                row.addFirst(f);
                                if (row.getFirstSet().size() > sizeBefore) changed = true;
                            }
                        }

                        if (!containsEpsilon(symbolFirst)) {
                            allEpsilon = false;
                            break;
                        }
                    }

                    if (allEpsilon) {
                        int sizeBefore = row.getFirstSet().size();
                        // Epsilon synthesized, line 0, col 0
                        row.addFirst(new Symbol(Constants.EPSILON, "SPECIAL", 0, 0));
                        if (row.getFirstSet().size() > sizeBefore) changed = true;
                    }
                }
            }
        }
    }

    private void computeFollowSets() {
        // Rule 1: Place $ in Follow(Start)
        Symbol startSymbol = grammar.getStartSymbol();
        if (startSymbol != null && resultsTable.getRow(startSymbol) != null) {
            resultsTable.getRow(startSymbol).addFollow(new Symbol(Constants.EOF, "SPECIAL", 0, 0));
        }

        boolean changed = true;
        while (changed) {
            changed = false;

            for (Symbol lhsSymbol : grammar.getNonTerminals()) {
                List<Symbol> lhsFollow = resultsTable.getRow(lhsSymbol).getFollowSet();

                for (List<Symbol> rhs : grammar.getRules().get(lhsSymbol)) {
                    for (int i = 0; i < rhs.size(); i++) {
                        Symbol currentSymbol = rhs.get(i);
                        
                        if (!grammar.getNonTerminals().contains(currentSymbol)) continue;

                        FirstFollowRow currentRow = resultsTable.getRow(currentSymbol);
                        if (currentRow == null) continue; // Safety check

                        boolean nextDerivesEpsilon = true;

                        // Rule 2: If A -> α B β, then everything in FIRST(β) except ε is in FOLLOW(B)
                        for (int j = i + 1; j < rhs.size(); j++) {
                            Set<Symbol> nextFirst = getFirstOfSymbol(rhs.get(j));
                            
                            for (Symbol f : nextFirst) {
                                if (!f.getLexeme().equals(Constants.EPSILON)) {
                                    int sizeBefore = currentRow.getFollowSet().size();
                                    currentRow.addFollow(f);
                                    if (currentRow.getFollowSet().size() > sizeBefore) changed = true;
                                }
                            }
                            
                            if (!containsEpsilon(nextFirst)) {
                                nextDerivesEpsilon = false;
                                break;
                            }
                        }

                        // Rule 3: If A -> α B, or A -> α B β where FIRST(β) contains ε, add FOLLOW(A) to FOLLOW(B)
                        if (nextDerivesEpsilon) {
                            for (Symbol f : lhsFollow) {
                                int sizeBefore = currentRow.getFollowSet().size();
                                currentRow.addFollow(f);
                                if (currentRow.getFollowSet().size() > sizeBefore) changed = true;
                            }
                        }
                    }
                }
            }
        }
    }

    // Changed signature to take a Symbol instead of a String
    private Set<Symbol> getFirstOfSymbol(Symbol symbol) {
        if (grammar.getNonTerminals().contains(symbol)) {
            FirstFollowRow row = resultsTable.getRow(symbol);
            return (row != null) ? new HashSet<>(row.getFirstSet()) : new HashSet<>();
        } else if (symbol.getLexeme().equals(Constants.EPSILON)) {
            return new HashSet<>(Collections.singleton(symbol)); // return the symbol instance representing EPSILON
        } else {
            return terminalFirstSets.getOrDefault(symbol, new HashSet<>());
        }
    }

    private boolean containsEpsilon(Set<Symbol> set) {
        return set.stream().anyMatch(s -> s.getLexeme().equals(Constants.EPSILON));
    }

    public Map<Symbol, FirstFollowRow> getFirstFollowRows() {
        return resultsTable.getTable();
    }

    public void printTables() {
        resultsTable.printTable();
    }
}