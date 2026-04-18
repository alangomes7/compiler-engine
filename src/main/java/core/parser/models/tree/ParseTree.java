package core.parser.models.tree;

/**
 * Represents the final result of the Syntactic Analysis. Wraps the root Node of the generated tree.
 */
public class ParseTree {
    private final Node root;

    public ParseTree(Node root) {
        this.root = root;
    }

    public Node getRoot() {
        return root;
    }

    /** Prints the entire tree structure starting from the root to the standard output. */
    public void print() {
        if (root != null) {
            root.print("", true);
        } else {
            System.out.println("Empty Parse Tree");
        }
    }
}
