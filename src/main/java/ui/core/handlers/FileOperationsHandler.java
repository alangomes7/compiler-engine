package ui.core.handlers;

import java.io.File;
import ui.Ui;
import ui.core.controllers.UiStateController;
import ui.core.services.FileService;
import ui.core.state.AnalysisState;

public class FileOperationsHandler {
    private final Ui ui;
    private final AnalysisState state;
    private final UiStateController stateController;

    public FileOperationsHandler(Ui ui, AnalysisState state, UiStateController stateController) {
        this.ui = ui;
        this.state = state;
        this.stateController = stateController;
    }

    public void handleLoadTokenFile() {
        File file =
                FileService.selectFile(
                        ui.getInputArea().getScene().getWindow(),
                        "Select Lexer Rules",
                        "*.txt",
                        "*.lexer");
        if (file == null) return;

        state.setTokenFilePath(file.getAbsolutePath());
        ui.getTokenFileLabel().setText(file.getName());

        resetForNewToken();

        state.setTokenLoaded(true);
        stateController.updateUIState();

        ui.getOutputArea()
                .setText(
                        "Token file loaded: "
                                + file.getName()
                                + "\nClick 'Run Lexer Analysis' to build the lexer and scan input.");
    }

    private void resetForNewToken() {
        state.setLexerRunSuccess(false);
        state.setParseRunSuccess(false);
        state.setHasSymbolTableData(false);
        state.setHasFirstFollowData(false);
        state.setHasParseTableData(false);
        state.setHasGrammarTree(false);
        state.setHasInputTree(false);
        state.setCurrentAutomaton(null);
        state.setLexerNeedsRebuild(true);
        stateController.updateUIState();
    }

    public void handleLoadGrammarFile() {
        File file =
                FileService.selectFile(
                        ui.getInputArea().getScene().getWindow(),
                        "Select Grammar File",
                        "*.txt",
                        "*.grammar");
        if (file == null) return;

        ui.getGrammarFileLabel().setText(file.getName());

        ui.getTaskExecutor()
                .execute(
                        "Loading Grammar...",
                        log -> {
                            try {
                                ui.getParserService().loadGrammar(file.getAbsolutePath());
                                return true;
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        },
                        success -> {
                            state.setGrammarLoaded(true);
                            ui.getOutputArea().setText("Grammar successfully loaded.");
                            stateController.updateUIState();
                        },
                        err -> {
                            ui.getOutputArea()
                                    .setText("Error loading grammar: " + err.getMessage());
                            state.setGrammarLoaded(false);
                            stateController.updateUIState();
                        });
    }

    public void handleLoadInputFile() {
        File file =
                FileService.selectFile(
                        ui.getInputArea().getScene().getWindow(),
                        "Select Input File",
                        "*.txt",
                        "*.input");
        if (file == null) return;

        try {
            String content = FileService.readFileContent(file);
            state.setProgrammaticChange(true);
            ui.getInputArea().setText(content);
            ui.getInputFileLabel().setText(file.getName());
            state.setProgrammaticChange(false);
            ui.getOutputArea().setText("Input file loaded successfully.");
        } catch (Exception ex) {
            ui.getOutputArea().setText("Error loading input file: " + ex.getMessage());
        }
    }
}
