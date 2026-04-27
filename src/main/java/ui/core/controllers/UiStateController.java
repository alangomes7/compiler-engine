package ui.core.controllers;

import ui.Ui;
import ui.core.state.AnalysisState;

/**
 * Controls the UI state (enabled/disabled buttons, visibility, etc.) based on the current analysis
 * state. Handles invalidation of analysis data when inputs change.
 *
 * <p>This controller is responsible for maintaining consistent UI behavior throughout the
 * application's workflow. It ensures that buttons and controls are properly enabled or disabled
 * based on what data is currently available, and invalidates analysis results when source data
 * changes.
 *
 * <p>The controller follows these key principles:
 *
 * <ul>
 *   <li>No analysis operation can be performed without the required prerequisite data
 *   <li>When source data (grammar or input) changes, dependent analysis results are automatically
 *       invalidated
 *   <li>UI controls reflect the current state of analysis data at all times
 * </ul>
 *
 * <p>Typical usage:
 *
 * <pre>
 * UiStateController stateController = new UiStateController(ui, analysisState);
 *
 * // After loading new grammar
 * stateController.invalidateAnalysisState();
 *
 * // After grammar validation
 * stateController.updateUIState();
 *
 * // After input text changes
 * stateController.invalidateInputState();
 * </pre>
 *
 * @see AnalysisState
 * @see Ui
 */
public class UiStateController {
    private final Ui ui;
    private final AnalysisState state;

    /**
     * Constructs a UI state controller with references to the main UI and shared state.
     *
     * @param ui the main UI instance containing references to all UI controls
     * @param state the shared analysis state that tracks what data is currently available
     * @throws NullPointerException if either parameter is null
     */
    public UiStateController(Ui ui, AnalysisState state) {
        this.ui = ui;
        this.state = state;
    }

    /**
     * Invalidates all analysis data and clears the corresponding UI components.
     *
     * <p>This method should be called when the grammar changes or when analysis results become
     * stale. It clears:
     *
     * <ul>
     *   <li>Parse table data and visualization
     *   <li>FIRST/FOLLOW table
     *   <li>Parse results
     *   <li>Grammar tree visualization
     *   <li>Validation output
     * </ul>
     *
     * <p>After invalidation, all corresponding state flags are set to {@code false} and dependent
     * UI components are cleared. The input state is also invalidated since input analysis depends
     * on grammar analysis.
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

    /**
     * Invalidates data that depends on the input text.
     *
     * <p>This method should be called when the input text changes. It clears:
     *
     * <ul>
     *   <li>Symbol table data and visualization
     *   <li>Input parse tree visualization
     *   <li>Lexer and parser run status flags
     * </ul>
     *
     * <p>After invalidation, all input-dependent state flags are set to {@code false} and the UI is
     * updated to reflect that input analysis needs to be re-executed.
     */
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

    /**
     * Updates the enabled/disabled state of all UI buttons based on the current analysis state.
     *
     * <p>This method evaluates all state flags and enables or disables UI controls accordingly. The
     * enablement logic follows these rules:
     *
     * <ul>
     *   <li>No operations are allowed until a token file is loaded
     *   <li>Grammar operations are only available after grammar is loaded
     *   <li>Parser operations require successful lexer execution
     *   <li>Export operations are only enabled when the corresponding data exists
     *   <li>Clear operations are enabled when there is data to clear
     * </ul>
     *
     * <p>This method should be called after any state change that might affect UI control
     * availability, such as:
     *
     * <ul>
     *   <li>Loading or clearing tokens
     *   <li>Loading or clearing grammar
     *   <li>Running lexer or parser
     *   <li>Generating trees or tables
     * </ul>
     */
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
        ui.getClearGrammarTreeBtn().setDisable(!state.isHasGrammarTree());
        ui.getExportInputTreeBtn().setDisable(!state.isHasInputTree());
        ui.getClearInputTreeBtn().setDisable(!state.isHasInputTree());
        ui.getClearValidationBtn().setDisable(!state.isHasValidationData());

        ui.getClearConsoleLogBtn().setDisable(false);
        ui.getClearOutputBtn().setDisable(false);
        ui.getSaveConsoleLogBtn().setDisable(false);
        ui.getSaveOutputBtn().setDisable(false);
        ui.getSaveValidationBtn().setDisable(false);
    }

    /**
     * Disables all UI controls except the "Load Token File" button.
     *
     * <p>This method is called when no token file is loaded, which is the foundational prerequisite
     * for all other operations. All analysis and export operations are disabled until a token file
     * is loaded.
     *
     * <p>The only enabled control is the token loading button, allowing the user to load tokens and
     * proceed with the application workflow.
     */
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
        ui.getClearGrammarTreeBtn().setDisable(true);
        ui.getExportInputTreeBtn().setDisable(true);
        ui.getClearInputTreeBtn().setDisable(true);
        ui.getSaveConsoleLogBtn().setDisable(true);
        ui.getClearConsoleLogBtn().setDisable(true);
        ui.getSaveOutputBtn().setDisable(true);
        ui.getClearOutputBtn().setDisable(true);
        ui.getSaveValidationBtn().setDisable(true);
        ui.getClearValidationBtn().setDisable(!state.isHasValidationData());
        ui.getInputArea().setDisable(true);
    }
}
