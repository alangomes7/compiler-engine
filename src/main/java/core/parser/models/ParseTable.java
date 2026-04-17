package core.parser.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.parser.models.atomic.Symbol;

public class ParseTable {
    // 2D Map representing M[NonTerminal, Terminal] = List of Productions
    private final Map<Symbol, Map<Symbol, List<Production>>> table;

    public ParseTable() {
        this.table = new HashMap<>();
    }

    /**
     * Adds a production to the parsing table. 
     * Appends to a list to allow for conflict tracking.
     * * @param nonTerminal The non-terminal (row)
     * @param terminal    The lookahead terminal (column)
     * @param production  The production to apply
     * @throws IllegalArgumentException if EPSILON is used as a column or if a terminal is used as a row.
     */
    public void addEntry(Symbol nonTerminal, Symbol terminal, Production production) {
        if (nonTerminal.isTerminal()) {
            throw new IllegalArgumentException("Row must be a Non-Terminal.");
        }
        if (terminal.equals(Symbol.EPSILON)) {
            throw new IllegalArgumentException("EPSILON cannot be a column in the parse table.");
        }

        table.computeIfAbsent(nonTerminal, k -> new HashMap<>())
             .computeIfAbsent(terminal, k -> new ArrayList<>())
             .add(production);
    }

    /**
     * Retrieves the list of productions to apply given a non-terminal and a lookahead terminal.
     * Returns an empty list if no entry exists (Syntax Error).
     */
    public List<Production> getEntry(Symbol nonTerminal, Symbol terminal) {
        Map<Symbol, List<Production>> row = table.get(nonTerminal);
        if (row != null) {
            return row.getOrDefault(terminal, new ArrayList<>());
        }
        return new ArrayList<>(); 
    }

    public Map<Symbol, Map<Symbol, List<Production>>> getTable() {
        return table;
    }

    /**
     * Utility method to print the Parse Table for debugging purposes.
     * Highlights cells that contain grammar conflicts.
     */
    public void printTable() {
        System.out.println("=== Parsing Table ===");
        for (Map.Entry<Symbol, Map<Symbol, List<Production>>> rowEntry : table.entrySet()) {
            Symbol nonTerminal = rowEntry.getKey();
            
            for (Map.Entry<Symbol, List<Production>> colEntry : rowEntry.getValue().entrySet()) {
                Symbol terminal = colEntry.getKey();
                List<Production> productions = colEntry.getValue();
                
                if (productions.size() > 1) {
                    System.out.printf("M[%s, %s] = %s  <-- CONFLICT!%n", 
                            nonTerminal.getName(), terminal.getName(), productions.toString());
                } else if (!productions.isEmpty()) {
                    System.out.printf("M[%s, %s] = %s%n", 
                            nonTerminal.getName(), terminal.getName(), productions.get(0).toString());
                }
            }
        }
    }
}