package core.parser.models;

import core.parser.models.atomic.Symbol;
import java.util.Map;
import java.util.Set;

/** Data model to hold the computed FIRST and FOLLOW sets for a grammar. */
public class FirstFollowTable {

    private final Map<Symbol, Set<Symbol>> firstSets;
    private final Map<Symbol, Set<Symbol>> followSets;

    /**
     * Constructs the table with the fully computed sets. * @param firstSets Map of Symbols to their
     * FIRST sets
     *
     * @param followSets Map of Symbols to their FOLLOW sets
     */
    public FirstFollowTable(
            Map<Symbol, Set<Symbol>> firstSets, Map<Symbol, Set<Symbol>> followSets) {
        this.firstSets = firstSets;
        this.followSets = followSets;
    }

    public Map<Symbol, Set<Symbol>> getFirstSets() {
        return firstSets;
    }

    public Map<Symbol, Set<Symbol>> getFollowSets() {
        return followSets;
    }
}
