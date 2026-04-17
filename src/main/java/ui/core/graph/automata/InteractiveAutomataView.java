package ui.core.graph.automata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.AFD;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurve;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Transform;

public class InteractiveAutomataView extends Pane {

    private double dragContextX;
    private double dragContextY;
    private final Group contentGroup;
    private final Map<State, StackPane> nodeMap = new HashMap<>();

    public InteractiveAutomataView(AFD afd) {
        contentGroup = new Group();
        this.getChildren().add(contentGroup);

        setupZoomAndPan();

        if (afd != null) {
            buildGraph(afd);
        }
    }

    /**
     * Takes a snapshot of the entire interactive graph and returns it as a WritableImage.
     */
    public WritableImage generateSnapshot() {
        // 1. Save the current pan and zoom state
        double oldScaleX = contentGroup.getScaleX();
        double oldScaleY = contentGroup.getScaleY();
        double oldTranslateX = contentGroup.getTranslateX();
        double oldTranslateY = contentGroup.getTranslateY();

        // 2. Reset the view to its default state for a clean export
        contentGroup.setScaleX(1.0);
        contentGroup.setScaleY(1.0);
        contentGroup.setTranslateX(0.0);
        contentGroup.setTranslateY(0.0);

        // 3. Configure the snapshot parameters
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.WHITE); // Solid white background

        // Get the total unclipped bounds of the entire graph
        javafx.geometry.Bounds bounds = contentGroup.getLayoutBounds();

        if (bounds.getWidth() > 0 && bounds.getHeight() > 0) {
            // Ensure at least a 3.0x scale for crispness on large graphs.
            // If the graph is small, it will still scale up to 3840px.
            // If the graph is huge, it will scale by 5.0x, exceeding 4K to preserve detail.
            double scaleFactorTo4K = 3840.0 / bounds.getWidth();
            double finalScale = Math.max(3.0, scaleFactorTo4K); 
            
            parameters.setTransform(Transform.scale(finalScale, finalScale));
        }

        // 4. Take the snapshot of the contentGroup (the whole graph), not the Pane
        WritableImage image = contentGroup.snapshot(parameters, null);

        // 5. Restore the user's previous pan and zoom state
        contentGroup.setScaleX(oldScaleX);
        contentGroup.setScaleY(oldScaleY);
        contentGroup.setTranslateX(oldTranslateX);
        contentGroup.setTranslateY(oldTranslateY);

