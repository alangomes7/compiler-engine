package models.tables;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import models.atomic.Symbol;
import models.others.FirstFollowRow;

import java.util.Collection;

/**
 * Represents the FIRST and FOLLOW sets for all non-terminals in a grammar.
 */
public class FirstFollowTable {

    // Key: Non-Terminal Symbol
    // Value: Row containing the FIRST and FOLLOW sets
    private final Map<Symbol, FirstFollowRow> table;

    public FirstFollowTable() {
        this.table = new HashMap<>();
    }

    /**
     * Initializes a row for a non-terminal if it doesn't already exist.
     */
    public void initializeRow(Symbol nonTerminal) {
        if (nonTerminal == null) return;
        table.putIfAbsent(nonTerminal, new FirstFollowRow(nonTerminal));
    }

    /**
     * Returns the specific row for a non-terminal.
     */
    public FirstFollowRow getRow(Symbol nonTerminal) {
        return table.get(nonTerminal);
    }

    /**
     * Returns an unmodifiable view of the table to maintain encapsulation.
     */
    public Map<Symbol, FirstFollowRow> getTable() {
        return Collections.unmodifiableMap(table);
    }

    /**
     * Clears all entries from the table.
     */
    public void clearTable() {
        this.table.clear();
        System.out.println("\n=== Symbol First Follow Table: cleaned ===");
    }

    /**
     * Formats the table into a human-readable ASCII representation.
     */
    @Override
    public String toString() {
        if (table.isEmpty()) {
            return "\n=== FIRST & FOLLOW TABLE ===\n(Table is empty)\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== FIRST & FOLLOW TABLE ===\n");
        // Adjusted padding for better visibility in console
        sb.append(String.format("%-25s | %-40s | %-40s\n", 
                "Non-Terminal", "FIRST Set", "FOLLOW Set"));
        sb.append("-".repeat(110)).append("\n");

        table.values().forEach(row -> 
            sb.append(String.format("%-25s | %-40s | %-40s\n",
                    row.getNonTerminal().getLexeme(),
                    formatSet(row.getFirstSet()),
                    formatSet(row.getFollowSet())
            ))
        );

        return sb.toString();
    }

    /**
     * Prints the table to standard output.
     */
    public void printTable() {
        System.out.print(this.toString());
    }

    /**
     * Helper to transform a collection of symbols into a clean string: { a, b, c }
     */
    private String formatSet(Collection<Symbol> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return "{ }";
        }
        
        String content = symbols.stream()
                .map(Symbol::getLexeme)
                .distinct() // Ensures no visual duplicates if the Set implementation differs
                .collect(Collectors.joining(", "));
        
        return "{ " + content + " }";
    }
}