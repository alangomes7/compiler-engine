package ui.core.handlers;

import core.lexer.models.atomic.LexerError;
import core.lexer.models.atomic.Token;
import core.lexer.models.automata.DFA;
import core.parser.models.FirstFollowTable;
import core.parser.models.ParseTable;
import core.parser.models.atomic.ParserError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import models.atomic.Constants;
import ui.Ui;
import ui.core.controllers.UiStateController;
import ui.core.graph.automata.AutomataVisualizer;
import ui.core.graph.automata.InteractiveAutomataView;
import ui.core.services.LexerService.LexerResult;
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

        if (state.isLexerNeedsRebuild() || !ui.getLexerService().isInitialized()) {
            buildLexerAndScan(input);
        } else {
            performScan(input);
        }
    }

    private void buildLexerAndScan(String input) {
        String tokenFilePath = state.getTokenFilePath();
        if (tokenFilePath == null || tokenFilePath.isEmpty()) {
            ui.getOutputArea()
                    .setText("Error: No token file loaded. Please load a token file first.");
            return;
        }

        ui.getTaskExecutor()
                .execute(
                        "Building Lexer...",
                        log -> {
                            log.accept("Reading token rules from: " + tokenFilePath);
                            DFA automaton = null;
                            try {
                                automaton = ui.getLexerService().buildLexer(tokenFilePath, log);
                            } catch (Exception ex) {
                                System.getLogger(ExecutionHandler.class.getName())
                                        .log(System.Logger.Level.ERROR, (String) null, ex);
                            }

                            log.accept("Creating Lexer Automaton Image...");
                            AutomataVisualizer.exportToImage(automaton, "lexer_automata.png");

                            log.accept("Scanning input...");
                            LexerResult scanResult = ui.getLexerService().scan(input);

                            return new Object[] {automaton, scanResult};
                        },
                        result -> {
                            Object[] res = (Object[]) result;
                            DFA automaton = (DFA) res[0];
                            LexerResult scanResult = (LexerResult) res[1];

                            state.setCurrentAutomaton(automaton);
                            ui.getAutomataDetailsArea().setText(automaton.toString());
                            ui.getInteractiveGraphContainer()
                                    .setCenter(new InteractiveAutomataView(automaton));

                            // 2. Format the output to include errors if they exist
                            if (scanResult.errors != null && !scanResult.errors.isEmpty()) {
                                String errorText =
                                        scanResult.errors.stream()
                                                .map(LexerError::toString)
                                                .collect(java.util.stream.Collectors.joining("\n"));
                                ui.getOutputArea()
                                        .setText(
                                                "Lexical Analysis Completed with Errors:\n"
                                                        + errorText);
                            } else {
                                ui.getOutputArea().setText(scanResult.output);
                            }

                            ui.getSymbolTableViewer()
                                    .getItems()
                                    .setAll(ui.getLexerService().getSymbolTable());
                            state.setHasSymbolTableData(true);
                            state.setLexerRunSuccess(true);
                            state.setLexerNeedsRebuild(false);
                            stateController.updateUIState();
                        },
                        err -> {
                            ui.getOutputArea().setText("Lexer Error: " + err.getMessage());
                            state.setLexerRunSuccess(false);
                            state.setLexerNeedsRebuild(true);
                            stateController.updateUIState();
                        });
    }

    private void performScan(String input) {
        ui.getTaskExecutor()
                .execute(
                        "Running Lexer...",
                        log -> {
                            log.accept("Scanning input...");
                            return ui.getLexerService().scan(input);
                        },
                        result -> {
                            LexerResult scanResult = (LexerResult) result;

                            if (scanResult.errors != null && !scanResult.errors.isEmpty()) {
                                String errorText =
                                        scanResult.errors.stream()
                                                .map(LexerError::toString)
                                                .collect(java.util.stream.Collectors.joining("\n"));
                                ui.getOutputArea()
                                        .setText(
                                                "Lexical Analysis Completed with Errors:\n"
                                                        + errorText);
                            } else {
                                ui.getOutputArea().setText(scanResult.output);
                            }

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
        String selectedAlgorithm = ui.getParserComboBox().getValue();

        ui.getTaskExecutor()
                .execute(
                        "Running Syntax Analysis...",
                        log -> {
                            FirstFollowTable ffTable = state.getCurrentFirstFollowTable();
                            if (ffTable == null) {
                                log.accept("Building First/Follow tables...");
                                ffTable = ui.getParserService().buildFirstFollowTable();
                                state.setCurrentFirstFollowTable(ffTable);
                            }

                            ParseTable parseTable = state.getCurrentParseTable();
                            if (parseTable == null) {
                                log.accept("Building Parse Table...");
                                parseTable =
                                        ui.getParserService()
                                                .buildParseTable(ffTable, Collections.emptyList());
                                state.setCurrentParseTable(parseTable);
                            }

                            log.accept("Parsing tokens using: " + selectedAlgorithm);
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
                                tokenStream.add(
                                        new Token(Constants.EOF, Constants.EOF, lastLine, lastCol));
                            }

                            log.accept("Parsing tokens...");
                            return ui.getParserService()
                                    .parseTokens(selectedAlgorithm, parseTable, tokenStream);
                        },
                        result -> {
                            state.setCurrentParseResult(result);
                            ui.getFirstFollowTable()
                                    .getItems()
                                    .setAll(ui.getParserService().getGrammar().getNonTerminals());
                            ui.getParserTableManager().populate(state.getCurrentParseTable());
                            state.setHasFirstFollowData(true);
                            state.setHasParseTableData(true);

                            // 3. Display the parser used in the final output[cite: 3]
                            if (result.errors.isEmpty()) {
                                state.setSyntaxBaseOutput(
                                        String.format(
                                                "Syntax Analysis using [%s] Completed Successfully.\nNo errors found.",
                                                result.parserUsed));
                                state.setParseRunSuccess(true);
                            } else {
                                String errorText =
                                        result.errors.stream()
                                                .map(ParserError::toString)
                                                .collect(java.util.stream.Collectors.joining("\n"));

                                state.setSyntaxBaseOutput(
                                        String.format(
                                                "Syntax Analysis using [%s] Completed with Errors:\n%s",
                                                result.parserUsed, errorText));
                                state.setParseRunSuccess(false);
                            }

                            if (result.tree != null) {
                                state.setSyntaxTreeOutput(
                                        "\nParser tree: \n" + result.tree.toString());
                            } else {
                                state.setSyntaxTreeOutput("");
                            }

                            ui.refreshTextOutputs();
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
                        "Validating Grammar & Compatibility...",
                        log -> {
                            core.parser.core.grammar.GrammarClassification classification;
                            if (state.getCurrentParseTable() == null) {
                                log.accept("Building temporary Parse table for validation...");
                                FirstFollowTable ffTable =
                                        ui.getParserService().buildFirstFollowTable();
                                classification =
                                        ui.getParserService()
                                                .classifyGrammarWithParserTable(
                                                        ui.getParserService()
                                                                .buildParseTable(
                                                                        ffTable,
                                                                        java.util.Collections
                                                                                .emptyList()));
                            } else {
                                classification =
                                        ui.getParserService()
                                                .classifyGrammarWithParserTable(
                                                        state.getCurrentParseTable());
                            }

                            log.accept("Checking Grammar-Lexer compatibility...");
                            String compatibilityReport =
                                    "Cannot validate Lexer compatibility: Lexer token file not loaded.\n";
                            String tokenPath = state.getTokenFilePath();

                            if (tokenPath != null && !tokenPath.isEmpty()) {
                                java.util.List<core.lexer.models.atomic.Rule> lexerRules =
                                        core.lexer.core.translators.RuleReader.readRules(tokenPath);
                                compatibilityReport =
                                        core.validator.GrammarLexerCompatibility.validate(
                                                ui.getParserService().getGrammar(), lexerRules);
                            }

                            // Return an array instead of a single string
                            return new String[] {classification.toString(), compatibilityReport};
                        },
                        result -> {
                            String[] reports = (String[]) result;
                            state.setValidationClassificationReport(reports[0]);
                            state.setValidationCompatibilityReport(reports[1]);
                            state.setHasValidationData(true);

                            ui.refreshTextOutputs(); // Dynamically update the UI
                            stateController.updateUIState();
                        },
                        err -> ui.getOutputArea().setText("Validation Error: " + err.getMessage()));
    }
}
