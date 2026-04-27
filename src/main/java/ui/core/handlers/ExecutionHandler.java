package ui.core.handlers;

import core.lexer.models.atomic.Token;
import core.lexer.models.automata.DFA;
import core.parser.models.FirstFollowTable;
import core.parser.models.ParseTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ui.Ui;
import ui.core.controllers.UiStateController;
import ui.core.graph.automata.AutomataVisualizer;
import ui.core.graph.automata.InteractiveAutomataView;
import ui.core.state.AnalysisState;

/**
 * Handles the execution of core compilation pipeline operations.
 *
 * <p>This handler orchestrates the three main execution phases of the application:
 *
 * <ul>
 *   <li><b>Lexical Analysis:</b> Scanning input text to produce a token stream
 *   <li><b>Syntax Analysis:</b> Parsing tokens according to grammar rules
 *   <li><b>Grammar Validation:</b> Checking grammar compatibility (LL(1) conformance)
 * </ul>
 *
 * <p><b>Lazy Lexer Building:</b> The lexer is built only when {@link #handleRunLexer()} is called
 * for the first time or when the token file has changed. This prevents unnecessary work and ensures
 * the lexer is always up-to-date.
 *
 * <p>Each operation is executed asynchronously using a background task executor to keep the UI
 * responsive. The handler caches intermediate results (FIRST/FOLLOW tables, parse tables) to avoid
 * redundant computations when the same grammar is used for multiple parsing operations.
 *
 * @see Ui
 * @see AnalysisState
 * @see UiStateController
 */
public class ExecutionHandler {
    private final Ui ui;
    private final AnalysisState state;
    private final UiStateController stateController;

    /**
     * Constructs an ExecutionHandler with necessary dependencies.
     *
     * @param ui the main UI instance providing access to services and components
     * @param state the shared analysis state for tracking operation results
     * @param stateController the controller for updating UI state after operations
     */
    public ExecutionHandler(Ui ui, AnalysisState state, UiStateController stateController) {
        this.ui = ui;
        this.state = state;
        this.stateController = stateController;
    }

    /**
     * Executes the lexical analyzer on the current input text.
     *
     * <p>This method performs the following steps:
     *
     * <ol>
     *   <li>Validates that input is not empty
     *   <li>Checks if lexer needs to be built (first run or token file changed)
     *   <li>If needed, builds the lexer from the loaded token file asynchronously
     *   <li>Runs the lexer service on the input text
     *   <li>Displays the scanning result in the output area
     *   <li>Populates the symbol table viewer with the resulting tokens
     *   <li>Updates the analysis state and UI controls
     * </ol>
     *
     * <p><b>Lazy Building:</b> The lexer is built only when necessary. If the token file hasn't
     * changed and the lexer already exists, only scanning is performed.
     *
     * <p>On success, {@code lexerRunSuccess} and {@code hasSymbolTableData} are set to {@code
     * true}. On failure, the error message is displayed and the state flags remain {@code false}.
     */
    public void handleRunLexer() {
        String input = ui.getInputArea().getText();
        if (input == null || input.trim().isEmpty()) {
            ui.getOutputArea().setText("Error: Input is empty. Please enter text or load a file.");
            return;
        }

        // Check if lexer needs to be built
        if (state.isLexerNeedsRebuild() || !ui.getLexerService().isInitialized()) {
            buildLexerAndScan(input);
        } else {
            performScan(input);
        }
    }

    /**
     * Builds the lexer from token rules and then scans the input.
     *
     * @param input the input text to scan
     */
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
                            String scanResult = ui.getLexerService().scan(input);

                            return new Object[] {automaton, scanResult};
                        },
                        result -> {
                            Object[] res = (Object[]) result;
                            DFA automaton = (DFA) res[0];
                            String scanResult = (String) res[1];

                            // Update UI with automaton
                            state.setCurrentAutomaton(automaton);
                            ui.getAutomataDetailsArea().setText(automaton.toString());
                            ui.getInteractiveGraphContainer()
                                    .setCenter(new InteractiveAutomataView(automaton));

                            // Update symbol table
                            ui.getOutputArea().setText(scanResult);
                            ui.getSymbolTableViewer()
                                    .getItems()
                                    .setAll(ui.getLexerService().getSymbolTable());

                            // Update state
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

    /**
     * Performs scanning only (lexer already built).
     *
     * @param input the input text to scan
     */
    private void performScan(String input) {
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

    /**
     * Executes syntax analysis (parsing) on the token stream from the lexer.
     *
     * <p>This method orchestrates the complete parsing pipeline:
     *
     * <ol>
     *   <li>Builds FIRST/FOLLOW tables if not already cached
     *   <li>Builds the parse table if not already cached
     *   <li>Prepares the token stream with an EOF marker ($)
     *   <li>Runs the LL(1) parser on the token stream
     *   <li>Displays results and populates table visualizations
     * </ol>
     *
     * <p>Caching is used to avoid recomputing FIRST/FOLLOW tables and parse tables when the same
     * grammar is used for multiple parsing operations.
     *
     * <p>The token stream is automatically augmented with an EOF token ($) if it is not already
     * present, which is required for proper parser termination.
     *
     * <p>On successful completion (no parse errors), {@code parseRunSuccess} is set to {@code
     * true}. The parse result is stored in the analysis state for later tree generation or export.
     */
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

    /**
     * Validates grammar compatibility by checking if it is LL(1).
     *
     * <p>This method performs grammar validation to determine whether the loaded grammar is LL(1)
     * compatible. An LL(1) grammar allows deterministic parsing with a single token of lookahead.
     *
     * <p>The validation process:
     *
     * <ul>
     *   <li>Uses the cached parse table if available, otherwise builds a temporary one
     *   <li>Classifies the grammar and identifies any conflicts
     *   <li>Displays the classification result in the validator output area
     * </ul>
     *
     * <p>On completion, {@code hasValidationData} is set to {@code true} and the UI is updated
     * accordingly.
     */
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
