package core.parser.models.atomic;

import java.util.Objects;

import models.atomic.Constants;

public class Symbol {
    public static final Symbol EPSILON = new Symbol(Constants.EPSILON, true);
    public static final Symbol EOF = new Symbol(Constants.EOF, true);

    private final String name;
    private final boolean isTerminal;

    public Symbol(String name, boolean isTerminal) {
        this.name = name;
        this.isTerminal = isTerminal;
    }

    public String getName() {
        return name;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return isTerminal == symbol.isTerminal && Objects.equals(name, symbol.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, isTerminal);
    }

    @Override
    public String toString() {
        return name;
    }
}