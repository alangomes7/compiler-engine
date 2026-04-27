package core.parser.models.atomic;

import models.atomic.Constants;

/**
 * Represents a grammar symbol, which may be either a terminal or a non‑terminal. Provides
 * predefined constants for epsilon (ε) and end‑of‑file (EOF).
 *
 * @author Generated
 * @version 1.0
 */
public class Symbol {

    /** The epsilon symbol (empty string), always a terminal. */
    public static final Symbol EPSILON = new Symbol(Constants.EPSILON, true);

    /** The end‑of‑input symbol, always a terminal. */
    public static final Symbol EOF = new Symbol(Constants.EOF, true);

    private final String name;
    private final boolean isTerminal;

    /**
     * Constructs a grammar symbol.
     *
     * @param name the symbol's name (e.g., "expr", "if", "id")
     * @param isTerminal {@code true} if the symbol is a terminal, {@code false} for non‑terminal
     */
    public Symbol(String name, boolean isTerminal) {
        this.name = name;
        this.isTerminal = isTerminal;
    }

    /**
     * Returns the name of the symbol.
     *
     * @return the symbol name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether this symbol is a terminal.
     *
     * @return true if terminal, false otherwise
     */
    public boolean isTerminal() {
        return isTerminal;
    }

    /**
     * Compares two symbols for equality based on name and terminal flag.
     *
     * @param o the other object
     * @return true if the symbols are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        // Compare based on the name of the symbol (e.g., "awk-program" or "print")
        return name != null ? name.equals(symbol.name) : symbol.name == null;
    }

    /**
     * Returns a hash code based on the symbol's name and terminal flag.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    /**
     * Returns a string representation of the symbol (its name).
     *
     * @return the symbol name
     */
    @Override
    public String toString() {
        return name;
    }
}
