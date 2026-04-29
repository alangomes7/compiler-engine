package core.parser.core;

import core.parser.models.FirstFollowTable;
import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParserTableBuilder {

    public static ParseTable build(Grammar grammar, FirstFollowTable firstFollowTable) {
        ParseTable parseTable = new ParseTable();

        for (Production production : grammar.getProductions()) {
            Symbol lhs = production.getLhs();
            List<Symbol> rhs = production.getRhs();

            Set<Symbol> firstRhs = computeFirstOfSequence(rhs, firstFollowTable);

            for (Symbol symbol : firstRhs) {
                if (!symbol.equals(Symbol.EPSILON)) {
                    parseTable.addEntry(lhs, symbol, production);
                }
            }

            if (firstRhs.contains(Symbol.EPSILON)) {
                Set<Symbol> followLhs =
                        firstFollowTable.getFollowSets().getOrDefault(lhs, Set.of());
                for (Symbol symbol : followLhs) {
                    parseTable.addEntry(lhs, symbol, production);
                }
            }
        }

        return parseTable;
    }

    private static Set<Symbol> computeFirstOfSequence(
            List<Symbol> sequence, FirstFollowTable table) {
        Set<Symbol> result = new HashSet<>();
        boolean allNullable = true;

        for (Symbol s : sequence) {
            if (s.isTerminal()) {
                result.add(s);
                allNullable = false;
                break;
            }

            Set<Symbol> firstS = table.getFirstSets().get(s);

            if (firstS == null) {
                throw new IllegalStateException("FIRST set completely missing for symbol: " + s);
            }

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