        return image;
    }

    private void buildGraph(AFD afd) {
        Map<State, Integer> stateLayers = new HashMap<>();
        Map<Integer, List<State>> layerToStates = new HashMap<>();
        Queue<State> queue = new LinkedList<>();

        State startState = afd.getStartState();
        if (startState != null) {
            queue.add(startState);
            stateLayers.put(startState, 0);
            layerToStates.computeIfAbsent(0, k -> new ArrayList<>()).add(startState);
        }

        while (!queue.isEmpty()) {
            State current = queue.poll();
            int currentLayer = stateLayers.get(current);

            for (Transition t : current.getTransitions()) {
                State target = t.getTarget();
                if (!stateLayers.containsKey(target)) {
                    stateLayers.put(target, currentLayer + 1);
                    layerToStates.computeIfAbsent(currentLayer + 1, k -> new ArrayList<>()).add(target);
                    queue.add(target);
                }
            }
        }

        double startX = 100;
        double startY = 300;
        double layerSpacingX = 220; 
        double nodeSpacingY = 120;  

        Set<State> allStates = stateLayers.keySet();

        for (Map.Entry<Integer, List<State>> entry : layerToStates.entrySet()) {
            int layer = entry.getKey();
            List<State> statesInLayer = entry.getValue();
            
            int numNodes = statesInLayer.size();
            double currentY = startY - ((numNodes - 1) * nodeSpacingY) / 2.0;

            for (State state : statesInLayer) {
                boolean isStart = state.equals(afd.getStartState());
                boolean isFinal = state.isFinal() || (afd.getFinalStates() != null && afd.getFinalStates().contains(state));
                
                StackPane nodeUI = createNodeUI(state, isStart, isFinal);
                
                nodeUI.setLayoutX(startX + (layer * layerSpacingX));
                nodeUI.setLayoutY(currentY);
                currentY += nodeSpacingY; 
                
                nodeMap.put(state, nodeUI);
                contentGroup.getChildren().add(nodeUI);
            }
        }

        for (State source : allStates) {
            Map<State, List<String>> groupedTransitions = new HashMap<>();
            
            for (Transition t : source.getTransitions()) {
                String symbol = t.getSymbol() == null || t.getSymbol().getValue().isEmpty() ? "ε" : t.getSymbol().getValue();
                groupedTransitions.computeIfAbsent(t.getTarget(), k -> new ArrayList<>()).add(symbol);
            }

            for (Map.Entry<State, List<String>> entry : groupedTransitions.entrySet()) {
                State target = entry.getKey();
                String labelStr = source.getId() + " -> " + target.getId() + ", {" + summarizeTokens(entry.getValue()) + "}";
                
                createEdgeUI(source, target, labelStr);
            }
        }

        for (StackPane node : nodeMap.values()) {
            node.toFront();
        }
    }

    private StackPane createNodeUI(State state, boolean isStart, boolean isFinal) {
        StackPane stack = new StackPane();
        
        Circle circle = new Circle(25); 
        circle.setFill(isStart ? Color.LIGHTGREEN : Color.LIGHTBLUE);
        circle.setStroke(isFinal ? Color.RED : Color.DARKBLUE);
        circle.setStrokeWidth(isFinal ? 4 : 2);

        String stateName = "q" + String.valueOf(state.getId());
        String labelText;

        if (isStart && isFinal) {
            labelText = "start_final: " + stateName;
        } else if (isStart) {
            labelText = "start: " + stateName;
        } else if (isFinal) {
            labelText = "final: " + stateName;
        } else {
            labelText = stateName;
        }

        Label label = new Label(labelText);
        label.setFont(Font.font("System", FontWeight.BOLD, 12));

        stack.getChildren().addAll(circle, label);

        stack.setOnMousePressed(e -> {
            dragContextX = stack.getLayoutX() - e.getSceneX();
            dragContextY = stack.getLayoutY() - e.getSceneY();
            e.consume();
        });

        stack.setOnMouseDragged(e -> {
            stack.setLayoutX(e.getSceneX() + dragContextX);
            stack.setLayoutY(e.getSceneY() + dragContextY);
            e.consume();
        });

        Tooltip.install(stack, new Tooltip("State: " + labelText));

        return stack;
    }

    private void createEdgeUI(State source, State target, String text) {
        StackPane sourceNode = nodeMap.get(source);
        StackPane targetNode = nodeMap.get(target);

        Label label = new Label(text);
        label.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-padding: 3px; -fx-border-color: gray; -fx-border-radius: 3px;");

        if (source.equals(target)) {
            QuadCurve curve = new QuadCurve();
            curve.setFill(Color.TRANSPARENT);
            curve.setStroke(Color.GRAY);
            curve.setStrokeWidth(1.5);

            curve.startXProperty().bind(sourceNode.layoutXProperty().add(15));
            curve.startYProperty().bind(sourceNode.layoutYProperty());
            curve.controlXProperty().bind(sourceNode.layoutXProperty().add(60));
            curve.controlYProperty().bind(sourceNode.layoutYProperty().subtract(70));
            curve.endXProperty().bind(sourceNode.layoutXProperty().add(35));
            curve.endYProperty().bind(sourceNode.layoutYProperty().add(10));

            // Arrow for self-loop
            Polygon arrow = new Polygon(0, 0, -10, 5, -10, -5);
            arrow.setFill(Color.GRAY);
            arrow.layoutXProperty().bind(curve.endXProperty());
            arrow.layoutYProperty().bind(curve.endYProperty());
            arrow.setRotate(107); // Fixed angle pointing down and slightly left along the curve's end trajectory

            label.layoutXProperty().bind(sourceNode.layoutXProperty().add(20));
            label.layoutYProperty().bind(sourceNode.layoutYProperty().subtract(50));

            contentGroup.getChildren().addAll(curve, arrow, label);

        } else {
            Line line = new Line();
            line.setStroke(Color.GRAY);
            line.setStrokeWidth(1.5);

            line.startXProperty().bind(sourceNode.layoutXProperty().add(25));
            line.startYProperty().bind(sourceNode.layoutYProperty().add(25));
            line.endXProperty().bind(targetNode.layoutXProperty().add(25));
            line.endYProperty().bind(targetNode.layoutYProperty().add(25));

            // Calculate arrow bindings dynamically
            DoubleBinding dx = line.endXProperty().subtract(line.startXProperty());
            DoubleBinding dy = line.endYProperty().subtract(line.startYProperty());
            DoubleBinding length = Bindings.createDoubleBinding(() -> 
                Math.sqrt(dx.get() * dx.get() + dy.get() * dy.get()), dx, dy);

            // Position arrow just outside the target node border (radius is ~25, we use 27 for padding)
            DoubleBinding arrowX = Bindings.createDoubleBinding(() -> {
                if (length.get() == 0) return line.getEndX();
                return line.getStartX() + (dx.get() / length.get()) * (length.get() - 27);
            }, line.startXProperty(), line.endXProperty(), dx, length);

            DoubleBinding arrowY = Bindings.createDoubleBinding(() -> {
                if (length.get() == 0) return line.getEndY();
                return line.getStartY() + (dy.get() / length.get()) * (length.get() - 27);
            }, line.startYProperty(), line.endYProperty(), dy, length);

            DoubleBinding angle = Bindings.createDoubleBinding(() -> 
                Math.toDegrees(Math.atan2(dy.get(), dx.get())), dx, dy);

            // Arrow shape
            Polygon arrow = new Polygon(0, 0, -10, 5, -10, -5);
            arrow.setFill(Color.GRAY);
            arrow.layoutXProperty().bind(arrowX);
            arrow.layoutYProperty().bind(arrowY);
            arrow.rotateProperty().bind(angle);

            label.layoutXProperty().bind(line.startXProperty().add(line.endXProperty()).divide(2).subtract(20));
            label.layoutYProperty().bind(line.startYProperty().add(line.endYProperty()).divide(2).subtract(15));

            contentGroup.getChildren().addAll(line, arrow, label);
        }
    }

    private void setupZoomAndPan() {
        this.setOnMousePressed(event -> {
            if (event.getTarget() == this) {
                dragContextX = event.getSceneX() - contentGroup.getTranslateX();
                dragContextY = event.getSceneY() - contentGroup.getTranslateY();
            }
        });

        this.setOnMouseDragged(event -> {
            if (event.getTarget() == this) {
                contentGroup.setTranslateX(event.getSceneX() - dragContextX);
                contentGroup.setTranslateY(event.getSceneY() - dragContextY);
            }
        });

        this.setOnScroll((ScrollEvent event) -> {
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

    private String summarizeTokens(List<String> symbols) {
        return String.join(":", symbols.toString().replaceAll("\\[|\\]", ""));
    }
}