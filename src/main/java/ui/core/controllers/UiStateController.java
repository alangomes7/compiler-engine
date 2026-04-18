package ui.core.controllers;

import ui.Ui;
import ui.core.state.AnalysisState;

public class UiStateController {
    private final Ui ui;
    private final AnalysisState state;

    public UiStateController(Ui ui, AnalysisState state) {
        this.ui = ui;
        this.state = state;
    }

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
        state.setHasInputTree(false);
        state.setParseRunSuccess(false);
        state.setHasValidationData(false);

        ui.getValidatorOutputArea().clear();
        invalidateInputState();
        updateUIState();
    }

    public void invalidateInputState() {
        ui.getSymbolTableViewer().getItems().clear();
        ui.getInputTreeContainer().setCenter(null);
        state.setHasSymbolTableData(false);
        state.setLexerRunSuccess(false);
        updateUIState();
    }

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
            ui.getRunSyntaxBtn().setDisable(!state.isLexerRunSuccess());
            ui.getGenerateInputTreeBtn().setDisable(!state.isParseRunSuccess());
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
