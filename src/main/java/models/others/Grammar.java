package models.others;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.atomic.Symbol;

public class Grammar {
    // Change all String generics and types to Symbol
    private Symbol startSymbol;
    private Map<Symbol, List<List<Symbol>>> rules = new LinkedHashMap<>();
    private Set<Symbol> nonTerminals = new LinkedHashSet<>();
    private Set<Symbol> terminals = new LinkedHashSet<>();

    public void addRule(Symbol lhs, List<Symbol> rhs) {
        nonTerminals.add(lhs);
        if (startSymbol == null) {
            startSymbol = lhs; // First rule is the start symbol
        }
        rules.computeIfAbsent(lhs, k -> new ArrayList<>()).add(rhs);
    }

    // --- Getters and Setters ---

    public Symbol getStartSymbol() {
        return startSymbol;
    }

    public void setStartSymbol(Symbol startSymbol) {
        this.startSymbol = startSymbol;
    }

    public Map<Symbol, List<List<Symbol>>> getRules() {
        return rules;
    }

    public void setRules(Map<Symbol, List<List<Symbol>>> rules) {
        this.rules = rules;
    }

    public Set<Symbol> getNonTerminals() {
        return nonTerminals;
    }

    public void setNonTerminals(Set<Symbol> nonTerminals) {
        this.nonTerminals = nonTerminals;
    }

    public Set<Symbol> getTerminals() {
        return terminals;
    }

    public void setTerminals(Set<Symbol> terminals) {
        this.terminals = terminals;
    }
}