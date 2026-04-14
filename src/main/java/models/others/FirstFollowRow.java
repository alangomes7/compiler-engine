package models.others;

import java.util.ArrayList;
import java.util.List;

import models.atomic.Symbol;

public class FirstFollowRow {
    private final Symbol nonTerminal;
    private final List<Symbol> firstSet;
    private final List<Symbol> followSet;

    // Constructor for when you already have the lists built
    public FirstFollowRow(Symbol nonTerminal, List<Symbol> firstSet, List<Symbol> followSet) {
        this.nonTerminal = nonTerminal;
        this.firstSet = firstSet;
        this.followSet = followSet;
    }

    // Convenience constructor that initializes empty lists
    public FirstFollowRow(Symbol nonTerminal) {
        this.nonTerminal = nonTerminal;
        this.firstSet = new ArrayList<>();
        this.followSet = new ArrayList<>();
    }

    public Symbol getNonTerminal() { return nonTerminal; }
    public List<Symbol> getFirstSet() { return firstSet; }
    public List<Symbol> getFollowSet() { return followSet; }
    
    // Helper methods to add to the sets easily
    public void addFirst(Symbol symbol) {
        if (!firstSet.contains(symbol)) {
            firstSet.add(symbol);
        }
    }

    public void addFollow(Symbol symbol) {
        if (!followSet.contains(symbol)) {
            followSet.add(symbol);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "%-15s | FIRST: %-25s | FOLLOW: %-25s",
            nonTerminal.toString(),
            firstSet.toString(),
            followSet.toString()
        );
    }

    /**
     * Prints the row directly to the console.
     */
    public void print() {
        System.out.println(this.toString());
    }
}