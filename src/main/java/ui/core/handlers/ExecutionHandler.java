package ui.core.handlers;

import core.lexer.core.translators.RuleReader;
import core.lexer.models.atomic.LexerError;
import core.lexer.models.atomic.Rule;
import core.lexer.models.atomic.Token;
import core.lexer.models.automata.DFA;
import core.parser.core.grammar.GrammarClassification;
import core.parser.models.FirstFollowTable;
import core.parser.models.ParseTable;
import core.parser.models.atomic.ParserError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import models.atomic.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ui.Ui;
import ui.core.controllers.UiStateController;
import ui.core.graph.automata.AutomataVisualizer;
import ui.core.graph.automata.InteractiveAutomataView;
import ui.core.services.LexerService.LexerResult;
import ui.core.state.AnalysisState;

public class ExecutionHandler {
    private static final Logger log = LoggerFactory.getLogger(ExecutionHandler.class);
    private final Ui ui;
    private final AnalysisState state;
    private final UiStateController stateController;

    public ExecutionHandler(Ui ui, AnalysisState state, UiStateController stateController) {
        this.ui = ui;
        this.state = state;
        this.stateController = stateController;
    }

    private <T> T trackServiceTime(
            String serviceName, Consumer<String> logCallback, Callable<T> task) throws Exception {
        boolean isDevMode = "Developer".equals(ui.getUserModeComboBox().getValue());
        long start = System.currentTimeMillis();

        T result = task.call();

        long duration = System.currentTimeMillis() - start;
        if (isDevMode) {
            logCallback.accept("[DEV] " + serviceName + " execution time: " + duration + " ms");
        }

        return result;
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
                        logCallback -> {
                            logCallback.accept("Reading token rules from: " + tokenFilePath);
                            DFA automaton = null;
                            LexerResult scanResult = null;
                            try {
                                automaton =
                                        trackServiceTime(
                                                "LexerService.buildLexer",
                                                logCallback,
                                                () ->
                                                        ui.getLexerService()
                                                                .buildLexer(
                                                                        tokenFilePath,
                                                                        logCallback));

                                logCallback.accept("Creating Lexer Automaton Image...");

                                DFA finalAutomaton = automaton;
                                trackServiceTime(
                                        "AutomataVisualizer.exportToImage",
                                        logCallback,
                                        () -> {
                                            AutomataVisualizer.exportToImage(
                                                    finalAutomaton, "lexer_automata.png");
                                            return null;
                                        });

                                logCallback.accept("Scanning input...");
                                scanResult =
                                        trackServiceTime(
                                                "LexerService.scan",
                                                logCallback,
                                                () -> ui.getLexerService().scan(input));
                            } catch (Exception ex) {
                                log.error(ex.getMessage());
                            }
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
                        logCallback -> {
                            logCallback.accept("Scanning input...");
                            LexerResult result = null;
                            try {
                                result =
                                        trackServiceTime(
                                                "LexerService.scan",
                                                logCallback,
                                                () -> ui.getLexerService().scan(input));
                            } catch (Exception e) {
                                log.error(e.getMessage());
                            }
                            return result;
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
                        logCallback -> {
                            FirstFollowTable ffTable = state.getCurrentFirstFollowTable();
                            if (ffTable == null) {
                                logCallback.accept("Building First/Follow tables...");
                                try {
                                    ffTable =
                                            trackServiceTime(
                                                    "ParserService.buildFirstFollowTable",
                                                    logCallback,
                                                    () ->
                                                            ui.getParserService()
                                                                    .buildFirstFollowTable());
                                } catch (Exception e) {
                                    logCallback.accept("Error: " + e.getMessage());
                                }
                                state.setCurrentFirstFollowTable(ffTable);
                            }

                            ParseTable parseTable = state.getCurrentParseTable();
                            if (parseTable == null) {
                                logCallback.accept("Building Parse Table...");
                                FirstFollowTable finalFfTable = ffTable;
                                try {
                                    parseTable =
                                            trackServiceTime(
                                                    "ParserService.buildParseTable",
                                                    logCallback,
                                                    () ->
                                                            ui.getParserService()
                                                                    .buildParseTable(
                                                                            finalFfTable,
                                                                            Collections
                                                                                    .emptyList()));
                                } catch (Exception e) {
                                    logCallback.accept("Error: " + e.getMessage());
                                }
                                state.setCurrentParseTable(parseTable);
                            }

                            logCallback.accept("Parsing tokens using: " + selectedAlgorithm);
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

                            logCallback.accept("Parsing tokens...");
                            ParseTable finalParseTable = parseTable;
                            try {
                                return trackServiceTime(
                                        "ParserService.parseTokens",
                                        logCallback,
                                        () ->
                                                ui.getParserService()
                                                        .parseTokens(
                                                                selectedAlgorithm,
                                                                finalParseTable,
                                                                tokenStream));
                            } catch (Exception ex) {
                                System.getLogger(ExecutionHandler.class.getName())
                                        .log(System.Logger.Level.ERROR, (String) null, ex);

                                throw new RuntimeException(
                                        "Syntax analysis failed: " + ex.getMessage(), ex);
                            }
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
                        logCallback -> {
                            GrammarClassification classification = new GrammarClassification();
                            String compatibilityReport = "";

                            try {
                                if (state.getCurrentParseTable() == null) {
                                    logCallback.accept(
                                            "Building temporary Parse table for validation...");

                                    FirstFollowTable ffTable =
                                            trackServiceTime(
                                                    "ParserService.buildFirstFollowTable",
                                                    logCallback,
                                                    () ->
                                                            ui.getParserService()
                                                                    .buildFirstFollowTable());

                                    ParseTable tempParseTable =
                                            trackServiceTime(
                                                    "ParserService.buildParseTable",
                                                    logCallback,
                                                    () ->
                                                            ui.getParserService()
                                                                    .buildParseTable(
                                                                            ffTable,
                                                                            java.util.Collections
                                                                                    .emptyList()));

                                    classification =
                                            trackServiceTime(
                                                    "ParserService.classifyGrammar",
                                                    logCallback,
                                                    () ->
                                                            ui.getParserService()
                                                                    .classifyGrammarWithParserTable(
                                                                            tempParseTable));
                                } else {
                                    classification =
                                            trackServiceTime(
                                                    "ParserService.classifyGrammar",
                                                    logCallback,
                                                    () ->
                                                            ui.getParserService()
                                                                    .classifyGrammarWithParserTable(
                                                                            state
                                                                                    .getCurrentParseTable()));
                                }

                                logCallback.accept("Checking Grammar-Lexer compatibility...");
                                compatibilityReport =
                                        "Cannot validate Lexer compatibility: Lexer token file not loaded.\n";
                                String tokenPath = state.getTokenFilePath();

                                if (tokenPath != null && !tokenPath.isEmpty()) {
                                    java.util.List<Rule> lexerRules =
                                            trackServiceTime(
                                                    "RuleReader.readRules",
                                                    logCallback,
                                                    () -> RuleReader.readRules(tokenPath));

                                    compatibilityReport =
                                            trackServiceTime(
                                                    "GrammarLexerCompatibility.validate",
                                                    logCallback,
                                                    () ->
                                                            core.validator.GrammarLexerCompatibility
                                                                    .validate(
                                                                            ui.getParserService()
                                                                                    .getGrammar(),
                                                                            lexerRules));
                                }
                            } catch (Exception e) {
                                log.error(e.getMessage());
                            }

                            return new String[] {classification.toString(), compatibilityReport};
                        },
                        result -> {
                            String[] reports = (String[]) result;
                            state.setValidationClassificationReport(reports[0]);
                            state.setValidationCompatibilityReport(reports[1]);
                            state.setHasValidationData(true);

                            ui.refreshTextOutputs();
                            stateController.updateUIState();
                        },
                        err -> ui.getOutputArea().setText("Validation Error: " + err.getMessage()));
    }
}
