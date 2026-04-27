package core.parser.models;

import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LL(1) parsing table: maps a non‑terminal and a lookahead terminal to a list of productions. A
 * list is used to detect conflicts (multiple entries in the same cell).
 *
 * @author Generated
 * @version 1.0
 */
public class ParseTable {
    // 2D Map representing M[NonTerminal, Terminal] = List of Productions
    private final Map<Symbol, Map<Symbol, List<Production>>> table;

    /** Constructs an empty parse table. */
    public ParseTable() {
        this.table = new HashMap<>();
    }

    /**
     * Adds a production to the parsing table. Appends to a list to allow for conflict tracking.
     *
     * @param nonTerminal The non‑terminal (row) – must not be a terminal
     * @param terminal The lookahead terminal (column) – must not be EPSILON
     * @param production The production to apply
     * @throws IllegalArgumentException if the row is not a non‑terminal or the column is EPSILON
     */
    public void addEntry(Symbol nonTerminal, Symbol terminal, Production production) {
        if (nonTerminal.isTerminal()) {
            throw new IllegalArgumentException("Row must be a Non-Terminal.");
        }
        if (terminal.equals(Symbol.EPSILON)) {
            throw new IllegalArgumentException("EPSILON cannot be a column in the parse table.");
        }

        List<Production> cell =
                table.computeIfAbsent(nonTerminal, k -> new HashMap<>())
                        .computeIfAbsent(terminal, k -> new ArrayList<>());

        // Only add the production if it isn't already in the cell
        if (!cell.contains(production)) {
            cell.add(production);
        }
    }

    /**
     * Retrieves the list of productions to apply given a non‑terminal and a lookahead terminal.
     * Returns an empty list if no entry exists (syntax error).
     *
     * @param nonTerminal the row symbol (non‑terminal)
     * @param terminal the column symbol (terminal)
     * @return a list of productions (may be empty; size > 1 indicates conflict)
     */
    public List<Production> getEntry(Symbol nonTerminal, Symbol terminal) {
        Map<Symbol, List<Production>> row = table.get(nonTerminal);
        if (row != null) {
            return row.getOrDefault(terminal, new ArrayList<>());
        }
        return new ArrayList<>();
    }

    /**
     * Returns an unmodifiable view of the entire parse table.
     *
     * @return the underlying table map
     */
    public Map<Symbol, Map<Symbol, List<Production>>> getTable() {
        return Collections.unmodifiableMap(table);
    }

    /**
     * Utility method to print the parse table for debugging. Cells containing multiple productions
     * (conflicts) are marked.
     */
    public void printTable() {
        System.out.println("=== Parsing Table ===");
        for (Map.Entry<Symbol, Map<Symbol, List<Production>>> rowEntry : table.entrySet()) {
            Symbol nonTerminal = rowEntry.getKey();

            for (Map.Entry<Symbol, List<Production>> colEntry : rowEntry.getValue().entrySet()) {
                Symbol terminal = colEntry.getKey();
                List<Production> productions = colEntry.getValue();

                if (productions.size() > 1) {
                    System.out.printf(
                            "M[%s, %s] = %s  <-- CONFLICT!%n",
                            nonTerminal.getName(), terminal.getName(), productions.toString());
                } else if (!productions.isEmpty()) {
                    System.out.printf(
                            "M[%s, %s] = %s%n",
                            nonTerminal.getName(),
                            terminal.getName(),
                            productions.get(0).toString());
                }
            }
        }
    }
}
