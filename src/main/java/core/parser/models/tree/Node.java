package core.parser.models.tree;

import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
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

    @Override
    public String toString() {
        return buildTreeString("", true);
    }

    private String buildTreeString(String prefix, boolean isTail) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix)
                .append(isTail ? "└── " : "├── ")
                .append(symbol.getName())
                .append(lexeme != null ? " (\"" + lexeme + "\")" : "")
                .append(System.lineSeparator());

        for (int i = 0; i < children.size(); i++) {
            sb.append(
                    children.get(i)
                            .buildTreeString(
                                    prefix + (isTail ? "    " : "│   "), i == children.size() - 1));
        }
        return sb.toString();
    }

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
