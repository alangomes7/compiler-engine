package core.parser.models.tree;

public class ParseTree {
    private final Node root;

    public ParseTree(Node root) {
        this.root = root;
    }

    public Node getRoot() {
        return root;
    }

    @Override
    public String toString() {
        if (root != null) {
            return root.toString();
        } else {
            return "Empty Parse Tree";
        }
    }

    public void print() {
        if (root != null) {
            root.print("", true);
        } else {
            System.out.println("Empty Parse Tree");
        }
    }
}
