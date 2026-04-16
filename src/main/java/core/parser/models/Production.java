package core.parser.models;

import java.util.Objects;

import core.parser.models.atomic.Symbol;

public class Production {
    private final Symbol leftHandSide;
    private final Symbol rightHandSide;

    public Production(Symbol leftHandSide, Symbol rightHandSide) {
        // Enforce that the left-hand side of a production MUST be a Non-Terminal
        if (leftHandSide.isTerminal()) {
            throw new IllegalArgumentException("Left-hand side of a production must be a Non-Terminal.");
        }
        
        this.leftHandSide = Objects.requireNonNull(leftHandSide, "Left side cannot be null");
        this.rightHandSide = Objects.requireNonNull(rightHandSide, "Right side cannot be null");
    }

    public Symbol getLeftHandSide() {
        return leftHandSide;
    }

    public Symbol getRightHandSide() {
        return rightHandSide;
    }

    @Override
    public String toString() {
        return leftHandSide.toString() + " -> " + rightHandSide.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Production that = (Production) o;
        return leftHandSide.equals(that.leftHandSide) && 
               rightHandSide.equals(that.rightHandSide);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftHandSide, rightHandSide);
    }
}