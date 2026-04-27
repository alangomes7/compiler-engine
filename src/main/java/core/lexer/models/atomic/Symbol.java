package core.lexer.models.atomic;

import java.util.Objects;
import lombok.Getter;

/**
 * Represents an input symbol used in finite automaton transitions and in the Lexer. A symbol is
 * essentially a wrapper around a String value.
 *
 * @author Generated
 * @version 1.0
 */
@Getter
public class Symbol {
    private final String value;

    /**
     * Constructs a new Symbol with the given string value.
     *
     * @param value the string representation of the symbol (e.g., a character or token name)
     */
    public Symbol(String value) {
        this.value = value;
    }

    /**
     * Compares this symbol to another object based on the stored value.
     *
     * @param o the object to compare
     * @return true if the other object is a Symbol with the same value
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return Objects.equals(value, symbol.value);
    }

    /**
     * Returns a hash code computed from the symbol's value.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Returns the string value of this symbol.
     *
     * @return the symbol's value
     */
    @Override
    public String toString() {
        return value;
    }
}
