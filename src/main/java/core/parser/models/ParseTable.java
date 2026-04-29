package core.parser.models;

import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseTable {
    private final Map<Symbol, Map<Symbol, List<Production>>> table;

    public ParseTable() {
        this.table = new HashMap<>();
    }

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

        if (!cell.contains(production)) {
            cell.add(production);
        }
    }

    public List<Production> getEntry(Symbol nonTerminal, Symbol terminal) {
        Map<Symbol, List<Production>> row = table.get(nonTerminal);
        if (row != null) {
            return row.getOrDefault(terminal, new ArrayList<>());
        }
        return new ArrayList<>();
    }

    public Map<Symbol, Map<Symbol, List<Production>>> getTable() {
        return Collections.unmodifiableMap(table);
    }

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
