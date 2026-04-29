package core.parser.models;

import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Grammar {
    private final Symbol startSymbol;
    private final Set<Symbol> terminals;
    private final Set<Symbol> nonTerminals;
    private final List<Production> productions;

    private static final Set<String> BUILTIN_TERMINALS = new HashSet<>(Set.of());

    public Grammar(Symbol startSymbol) {
        this.startSymbol = startSymbol;
        this.terminals = new HashSet<>();
        this.nonTerminals = new HashSet<>();
        this.productions = new ArrayList<>();

        this.nonTerminals.add(startSymbol);
    }

    public static void addBuiltinTerminal(String terminal) {
        if (terminal != null && !terminal.trim().isEmpty()) {
            BUILTIN_TERMINALS.add(terminal);
        }
    }

    public static void excludeBuiltinTerminal(String terminal) {
        if (terminal != null) {
            BUILTIN_TERMINALS.remove(terminal);
        }
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

    public Symbol getStartSymbol() {
        return startSymbol;
    }

    public Set<Symbol> getTerminals() {
        return Collections.unmodifiableSet(terminals);
    }

    public Set<Symbol> getNonTerminals() {
        return Collections.unmodifiableSet(nonTerminals);
    }

    public List<Production> getProductions() {
        return Collections.unmodifiableList(productions);
    }

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
