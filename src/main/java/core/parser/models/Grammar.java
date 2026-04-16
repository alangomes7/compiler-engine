package core.parser.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import core.parser.models.atomic.Symbol;

public class Grammar {
    private final Symbol startSymbol;
    private final Set<Symbol> terminals;
    private final Set<Symbol> nonTerminals;
    private final List<Production> productions;

    public Grammar(Symbol startSymbol) {
        this.startSymbol = startSymbol;
        this.terminals = new HashSet<>();
        this.nonTerminals = new HashSet<>();
        this.productions = new ArrayList<>();
        
        // The start symbol is always a non-terminal
        this.nonTerminals.add(startSymbol);
    }

    public void addProduction(Production production) {
        this.productions.add(production);
        this.nonTerminals.add(production.getLhs());
        
        for (Symbol symbol : production.getRhs()) {
            if (symbol.isTerminal() && !symbol.equals(Symbol.EPSILON)) {
                this.terminals.add(symbol);
            } else if (!symbol.isTerminal()) {
                this.nonTerminals.add(symbol);
            }
        }
    }

    public Symbol getStartSymbol() { return startSymbol; }
    public Set<Symbol> getTerminals() { return terminals; }
    public Set<Symbol> getNonTerminals() { return nonTerminals; }
    public List<Production> getProductions() { return productions; }

    /**
     * Helper to get all productions for a specific Non-Terminal
     */
    public List<Production> getProductionsFor(Symbol nonTerminal) {
        List<Production> result = new ArrayList<>();
        for (Production p : productions) {
            if (p.getLhs().equals(nonTerminal)) {
                result.add(p);
            }
        }
        return result;
    }
}