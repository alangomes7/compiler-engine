package core.parser.models;

import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

public class Production {
    @Getter private final Symbol lhs;
    private final List<Symbol> rhs;

    public Production(Symbol lhs, List<Symbol> rhs) {
        if (lhs.isTerminal()) {
            throw new IllegalArgumentException("LHS of a production must be a Non-Terminal.");
        }
        this.lhs = lhs;
        this.rhs = new ArrayList<>(rhs);
    }

    public List<Symbol> getRhs() {
        return Collections.unmodifiableList(rhs);
    }

    public boolean isEpsilonProduction() {
        return rhs.size() == 1 && rhs.get(0).equals(Symbol.EPSILON);
    }

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
