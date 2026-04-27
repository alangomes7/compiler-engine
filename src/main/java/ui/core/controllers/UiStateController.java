package ui.core.controllers;

import ui.Ui;
import ui.core.state.AnalysisState;

/**
 * Controls the UI state (enabled/disabled buttons, visibility, etc.) based on the current analysis
 * state. Handles invalidation of analysis data when inputs change.
 *
 * @author Generated
 * @version 1.0
 */
public class UiStateController {
    private final Ui ui;
    private final AnalysisState state;

    /**
     * Constructs a UI state controller.
     *
     * @param ui the main UI instance
     * @param state the shared analysis state
     */
    public UiStateController(Ui ui, AnalysisState state) {
        this.ui = ui;
        this.state = state;
    }

    /**
     * Invalidates all analysis data (FIRST/FOLLOW, parse table, parse results, etc.) and clears the
     * corresponding UI components.
     */
    public void invalidateAnalysisState() {
        ui.getParserTableManager().clear();
        state.setCurrentParseTable(null);
        ui.getFirstFollowTable().getItems().clear();
        state.setCurrentFirstFollowTable(null);
        state.setCurrentParseResult(null);
        ui.getGrammarTreeContainer().setCenter(null);

        state.setHasFirstFollowData(false);
        state.setHasParseTableData(false);
        state.setHasGrammarTree(false);
        state.setHasValidationData(false);

        ui.getValidatorOutputArea().clear();
        invalidateInputState();
    }

    /** Invalidates data that depends on the input text (symbol table, parse tree, etc.). */
    public void invalidateInputState() {
        ui.getSymbolTableViewer().getItems().clear();
        ui.getInputTreeContainer().setCenter(null);

        state.setHasSymbolTableData(false);
        state.setLexerRunSuccess(false);

        // Explicitly lock the Syntax Tree buttons when input changes
        state.setParseRunSuccess(false);
        state.setHasInputTree(false);

        updateUIState();
    }

    /** Updates the enabled/disabled state of all UI buttons based on the current analysis state. */
    public void updateUIState() {
        if (!state.isTokenLoaded()) {
            disableAllExceptTokenLoad();
            return;
        }

        ui.getLoadGrammarBtn().setDisable(false);
        ui.getLoadInputBtn().setDisable(false);
        ui.getRunLexerBtn().setDisable(false);
        ui.getInputArea().setDisable(false);

        if (!state.isGrammarLoaded()) {
            ui.getValidateCompatibilityBtn().setDisable(true);
            ui.getGenerateGrammarTreeBtn().setDisable(true);
            ui.getRunSyntaxBtn().setDisable(true);
            ui.getGenerateInputTreeBtn().setDisable(true);
        } else {
            ui.getValidateCompatibilityBtn().setDisable(false);
            ui.getGenerateGrammarTreeBtn().setDisable(false);
            ui.getGenerateInputTreeBtn().setDisable(false);
            ui.getRunSyntaxBtn().setDisable(!state.isLexerRunSuccess());
        }

        boolean hasAutomaton = state.getCurrentAutomaton() != null;
        ui.getExportGraphImageBtn().setDisable(!hasAutomaton);
        ui.getExportGraphTextBtn().setDisable(!hasAutomaton);

        boolean hasAnyTableData =
                state.isHasSymbolTableData()
                        || state.isHasFirstFollowData()
                        || state.isHasParseTableData();
        ui.getExportCsvBtn().setDisable(!hasAnyTableData);
        ui.getClearTablesBtn()
                .setDisable(
                        !hasAnyTableData && !state.isHasGrammarTree() && !state.isHasInputTree());

        ui.getExportGrammarTreeBtn().setDisable(!state.isHasGrammarTree());
        ui.getExportInputTreeBtn().setDisable(!state.isHasInputTree());
        ui.getClearValidationBtn().setDisable(!state.isHasValidationData());

        ui.getClearConsoleLogBtn().setDisable(false);
        ui.getClearOutputBtn().setDisable(false);
        ui.getSaveConsoleLogBtn().setDisable(false);
        ui.getSaveOutputBtn().setDisable(false);
        ui.getSaveValidationBtn().setDisable(false);
    }

    /** Disables all UI controls except the "Load Token File" button. */
    private void disableAllExceptTokenLoad() {
        ui.getLoadTokenBtn().setDisable(false);
        ui.getLoadGrammarBtn().setDisable(true);
        ui.getLoadInputBtn().setDisable(true);
        ui.getRunLexerBtn().setDisable(true);
        ui.getRunSyntaxBtn().setDisable(true);
        ui.getValidateCompatibilityBtn().setDisable(true);
        ui.getGenerateGrammarTreeBtn().setDisable(true);
        ui.getGenerateInputTreeBtn().setDisable(true);
        ui.getExportGraphImageBtn().setDisable(true);
        ui.getExportGraphTextBtn().setDisable(true);
        ui.getExportCsvBtn().setDisable(true);
        ui.getClearTablesBtn().setDisable(true);
        ui.getExportGrammarTreeBtn().setDisable(true);
        ui.getExportInputTreeBtn().setDisable(true);
        ui.getSaveConsoleLogBtn().setDisable(true);
        ui.getClearConsoleLogBtn().setDisable(true);
        ui.getSaveOutputBtn().setDisable(true);
        ui.getClearOutputBtn().setDisable(true);
        ui.getSaveValidationBtn().setDisable(true);
        ui.getClearValidationBtn().setDisable(!state.isHasValidationData());
        ui.getInputArea().setDisable(true);
    }
}
