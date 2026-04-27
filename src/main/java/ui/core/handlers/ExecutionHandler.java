package ui.core.handlers;

import core.lexer.models.atomic.Token;
import core.parser.models.FirstFollowTable;
import core.parser.models.ParseTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
                            // 1. Only build First/Follow if it doesn't already exist
                            FirstFollowTable ffTable = state.getCurrentFirstFollowTable();
                            if (ffTable == null) {
                                log.accept("Building First/Follow tables...");
                                ffTable = ui.getParserService().buildFirstFollowTable();
                                state.setCurrentFirstFollowTable(ffTable);
                            }

                            // 2. Only build Parse Table if it doesn't already exist
                            ParseTable parseTable = state.getCurrentParseTable();
                            if (parseTable == null) {
                                log.accept("Building Parse Table...");
                                parseTable =
                                        ui.getParserService()
                                                .buildParseTable(ffTable, Collections.emptyList());
                                state.setCurrentParseTable(parseTable);
                            }

                            // 3. Prepare token stream and append End Of File marker ($)
                            log.accept("Preparing Token Stream...");
                            List<Token> tokenStream =
                                    new ArrayList<>(ui.getLexerService().getSymbolTable());

                            if (tokenStream.isEmpty()
                                    || !tokenStream
                                            .get(tokenStream.size() - 1)
                                            .getLexeme()
                                            .equals("$")) {
                                int lastLine =
                                        tokenStream.isEmpty()
                                                ? 1
                                                : tokenStream.get(tokenStream.size() - 1).getLine();
                                int lastCol =
                                        tokenStream.isEmpty()
                                                ? 1
                                                : tokenStream.get(tokenStream.size() - 1).getCol()
                                                        + 1;
                                tokenStream.add(new Token("$", "$", lastLine, lastCol));
                            }

                            log.accept("Parsing tokens...");
                            return ui.getParserService().parseTokens(parseTable, tokenStream);
                        },
                        result -> {
                            state.setCurrentParseResult(result);

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
                                                                ffTable, Collections.emptyList()));
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
