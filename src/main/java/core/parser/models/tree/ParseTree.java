package core.parser.models.tree;

/**
 * Represents the final result of syntactic analysis. Wraps the root node of the generated parse
 * tree.
 *
 * @author Generated
 * @version 1.0
 */
public class ParseTree {
    private final Node root;

    /**
     * Constructs a parse tree with the given root node.
     *
     * @param root the root node of the tree
     */
    public ParseTree(Node root) {
        this.root = root;
    }

    /**
     * Returns the root node of the parse tree.
     *
     * @return the root node
     */
    public Node getRoot() {
        return root;
    }

    /**
     * Returns a string representation of the entire parse tree.
     * If the root is {@code null}, returns "Empty Parse Tree".
     *
     * @return the string representation of the parse tree
     */
    @Override
    public String toString() {
        if (root != null) {
            return root.toString();
        } else {
            return "Empty Parse Tree";
        }
    }

    /**
     * Prints the entire tree structure starting from the root to standard output. If the root is
     * {@code null}, prints "Empty Parse Tree".
     */
    public void print() {
        if (root != null) {
            root.print("", true);
        } else {
            System.out.println("Empty Parse Tree");
        }
    }
}
