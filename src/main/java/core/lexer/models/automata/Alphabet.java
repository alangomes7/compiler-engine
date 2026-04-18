package core.lexer.models.automata;

import core.lexer.models.atomic.Symbol;
import java.util.HashSet;
import java.util.Set;

public class Alphabet {
    private final Set<Symbol> symbols;

    public Alphabet() {
        this.symbols = new HashSet<>();
    }

    public Alphabet(Set<Symbol> symbols) {
        this.symbols = symbols;
    }

    public void addSymbol(Symbol symbol) {
        this.symbols.add(symbol);
    }

    public void removeSymbol(Symbol symbol) {
        this.symbols.remove(symbol);
    }

    public Set<Symbol> getSymbols() {
        return symbols;
    }
}
