package core.parser.models;

import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a context‑free grammar (CFG) consisting of a start symbol, sets of terminals and
 * non‑terminals, and a list of productions.
 *
 * <p>The grammar is built incrementally by adding productions. Terminals and non‑terminals are
 * automatically collected from the productions.
 *
 * @author Generated
 * @version 1.0
 */
public class Grammar {
    private final Symbol startSymbol;
    private final Set<Symbol> terminals;
    private final Set<Symbol> nonTerminals;
    private final List<Production> productions;

    /**
     * Constructs a grammar with a given start symbol. The start symbol is automatically added to
     * the set of non‑terminals.
     *
     * @param startSymbol the start symbol of the grammar (must be a non‑terminal)
     */
    public Grammar(Symbol startSymbol) {
        this.startSymbol = startSymbol;
        this.terminals = new HashSet<>();
        this.nonTerminals = new HashSet<>();
        this.productions = new ArrayList<>();

        // The start symbol is always a non‑terminal
        this.nonTerminals.add(startSymbol);
    }

    /**
     * Adds a production to the grammar. The LHS non‑terminal and all symbols appearing in the RHS
     * are added to the respective non‑terminal/terminal sets. EPSILON is not added to terminals.
     *
     * @param production the production to add
     */
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

    /**
     * Returns the grammar's start symbol.
     *
     * @return the start symbol
     */
    public Symbol getStartSymbol() {
        return startSymbol;
    }

    /**
     * Returns an unmodifiable set of terminals (excluding EPSILON).
     *
     * @return the set of terminals
     */
    public Set<Symbol> getTerminals() {
        return Collections.unmodifiableSet(terminals);
    }

    /**
     * Returns an unmodifiable set of non‑terminals.
     *
     * @return the set of non‑terminals
     */
    public Set<Symbol> getNonTerminals() {
        return Collections.unmodifiableSet(nonTerminals);
    }

    /**
     * Returns an unmodifiable list of all productions in the grammar.
     *
     * @return the productions list
     */
    public List<Production> getProductions() {
        return Collections.unmodifiableList(productions);
    }

    /**
     * Returns all productions for a given non‑terminal.
     *
     * @param nonTerminal the non‑terminal (LHS)
     * @return a list of productions (may be empty)
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
