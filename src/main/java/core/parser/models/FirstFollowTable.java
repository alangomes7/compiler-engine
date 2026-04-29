package core.parser.models;

import core.parser.models.atomic.Symbol;
import java.util.Map;
import java.util.Set;

public class FirstFollowTable {

    private final Map<Symbol, Set<Symbol>> firstSets;
    private final Map<Symbol, Set<Symbol>> followSets;

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
