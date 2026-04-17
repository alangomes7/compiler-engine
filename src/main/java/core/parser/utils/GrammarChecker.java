package core.parser.utils;

import java.util.List;
import java.util.Map;

import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;

public class GrammarChecker {

    /**
     * Checks if the given ParseTable represents a strict LL(1) grammar.
     * A grammar is LL(1) if and only if its parsing table has no multiple-defined entries (conflicts).
     *
     * @param parseTable The generated parse table to evaluate.
     * @return true if the grammar is LL(1), false if there are conflicts.
     */
    public static boolean isLL1(ParseTable parseTable) {
        if (parseTable == null || parseTable.getTable() == null) {
            return false;
        }

        Map<Symbol, Map<Symbol, List<Production>>> table = parseTable.getTable();

        for (Map<Symbol, List<Production>> row : table.values()) {
            for (List<Production> cell : row.values()) {
                if (cell.size() > 1) {
                    return false; // Conflict found, not LL(1)
                }
            }
        }
        
        return true; // No conflicts found
    }
}