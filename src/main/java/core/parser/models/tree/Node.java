package core.parser.models.tree;

import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.List;

public class Node {
    private final Symbol symbol;
    private String lexeme;
    private final List<Node> children;

    public Node(Symbol symbol) {
        this.symbol = symbol;
        this.children = new ArrayList<>();
    }

    public void addChild(Node child) {
        this.children.add(child);
    }

    public void setLexeme(String lexeme) {
        this.lexeme = lexeme;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public String getLexeme() {
        return lexeme;
    }

    public List<Node> getChildren() {
        return children;
    }

    /**
     * Helper to print the tree in a visual format for debugging. It uses the name of the Symbol and
     * the lexeme if available.
     */
    public void print(String prefix, boolean isTail) {
        System.out.println(
                prefix
                        + (isTail ? "└── " : "├── ")
                        + symbol.getName()
                        + (lexeme != null ? " (\"" + lexeme + "\")" : ""));
        for (int i = 0; i < children.size(); i++) {
            children.get(i).print(prefix + (isTail ? "    " : "│   "), i == children.size() - 1);
        }
    }
}
