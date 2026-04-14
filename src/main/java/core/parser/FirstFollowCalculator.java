package core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import models.atomic.Symbol;
import models.others.Grammar;
import models.tables.FirstFollowTable;
import models.others.FirstFollowRow;

public class FirstFollowCalculator {
    public static final String EPSILON = "ε";
    public static final String EOF = "$";

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
        // Initialize rows for all Non-Terminals in the Symbol-based table
        for (String ntLexeme : grammar.getNonTerminals()) {
            Symbol ntSymbol = new Symbol(ntLexeme, "NON_TERMINAL");
            resultsTable.initializeRow(ntSymbol);
        }

        // Identify and initialize terminals
        for (List<List<String>> rhsList : grammar.getRules().values()) {
            for (List<String> rhs : rhsList) {
                for (String symbolLexeme : rhs) {
                    if (!grammar.getNonTerminals().contains(symbolLexeme) && !symbolLexeme.equals(EPSILON)) {
                        grammar.getTerminals().add(symbolLexeme);
                        
                        Symbol termSymbol = new Symbol(symbolLexeme, "TERMINAL");
                        terminalFirstSets.computeIfAbsent(termSymbol, k -> new HashSet<>(Collections.singleton(termSymbol)));
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
            for (String lhsLexeme : grammar.getNonTerminals()) {
                Symbol lhsSymbol = new Symbol(lhsLexeme, "NON_TERMINAL");
                FirstFollowRow row = resultsTable.getRow(lhsSymbol);

                for (List<String> rhs : grammar.getRules().get(lhsLexeme)) {
                    boolean allEpsilon = true;
                    
                    for (String symbolLexeme : rhs) {
                        Set<Symbol> symbolFirst = getFirstOfSymbol(symbolLexeme);
                        
                        for (Symbol f : symbolFirst) {
                            if (!f.getLexeme().equals(EPSILON)) {
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
                        row.addFirst(new Symbol(EPSILON, "SPECIAL"));
                        if (row.getFirstSet().size() > sizeBefore) changed = true;
                    }
                }
            }
        }
    }

    private void computeFollowSets() {
        // Rule 1: Place $ in Follow(Start)
        Symbol startSymbol = new Symbol(grammar.getStartSymbol(), "NON_TERMINAL");
        resultsTable.getRow(startSymbol).addFollow(new Symbol(EOF, "SPECIAL"));

        boolean changed = true;
        while (changed) {
            changed = false;

            for (String lhsLexeme : grammar.getNonTerminals()) {
                Symbol lhsSymbol = new Symbol(lhsLexeme, "NON_TERMINAL");
                List<Symbol> lhsFollow = resultsTable.getRow(lhsSymbol).getFollowSet();

                for (List<String> rhs : grammar.getRules().get(lhsLexeme)) {
                    for (int i = 0; i < rhs.size(); i++) {
                        String currentLexeme = rhs.get(i);
                        
                        if (!grammar.getNonTerminals().contains(currentLexeme)) continue;

                        Symbol currentSymbol = new Symbol(currentLexeme, "NON_TERMINAL");
                        FirstFollowRow currentRow = resultsTable.getRow(currentSymbol);
                        boolean nextDerivesEpsilon = true;

                        // Rule 2: If A -> α B β, then everything in FIRST(β) except ε is in FOLLOW(B)
                        for (int j = i + 1; j < rhs.size(); j++) {
                            Set<Symbol> nextFirst = getFirstOfSymbol(rhs.get(j));
                            
                            for (Symbol f : nextFirst) {
                                if (!f.getLexeme().equals(EPSILON)) {
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

    private Set<Symbol> getFirstOfSymbol(String lexeme) {
        if (grammar.getNonTerminals().contains(lexeme)) {
            FirstFollowRow row = resultsTable.getRow(new Symbol(lexeme, "NON_TERMINAL"));
            return (row != null) ? new HashSet<>(row.getFirstSet()) : new HashSet<>();
        } else if (lexeme.equals(EPSILON)) {
            return new HashSet<>(Collections.singleton(new Symbol(EPSILON, "SPECIAL")));
        } else {
            return terminalFirstSets.getOrDefault(new Symbol(lexeme, "TERMINAL"), new HashSet<>());
        }
    }

    private boolean containsEpsilon(Set<Symbol> set) {
        return set.stream().anyMatch(s -> s.getLexeme().equals(EPSILON));
    }

    // Add these to FirstFollowCalculator.java
    public Map<Symbol, FirstFollowRow> getFirstFollowRows() {
        return resultsTable.getTable();
    }

    public void printTables() {
        resultsTable.printTable();
    }
}
