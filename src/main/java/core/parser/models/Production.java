package core.parser.models;

import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

/**
 * Represents a context‑free production rule: LHS → RHS. The left‑hand side must be a non‑terminal.
 * The right‑hand side is a sequence of terminals and non‑terminals.
 *
 * @author Generated
 * @version 1.0
 */
public class Production {
    @Getter private final Symbol lhs; // Left-Hand Side (must be a non‑terminal)
    private final List<Symbol> rhs; // Right-Hand Side

    /**
     * Constructs a production.
     *
     * @param lhs the left‑hand side non‑terminal
     * @param rhs the right‑hand side sequence of symbols (may be empty or contain EPSILON)
     * @throws IllegalArgumentException if lhs is a terminal
     */
    public Production(Symbol lhs, List<Symbol> rhs) {
        if (lhs.isTerminal()) {
            throw new IllegalArgumentException("LHS of a production must be a Non-Terminal.");
        }
        this.lhs = lhs;
        this.rhs = new ArrayList<>(rhs);
    }

    /**
     * Returns an unmodifiable list of the right‑hand side symbols.
     *
     * @return the RHS list
     */
    public List<Symbol> getRhs() {
        return Collections.unmodifiableList(rhs);
    }

    /**
     * Checks whether this production is an epsilon production (LHS → ε).
     *
     * @return true if the RHS consists of exactly the EPSILON symbol
     */
    public boolean isEpsilonProduction() {
        return rhs.size() == 1 && rhs.get(0).equals(Symbol.EPSILON);
    }

    /**
     * Returns a string representation of the production.
     *
     * @return the production in the form "LHS -> RHS"
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lhs.getName()).append(" -> ");
        if (rhs.isEmpty()) {
            sb.append(Symbol.EPSILON.getName());
        } else {
            for (Symbol s : rhs) {
                sb.append(s.getName()).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
