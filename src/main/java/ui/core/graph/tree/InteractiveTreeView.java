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

public class InteractiveTreeView extends Pane {

    private double dragContextX;
    private double dragContextY;
    private final Group contentGroup;

    private static final double NODE_WIDTH = 80;
    private static final double NODE_HEIGHT = 40;
    private static final double LEVEL_GAP = 80;
    private static final double SIBLING_GAP = 20;

    public InteractiveTreeView(Node root) {
        contentGroup = new Group();
        this.getChildren().add(contentGroup);

        setupZoomAndPan();

        if (root != null) {
            drawTree(root, 400, 50, calculateSubtreeWidth(root));
        }
    }

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

    // CHANGED: Now returns the generated StackPane so the parent can bind to it
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

                // CHANGED: Draw connecting line dynamically using bindings
                Line line = new Line();
                line.setStroke(Color.GRAY);
                line.setStrokeWidth(1.5);

                // Bind line start to the bottom-center of the parent node
                line.startXProperty().bind(nodeUI.layoutXProperty().add(NODE_WIDTH / 2));
                line.startYProperty().bind(nodeUI.layoutYProperty().add(NODE_HEIGHT));

                // Bind line end to the top-center of the child node
                line.endXProperty().bind(childUI.layoutXProperty().add(NODE_WIDTH / 2));
                line.endYProperty().bind(childUI.layoutYProperty());

                contentGroup.getChildren().add(0, line); // Add line behind nodes

                currentX += childWidth + SIBLING_GAP;
            }
        }

        return nodeUI;
    }

    private StackPane createNodeUI(Node node) {
        StackPane stack = new StackPane();

        Rectangle rect = new Rectangle(NODE_WIDTH, NODE_HEIGHT);
        rect.setArcWidth(10);
        rect.setArcHeight(10);

        boolean isTerminal = node.getSymbol().isTerminal();
        rect.setFill(isTerminal ? Color.LIGHTGREEN : Color.LIGHTBLUE);
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
        Tooltip.install(
                stack,
                new Tooltip(
                        isTerminal
                                ? "Terminal: " + node.getSymbol().getName()
                                : "Non-Terminal: " + node.getSymbol().getName()));

        // CHANGED: Add drag functionality for individual nodes
        final double[] dragDelta = new double[2]; // Using array to hold mutable state inside lambda

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
