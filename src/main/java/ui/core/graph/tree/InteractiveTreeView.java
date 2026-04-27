package ui.core.graph.tree;

import core.parser.models.tree.Node;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Transform;
import models.atomic.Constants;

/**
 * Interactive JavaFX component for displaying a parse tree. Supports zoom, pan, and individual node
 * dragging. The tree is laid out recursively with automatic positioning based on subtree widths.
 */
public class InteractiveTreeView extends Pane {

    private double dragContextX;
    private double dragContextY;
    private final Group contentGroup;

    private static final double NODE_WIDTH = 80;
    private static final double NODE_HEIGHT = 40;
    private static final double LEVEL_GAP = 80;
    private static final double SIBLING_GAP = 20;

    /**
     * Constructs an interactive view for the given parse tree root.
     *
     * @param root the root node of the parse tree (may be null)
     */
    public InteractiveTreeView(Node root) {
        contentGroup = new Group();
        this.getChildren().add(contentGroup);

        setupZoomAndPan();

        if (root != null) {
            drawTree(root, 400, 50, calculateSubtreeWidth(root));
        }
    }

    /**
     * Captures a high‑resolution snapshot of the current tree view. Temporarily resets zoom/pan to
     * identity, scales for 4K output, then restores.
     *
     * @return a WritableImage containing the rendered tree
     */
    public WritableImage generateSnapshot() {
        double oldScaleX = contentGroup.getScaleX();
        double oldScaleY = contentGroup.getScaleY();
        double oldTranslateX = contentGroup.getTranslateX();
        double oldTranslateY = contentGroup.getTranslateY();

        contentGroup.setScaleX(1.0);
        contentGroup.setScaleY(1.0);
        contentGroup.setTranslateX(0.0);
        contentGroup.setTranslateY(0.0);

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.WHITE);

        javafx.geometry.Bounds bounds = contentGroup.getLayoutBounds();
        if (bounds.getWidth() > 0 && bounds.getHeight() > 0) {
            double scaleFactor = 3840.0 / bounds.getWidth();
            double finalScale = Math.max(3.0, scaleFactor);
            parameters.setTransform(Transform.scale(finalScale, finalScale));
        }

        WritableImage image = contentGroup.snapshot(parameters, null);

        contentGroup.setScaleX(oldScaleX);
        contentGroup.setScaleY(oldScaleY);
        contentGroup.setTranslateX(oldTranslateX);
        contentGroup.setTranslateY(oldTranslateY);

