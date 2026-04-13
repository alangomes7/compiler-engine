package models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Grammar {
    private String startSymbol;
    private Map<String, List<List<String>>> rules = new LinkedHashMap<>();
    private Set<String> nonTerminals = new LinkedHashSet<>();
    private Set<String> terminals = new LinkedHashSet<>();

    public void addRule(String lhs, List<String> rhs) {
        nonTerminals.add(lhs);
        if (startSymbol == null) {
            startSymbol = lhs; // First rule is the start symbol
        }
        rules.computeIfAbsent(lhs, k -> new ArrayList<>()).add(rhs);
    }

    // --- Getters and Setters ---

    public String getStartSymbol() {
        return startSymbol;
    }

    public void setStartSymbol(String startSymbol) {
        this.startSymbol = startSymbol;
    }

    public Map<String, List<List<String>>> getRules() {
        return rules;
    }

    public void setRules(Map<String, List<List<String>>> rules) {
        this.rules = rules;
    }

    public Set<String> getNonTerminals() {
        return nonTerminals;
    }

    public void setNonTerminals(Set<String> nonTerminals) {
        this.nonTerminals = nonTerminals;
    }

    public Set<String> getTerminals() {
        return terminals;
    }

    public void setTerminals(Set<String> terminals) {
        this.terminals = terminals;
    }
}