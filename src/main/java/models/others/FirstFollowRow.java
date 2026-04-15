package models.others;

import models.atomic.Symbol;
import java.util.ArrayList;
import java.util.List;

public class FirstFollowRow {
    private Symbol nonTerminal;
    private List<Symbol> firstSet;
    private List<Symbol> followSet;

    public FirstFollowRow(Symbol nonTerminal) {
        this.nonTerminal = nonTerminal;
        // Initialize the sets to prevent null pointer exceptions inside the row
        this.firstSet = new ArrayList<>();
        this.followSet = new ArrayList<>();
    }

    // --- Getters and Setters ---
    public Symbol getNonTerminal() { return nonTerminal; }
    public void setNonTerminal(Symbol nonTerminal) { this.nonTerminal = nonTerminal; }

    public List<Symbol> getFirstSet() { return firstSet; }
    public void setFirstSet(List<Symbol> firstSet) { this.firstSet = firstSet; }

    public List<Symbol> getFollowSet() { return followSet; }
    public void setFollowSet(List<Symbol> followSet) { this.followSet = followSet; }

    // --- Helper Methods used by Calculator ---
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
}