package ui.core.handlers;

import ui.Ui;
import ui.core.controllers.UiStateController;
import ui.core.graph.tree.InteractiveTreeView;
import ui.core.state.AnalysisState;
import ui.util.UiUtils;

public class VisualizationHandler {
    private final Ui ui;
    private final AnalysisState state;
    private final UiStateController stateController;

    public VisualizationHandler(Ui ui, AnalysisState state, UiStateController stateController) {
        this.ui = ui;
        this.state = state;
        this.stateController = stateController;
    }

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
                                setOutputAndLog("Grammar tree generated successfully.");
                            } else {
                                setOutputAndLog("Failed to generate grammar tree.");
                            }
                            stateController.updateUIState();
                        },
                        err ->
                                ui.getOutputArea()
                                        .setText(
                                                "Error generating grammar tree: "
                                                        + err.getMessage()));
    }

    public void handleGenerateInputTree() {
        if (state.getCurrentParseResult() == null || state.getCurrentParseResult().tree == null) {
            setOutputAndLog("No parse tree available. Run syntax analysis first.");
            return;
        }

        boolean hasErrors = !state.getCurrentParseResult().errors.isEmpty();
        boolean isDeveloper = "Developer".equals(ui.getUserModeComboBox().getValue());

        if (hasErrors && !isDeveloper) {
            ui.getOutputArea()
                    .setText(
                            "Partial derivation tree (due to errors) is only available in Developer mode.");
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
                            setOutputAndLog("Input tree generated successfully.");
                            stateController.updateUIState();
                        },
                        err ->
                                ui.getOutputArea()
                                        .setText(
                                                "Error generating input tree: "
                                                        + err.getMessage()));
    }

    public void handleClearGrammarTree() {
        ui.getGrammarTreeContainer().setCenter(null);
        state.setHasGrammarTree(false);
        setOutputAndLog("Grammar tree cleared.");
        stateController.updateUIState();
    }

    public void handleClearInputTree() {
        ui.getInputTreeContainer().setCenter(null);
        state.setHasInputTree(false);
        setOutputAndLog("Input tree cleared.");
        stateController.updateUIState();
    }

    public void setOutputAndLog(String text) {
        ui.getOutputArea().setText(text);

        String time = UiUtils.getDisplayTimestamp();
        ui.getConsoleArea().appendText("[" + time + "] [OUTPUT] " + text + "\n");
        ui.getConsoleArea().positionCaret(ui.getConsoleArea().getLength());
    }
}
