package core.parser.models;

import core.parser.models.atomic.Symbol;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FirstFollowTable {
    private final Map<Symbol, Set<Symbol>> firstSets;
    private final Map<Symbol, Set<Symbol>> followSets;

    public FirstFollowTable() {
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
    }

    public void addFirst(Symbol nonTerminal, Symbol terminal) {
        firstSets.computeIfAbsent(nonTerminal, k -> new HashSet<>()).add(terminal);
    }

    public void addFollow(Symbol nonTerminal, Symbol terminal) {
        followSets.computeIfAbsent(nonTerminal, k -> new HashSet<>()).add(terminal);
    }

    public Set<Symbol> getFirst(Symbol symbol) {
        return firstSets.getOrDefault(symbol, new HashSet<>());
    }

    public Set<Symbol> getFollow(Symbol nonTerminal) {
        return followSets.getOrDefault(nonTerminal, new HashSet<>());
    }

    public Map<Symbol, Set<Symbol>> getAllFirstSets() {
        return firstSets;
    }

    public Map<Symbol, Set<Symbol>> getAllFollowSets() {
        return followSets;
    }

    public void printSets() {
        System.out.println("=== First Sets ===");
        firstSets.forEach((k, v) -> System.out.println("FIRST(" + k + ") = " + v));

        System.out.println("\n=== Follow Sets ===");
        followSets.forEach((k, v) -> System.out.println("FOLLOW(" + k + ") = " + v));
    }
}
