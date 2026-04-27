package core.parser.models.tree;

import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * Represents a node in a parse tree (or abstract syntax tree). Each node corresponds to a grammar
 * symbol (terminal or non‑terminal). Terminal nodes may also store a lexeme (the actual text from
 * the source).
 *
 * @author Generated
 * @version 1.0
 */
@Getter
public class Node {
    private final Symbol symbol;
    private String lexeme;
    private final List<Node> children;

    /**
     * Constructs a new tree node for the given grammar symbol.
     *
     * @param symbol the grammar symbol (terminal or non‑terminal) associated with this node
     */
    public Node(Symbol symbol) {
        this.symbol = symbol;
        this.children = new ArrayList<>();
    }

    /**
     * Adds a child node to this node.
     *
     * @param child the child node to add
     */
    public void addChild(Node child) {
        this.children.add(child);
    }

    /**
     * Sets the lexeme (source text) for this node. Typically set only for terminal nodes.
     *
     * @param lexeme the actual source text matched for this node
     */
    public void setLexeme(String lexeme) {
        this.lexeme = lexeme;
    }

    /**
     * Prints the tree rooted at this node in a visual, tree‑like format. Each node is shown with
     * its symbol name; terminal nodes also show the lexeme in quotes. Used primarily for debugging.
     *
     * @param prefix the indentation prefix (built recursively)
     * @param isTail {@code true} if this node is the last child of its parent
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
