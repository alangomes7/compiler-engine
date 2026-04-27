package ui.core.handlers;

import ui.Ui;
import ui.core.controllers.UiStateController;
import ui.core.graph.tree.InteractiveTreeView;
import ui.core.state.AnalysisState;

/**
 * Handles the generation of visual tree representations.
 *
 * <p>This handler is responsible for creating interactive tree visualizations of two types of
 * hierarchical structures:
 *
 * <ul>
 *   <li><b>Grammar Tree:</b> Visual representation of the grammar's production rules, showing how
 *       non-terminals expand to sequences of symbols
 *   <li><b>Input Parse Tree:</b> Concrete parse tree showing how the input text was parsed
 *       according to the grammar rules
 * </ul>
 *
 * <p>Both tree visualizations are interactive, allowing users to:
 *
 * <ul>
 *   <li>Expand and collapse nodes
 *   <li>Pan and zoom the view
 *   <li>Export snapshots via the export handler
 * </ul>
 *
 * <p>Typical usage:
 *
 * <pre>
 * VisualizationHandler visualizer = new VisualizationHandler(ui, state, stateController);
 *
 * // After loading grammar
 * visualizer.handleGenerateGrammarTree();
 *
 * // After successful syntax analysis
 * visualizer.handleGenerateInputTree();
 * </pre>
 *
 * @see Ui
 * @see AnalysisState
 * @see UiStateController
 * @see InteractiveTreeView
 */
public class VisualizationHandler {
    private final Ui ui;
    private final AnalysisState state;
    private final UiStateController stateController;

    /**
     * Constructs a VisualizationHandler with references to the main UI, analysis state, and state
     * controller.
     *
     * @param ui the main UI instance providing access to services and containers
     * @param state the shared analysis state for tracking tree availability
     * @param stateController the controller for updating UI components based on state changes
     */
    public VisualizationHandler(Ui ui, AnalysisState state, UiStateController stateController) {
        this.ui = ui;
        this.state = state;
        this.stateController = stateController;
    }

    /**
     * Generates a tree view of the grammar (productions for each non‑terminal) and displays it.
     *
     * <p>The grammar tree visualizes the structure of the loaded grammar, showing each non-terminal
     * and its production rules. The tree is generated recursively, with:
     *
     * <ul>
     *   <li>Root node: the grammar's start symbol
     *   <li>Production nodes: labeled with "::="
     *   <li>Symbol nodes: terminals and non-terminals in the RHS
     * </ul>
     *
     * <p>This operation runs asynchronously and may take time for large grammars. Progress is shown
     * via the background task executor.
     *
     * <p>Requires that a grammar has been loaded.
     *
     * <p>On successful generation, {@code hasGrammarTree} is set to {@code true} and the tree is
     * displayed in the grammar tree container.
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
     * Generates a parse tree from the last successful syntax analysis result and displays it.
     *
     * <p>The input parse tree shows how the input text was parsed according to the grammar rules.
     * Each node in the tree represents either:
     *
     * <ul>
     *   <li>A non-terminal that is expanded using a production rule
     *   <li>A terminal token that was matched in the input
     * </ul>
     *
     * <p>This operation is quick (just retrieving the cached parse result) and runs asynchronously
     * to keep the UI responsive.
     *
     * <p>Requires that syntax analysis has been run successfully and a parse result is available.
     * If no parse result exists, an error message is displayed in the output area and the operation
     * is aborted.
     *
     * <p>On successful generation, {@code hasInputTree} is set to {@code true} and the tree is
     * displayed in the input tree container.
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

    /**
     * Clears the currently displayed grammar tree visualization.
     *
     * <p>Removes the interactive tree view from its container to free up memory,
     * updates the corresponding state flag ({@code hasGrammarTree} to false), 
     * and refreshes the UI state to disable related export and clear buttons.
     */
    public void handleClearGrammarTree() {
        ui.getGrammarTreeContainer().setCenter(null);
        state.setHasGrammarTree(false);
        ui.getOutputArea().setText("Grammar tree cleared.");
        stateController.updateUIState();
    }

    /**
     * Clears the currently displayed input parse tree visualization.
     *
     * <p>Removes the interactive tree view from its container to free up memory,
     * updates the corresponding state flag ({@code hasInputTree} to false), 
     * and refreshes the UI state to disable related export and clear buttons.
     */
    public void handleClearInputTree() {
        ui.getInputTreeContainer().setCenter(null);
        state.setHasInputTree(false);
        ui.getOutputArea().setText("Input tree cleared.");
        stateController.updateUIState();
    }
}
