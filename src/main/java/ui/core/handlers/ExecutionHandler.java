package ui.core.handlers;

import core.parser.models.FirstFollowTable;
import core.parser.models.ParseTable;
import ui.Ui;
import ui.core.controllers.UiStateController;
import ui.core.state.AnalysisState;

public class ExecutionHandler {
    private final Ui ui;
    private final AnalysisState state;
    private final UiStateController stateController;

    public ExecutionHandler(Ui ui, AnalysisState state, UiStateController stateController) {
        this.ui = ui;
        this.state = state;
        this.stateController = stateController;
    }

    public void handleRunLexer() {
        String input = ui.getInputArea().getText();
        if (input == null || input.trim().isEmpty()) {
            ui.getOutputArea().setText("Error: Input is empty. Please enter text or load a file.");
            return;
        }

        ui.getTaskExecutor()
                .execute(
                        "Running Lexer...",
                        log -> {
                            log.accept("Scanning input...");
                            return ui.getLexerService().scan(input);
                        },
                        result -> {
                            ui.getOutputArea().setText(result);
                            ui.getSymbolTableViewer()
                                    .getItems()
                                    .setAll(ui.getLexerService().getSymbolTable());
                            state.setHasSymbolTableData(true);
                            state.setLexerRunSuccess(true);
                            stateController.updateUIState();
                        },
                        err -> {
                            ui.getOutputArea().setText("Lexer Error: " + err.getMessage());
                            state.setLexerRunSuccess(false);
                            stateController.updateUIState();
                        });
    }

    public void handleRunSyntaxAnalysis() {
        ui.getTaskExecutor()
                .execute(
                        "Running Syntax Analysis...",
                        log -> {
                            log.accept("Building First/Follow tables...");
                            FirstFollowTable ffTable =
                                    ui.getParserService().buildFirstFollowTable();
                            state.setCurrentFirstFollowTable(ffTable);

                            log.accept("Building Parse Table...");
                            ParseTable parseTable =
                                    ui.getParserService()
                                            .buildParseTable(
                                                    ffTable, ui.getLexerService().getSymbolTable());
                            state.setCurrentParseTable(parseTable);

                            log.accept("Parsing tokens...");
                            return ui.getParserService()
                                    .parseTokens(parseTable, ui.getLexerService().getSymbolTable());
                        },
                        result -> {
                            state.setCurrentParseResult(result);

                            // Update UI tables
                            ui.getFirstFollowTable()
                                    .getItems()
                                    .setAll(ui.getParserService().getGrammar().getNonTerminals());
                            ui.getParserTableManager().populate(state.getCurrentParseTable());

                            state.setHasFirstFollowData(true);
                            state.setHasParseTableData(true);

                            if (result.errors.isEmpty()) {
                                ui.getOutputArea()
                                        .setText(
                                                "Syntax Analysis Completed Successfully.\nNo errors found.");
                                state.setParseRunSuccess(true);
                            } else {
                                ui.getOutputArea()
                                        .setText(
                                                "Syntax Analysis Completed with Errors:\n"
                                                        + String.join("\n", result.errors));
                                state.setParseRunSuccess(false);
                            }
                            stateController.updateUIState();
                        },
                        err -> {
                            ui.getOutputArea().setText("Syntax Error: " + err.getMessage());
                            state.setParseRunSuccess(false);
                            stateController.updateUIState();
                        });
    }

    public void handleValidateCompatibility() {
        ui.getTaskExecutor()
                .execute(
                        "Validating Grammar...",
                        log -> {
                            if (state.getCurrentParseTable() == null) {
                                log.accept("Building temporary Parse table for validation...");
                                FirstFollowTable ffTable =
                                        ui.getParserService().buildFirstFollowTable();
                                return ui.getParserService()
                                        .classifyGrammarWithParserTable(
                                                ui.getParserService()
                                                        .buildParseTable(
                                                                ffTable,
                                                                java.util.Collections.emptyList()));
                            }
                            return ui.getParserService()
                                    .classifyGrammarWithParserTable(state.getCurrentParseTable());
                        },
                        classification -> {
                            ui.getValidatorOutputArea().setText(classification.toString());
                            state.setHasValidationData(true);
                            stateController.updateUIState();
                        },
                        err -> ui.getOutputArea().setText("Validation Error: " + err.getMessage()));
    }
}
