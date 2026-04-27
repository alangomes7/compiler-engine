package ui.core.handlers;

import ui.Ui;
import ui.core.controllers.UiStateController;
import ui.core.graph.tree.InteractiveTreeView;
import ui.core.state.AnalysisState;

/**
 * Handles the generation of visual tree representations: the grammar structure tree and the parse
 * tree of the input.
 *
 * @author Generated
 * @version 1.0
 */
public class VisualizationHandler {
    private final Ui ui;
    private final AnalysisState state;
    private final UiStateController stateController;

    /**
     * Constructs a VisualizationHandler with references to the main UI, analysis state, and state
     * controller.
     *
     * @param ui the main UI instance
     * @param state the shared analysis state
     * @param stateController the controller for updating UI components based on state changes
     */
    public VisualizationHandler(Ui ui, AnalysisState state, UiStateController stateController) {
        this.ui = ui;
        this.state = state;
        this.stateController = stateController;
    }

    /**
     * Generates a tree view of the grammar (productions for each non‑terminal) and displays it in
     * the grammar tree container.
     */
    public void handleGenerateGrammarTree() {
        ui.getTaskExecutor()
                .execute(
                        "Generating Grammar Tree...",
                        log -> ui.getParserService().buildFullGrammarTree(),
                        tree -> {
                            if (tree != null) {
                                ui.getGrammarTreeContainer()
                                        .setCenter(new InteractiveTreeView(tree.getRoot()));
                                state.setHasGrammarTree(true);
                                ui.getOutputArea().setText("Grammar tree generated successfully.");
                            } else {
                                ui.getOutputArea().setText("Failed to generate grammar tree.");
                            }
                            stateController.updateUIState();
                        },
                        err ->
                                ui.getOutputArea()
                                        .setText(
                                                "Error generating grammar tree: "
                                                        + err.getMessage()));
    }

    /**
     * Generates a parse tree from the last successful syntax analysis result and displays it in the
     * input tree container. Does nothing if no parse result is available.
     */
    public void handleGenerateInputTree() {
        if (state.getCurrentParseResult() == null || state.getCurrentParseResult().tree == null) {
            ui.getOutputArea().setText("No parse tree available. Run syntax analysis first.");
            return;
        }

        ui.getTaskExecutor()
                .execute(
                        "Generating Input Tree...",
                        log -> state.getCurrentParseResult().tree,
                        tree -> {
                            ui.getInputTreeContainer()
                                    .setCenter(new InteractiveTreeView(tree.getRoot()));
                            state.setHasInputTree(true);
                            ui.getOutputArea().setText("Input tree generated successfully.");
                            stateController.updateUIState();
                        },
                        err ->
                                ui.getOutputArea()
                                        .setText(
                                                "Error generating input tree: "
                                                        + err.getMessage()));
    }
}
