package core.lexer.models.automata;

import core.lexer.models.atomic.Symbol;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an alphabet for a finite automaton. An alphabet is a set of symbols (characters or
 * tokens) that can appear on transitions.
 *
 * @author Generated
 * @version 1.0
 */
public class Alphabet {
    private final Set<Symbol> symbols;

    /** Constructs an empty alphabet. */
    public Alphabet() {
        this.symbols = new HashSet<>();
    }

    /**
     * Constructs an alphabet initialised with a given set of symbols.
     *
     * @param symbols the initial set of symbols (may be empty)
     */
    public Alphabet(Set<Symbol> symbols) {
        this.symbols = symbols;
    }

    /**
     * Adds a symbol to the alphabet.
     *
     * @param symbol the symbol to add
     */
    public void addSymbol(Symbol symbol) {
        this.symbols.add(symbol);
    }

    /**
     * Removes a symbol from the alphabet if present.
     *
     * @param symbol the symbol to remove
     */
    public void removeSymbol(Symbol symbol) {
        this.symbols.remove(symbol);
    }

    /**
     * Returns the set of symbols currently in the alphabet.
     *
     * @return an unmodifiable view. Actually returns the internal set – caller should not modify.
     *     Consider returning a copy in production code.
     */
    public Set<Symbol> getSymbols() {
        return Collections.unmodifiableSet(symbols);
    }
}
