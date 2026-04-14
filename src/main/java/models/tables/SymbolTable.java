package models.tables;

import java.util.HashMap;
import java.util.Map;

import models.atomic.Symbol;

public class SymbolTable {
    private final Map<String, Symbol> table;

    public SymbolTable() {
        this.table = new HashMap<>();
    }

    /**
     * Inserts a new symbol into the table if it doesn't already exist.
     * Returns the existing or newly created symbol.
     */
    public Symbol insert(String lexeme, String tokenType) {
        if (!table.containsKey(lexeme)) {
            Symbol symbol = new Symbol(lexeme, tokenType);
            table.put(lexeme, symbol);
            return symbol;
        }
        return table.get(lexeme);
    }

    /**
     * Retrieves a symbol from the table by its lexeme.
     * Returns null if not found.
     */
    public Symbol lookup(String lexeme) {
        return table.get(lexeme);
    }

    /**
     * Clears all symbols from the table.
     */
    public void clearTable() {
        this.table.clear();
        System.out.println("\n=== Symbol Table: cleaned ===");
        printTable();
    }

    public void printTable() {
        System.out.println("\n=== Symbol Table ===");
        if (table.isEmpty()) {
            System.out.println(" (Empty) ");
        } else {
            for (Symbol sym : table.values()) {
                System.out.println(" " + sym);
            }
        }
        System.out.println("====================\n");
    }
    
    // Provide access to the underlying map if the Parser needs to iterate over it
    public Map<String, Symbol> getTable() {
        return table;
    }
}