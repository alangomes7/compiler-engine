package core.parser.core;

import java.util.List;
import java.util.Set;

import core.parser.models.FirstFollowTable;
import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;

/**
 * Reads the grammar and the FirstFollowTable and creates a populated Parser Table object model.
 */
public class ParserTableBuilder {

    /**
     * Builds the LL(1) Parsing Table using the Grammar and its First/Follow sets.
     */
    public static ParseTable build(Grammar grammar, FirstFollowTable firstFollowTable) {
        ParseTable parseTable = new ParseTable();

        for (Production production : grammar.getProductions()) {
            Symbol lhs = production.getLhs();
            List<Symbol> rhs = production.getRhs();

            // 1. For each terminal 'a' in FIRST(RHS), add M[A, a] = production
            Set<Symbol> firstRhs = computeFirstOfSequence(rhs, firstFollowTable);

            for (Symbol symbol : firstRhs) {
                if (!symbol.equals(Symbol.EPSILON)) {
                    parseTable.addEntry(lhs, symbol, production);
                }
            }

            // 2. If EPSILON is in FIRST(RHS), for each terminal 'b' in FOLLOW(LHS), add M[A, b] = production
            if (firstRhs.contains(Symbol.EPSILON)) {
                Set<Symbol> followLhs = firstFollowTable.getFollow(lhs);
                for (Symbol symbol : followLhs) {
                    parseTable.addEntry(lhs, symbol, production);
                }
            }
        }

        return parseTable;
    }

    /**
     * Helper to calculate the FIRST set of a sequence of symbols (the RHS of a production).
     */
    private static Set<Symbol> computeFirstOfSequence(List<Symbol> sequence, FirstFollowTable table) {
        java.util.Set<Symbol> result = new java.util.HashSet<>();
        boolean allNullable = true;

        for (Symbol s : sequence) {
            Set<Symbol> firstS = table.getFirst(s);
            for (Symbol f : firstS) {
                if (!f.equals(Symbol.EPSILON)) {
                    result.add(f);
                }
            }
            if (!firstS.contains(Symbol.EPSILON)) {
                allNullable = false;
                break;
            }
        }

        if (allNullable) {
            result.add(Symbol.EPSILON);
        }

        return result;
    }
}