        return image;
    }

    /**
     * Recursively calculates the total width required for a node's subtree. Used for horizontal
     * positioning.
     *
     * @param node the root of the subtree
     * @return the width in pixels
     */
    private double calculateSubtreeWidth(Node node) {
        if (node.getChildren().isEmpty()) {
            return NODE_WIDTH;
        }
        double width = 0;
        for (Node child : node.getChildren()) {
            width += calculateSubtreeWidth(child) + SIBLING_GAP;
        }
        return width - SIBLING_GAP;
    }

    /**
     * Recursively draws the tree starting at the given node. Creates the node UI, positions it, and
     * draws connecting lines to children.
     *
     * @param node the current tree node
     * @param x the X coordinate of the node's centre
     * @param y the Y coordinate of the node's top
     * @param subtreeWidth the total width of this node's subtree (used for centering children)
     * @return the StackPane representing the node (for binding lines)
     */
    private StackPane drawTree(Node node, double x, double y, double subtreeWidth) {
        StackPane nodeUI = createNodeUI(node);
        nodeUI.setLayoutX(x - NODE_WIDTH / 2);
        nodeUI.setLayoutY(y);

        contentGroup.getChildren().add(nodeUI);

        if (!node.getChildren().isEmpty()) {
            double currentX = x - subtreeWidth / 2;
            for (Node child : node.getChildren()) {
                double childWidth = calculateSubtreeWidth(child);
                double childCenterX = currentX + childWidth / 2;
                double childY = y + NODE_HEIGHT + LEVEL_GAP;

                // Recursively draw child and get its UI element
                StackPane childUI = drawTree(child, childCenterX, childY, childWidth);

                // Draw connecting line dynamically using bindings
                Line line = new Line();
                line.setStroke(Color.GRAY);
                line.setStrokeWidth(1.5);

                // Bind line start to the bottom-centre of the parent node
                line.startXProperty().bind(nodeUI.layoutXProperty().add(NODE_WIDTH / 2));
                line.startYProperty().bind(nodeUI.layoutYProperty().add(NODE_HEIGHT));

                // Bind line end to the top-centre of the child node
                line.endXProperty().bind(childUI.layoutXProperty().add(NODE_WIDTH / 2));
                line.endYProperty().bind(childUI.layoutYProperty());

                contentGroup.getChildren().add(0, line); // Add line behind nodes

                currentX += childWidth + SIBLING_GAP;
            }
        }

        return nodeUI;
    }

    /**
     * Creates a draggable UI node (rectangle + label) for a parse tree node. Terminal nodes are
     * light green, non‑terminals light blue, and Epsilon nodes are light gray. Includes a tooltip
     * showing symbol type.
     *
     * @param node the parse tree node
     * @return a StackPane that can be placed on the scene
     */
    private StackPane createNodeUI(Node node) {
        StackPane stack = new StackPane();

        Rectangle rect = new Rectangle(NODE_WIDTH, NODE_HEIGHT);
        rect.setArcWidth(10);
        rect.setArcHeight(10);

        boolean isTerminal = node.getSymbol().isTerminal();
        boolean isEpsilon = node.getSymbol().getName().equals(Constants.EPSILON);

        // Color Logic: Gray for Epsilon, Green for Terminals, Blue for Non-Terminals
        if (isEpsilon) {
            rect.setFill(Color.LIGHTGRAY);
        } else if (isTerminal) {
            rect.setFill(Color.LIGHTGREEN);
        } else {
            rect.setFill(Color.LIGHTBLUE);
        }

        rect.setStroke(Color.DARKBLUE);
        rect.setStrokeWidth(2);

        String displayText = node.getSymbol().getName();
        if (node.getLexeme() != null) {
            displayText += "\n\"" + node.getLexeme() + "\"";
        }

        Label label = new Label(displayText);
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setStyle("-fx-text-alignment: center;");

        stack.getChildren().addAll(rect, label);

        String tooltipText;
        if (isEpsilon) {
            tooltipText = "Epsilon Transition";
        } else if (isTerminal) {
            tooltipText = "Terminal: " + node.getSymbol().getName();
        } else {
            tooltipText = "Non-Terminal: " + node.getSymbol().getName();
        }
        Tooltip.install(stack, new Tooltip(tooltipText));

        // Add drag functionality for individual nodes
        final double[] dragDelta = new double[2]; // Array to hold mutable state inside lambda

        stack.setOnMousePressed(
                e -> {
                    dragDelta[0] = stack.getLayoutX() - e.getSceneX();
                    dragDelta[1] = stack.getLayoutY() - e.getSceneY();
                    e.consume(); // Prevents the canvas pan event from triggering
                });

        stack.setOnMouseDragged(
                e -> {
                    stack.setLayoutX(e.getSceneX() + dragDelta[0]);
                    stack.setLayoutY(e.getSceneY() + dragDelta[1]);
                    e.consume();
                });

        return stack;
    }

    /**
     * Configures the pane to support mouse panning (drag on background) and zoom (scroll wheel).
     */
    private void setupZoomAndPan() {
        this.setOnMousePressed(
                event -> {
                    if (event.getTarget() == this) {
                        dragContextX = event.getSceneX() - contentGroup.getTranslateX();
                        dragContextY = event.getSceneY() - contentGroup.getTranslateY();
                    }
                });

        this.setOnMouseDragged(
                event -> {
                    if (event.getTarget() == this) {
                        contentGroup.setTranslateX(event.getSceneX() - dragContextX);
                        contentGroup.setTranslateY(event.getSceneY() - dragContextY);
                    }
                });

        this.setOnScroll(
                (ScrollEvent event) -> {
                    double zoomFactor = 1.05;
                    double deltaY = event.getDeltaY();
                    if (deltaY < 0) {
                        zoomFactor = 1 / zoomFactor;
                    }
                    contentGroup.setScaleX(contentGroup.getScaleX() * zoomFactor);
                    contentGroup.setScaleY(contentGroup.getScaleY() * zoomFactor);
                    event.consume();
                });
    }
}
