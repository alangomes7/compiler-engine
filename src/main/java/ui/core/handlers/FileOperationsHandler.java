package ui.core.handlers;

import java.io.File;
import ui.Ui;
import ui.core.controllers.UiStateController;
import ui.core.services.FileService;
import ui.core.state.AnalysisState;

/**
 * Handles file loading operations for the compiler application.
 *
 * <p>This handler manages the loading of three types of input files:
 *
 * <ul>
 *   <li><b>Token rule files:</b> Lexer specification defining token patterns
 *   <li><b>Grammar files:</b> Context-free grammar definition for parsing
 *   <li><b>Input files:</b> Source code or input text to be analyzed
 * </ul>
 *
 * <p><b>Design Philosophy - Lazy Lexer Building:</b> The lexer is NOT built immediately when the
 * token file is loaded. Instead, only the token file path is stored and validated. The actual lexer
 * construction is deferred until {@link ExecutionHandler#handleRunLexer()} is called. This
 * approach:
 *
 * <ul>
 *   <li>Avoids unnecessary work if the user never runs the lexer
 *   <li>Ensures the lexer is always built with the latest token rules
 *   <li>Prevents outdated lexer instances from being used when token rules change
 *   <li>Improves application startup and file loading responsiveness
 * </ul>
 *
 * <p>Each file load operation:
 *
 * <ol>
 *   <li>Presents a file chooser dialog filtered by appropriate extensions
 *   <li>Updates the UI to show the loaded filename
 *   <li>Updates the analysis state with the file path (but does NOT process the file)
 *   <li>Marks the corresponding "loaded" flag as true
 *   <li>Invalidates dependent analysis results
 * </ol>
 *
 * <p>Typical usage:
 *
 * <pre>
 * FileOperationsHandler fileHandler = new FileOperationsHandler(ui, state, stateController);
 *
 * // Load files in recommended order (lexer building deferred)
 * fileHandler.handleLoadTokenFile();      // Step 1: Store token file path
 * fileHandler.handleLoadGrammarFile();    // Step 2: Load grammar
 * fileHandler.handleLoadInputFile();      // Step 3: Load input to analyze
 *
 * // Lexer is built later when user clicks "Run Lexer Analysis"
 * </pre>
 *
 * @see Ui
 * @see AnalysisState
 * @see UiStateController
 * @see FileService
 * @see ExecutionHandler
 */
public class FileOperationsHandler {
    private final Ui ui;
    private final AnalysisState state;
    private final UiStateController stateController;

    /**
     * Constructs a FileOperationsHandler with references to the main UI, analysis state, and state
     * controller.
     *
     * @param ui the main UI instance
     * @param state the shared analysis state
     * @param stateController the controller for updating UI components based on state changes
     */
    public FileOperationsHandler(Ui ui, AnalysisState state, UiStateController stateController) {
        this.ui = ui;
        this.state = state;
        this.stateController = stateController;
    }

    /**
     * Loads a token rule file and stores its path for later lexer building.
     *
     * <p><b>Important Behavioral Change:</b> Unlike the previous implementation, this method does
     * NOT build the lexer. It only:
     *
     * <ol>
     *   <li>Validates the selected file
     *   <li>Stores the token file path in the analysis state
     *   <li>Updates the token file label with the filename
     *   <li>Resets analysis state that depends on token rules
     *   <li>Sets {@code tokenLoaded} flag to true
     * </ol>
     *
     * <p>The actual lexer building is deferred until {@link ExecutionHandler#handleRunLexer()} is
     * called. This ensures the lexer is always built with the latest token rules and prevents
     * outdated lexer instances from being used.
     *
     * <p>If the user loads a new token file after previous analysis, the analysis state is
     * invalidated to ensure consistency.
     */
    public void handleLoadTokenFile() {
        File file =
                FileService.selectFile(
                        ui.getInputArea().getScene().getWindow(),
                        "Select Lexer Rules",
                        "*.txt",
                        "*.lexer");
        if (file == null) return;

        // Store the token file path for later lexer building
        state.setTokenFilePath(file.getAbsolutePath());
        ui.getTokenFileLabel().setText(file.getName());

        // Reset analysis state that depends on token rules
        resetForNewToken();

        // Mark token as loaded - lexer will be built when runLexer is called
        state.setTokenLoaded(true);
        stateController.updateUIState();

        ui.getOutputArea()
                .setText(
                        "Token file loaded: "
                                + file.getName()
                                + "\nClick 'Run Lexer Analysis' to build the lexer and scan input.");
    }

    /**
     * Resets all analysis‑related state flags when a new token file is loaded.
     *
     * <p>This ensures that previously computed results are invalidated and the UI reflects the need
     * to re-analyze with the new token rules.
     *
     * <p><b>Note:</b> The lexer itself is NOT cleared or rebuilt here. The lexer will be rebuilt
     * when the user runs the lexer analysis.
     */
    private void resetForNewToken() {
        state.setLexerRunSuccess(false);
        state.setParseRunSuccess(false);
        state.setHasSymbolTableData(false);
        state.setHasFirstFollowData(false);
        state.setHasParseTableData(false);
        state.setHasGrammarTree(false);
        state.setHasInputTree(false);
        state.setCurrentAutomaton(null);
        state.setLexerNeedsRebuild(true); // Mark that lexer needs to be rebuilt
        stateController.updateUIState();
    }

    /**
     * Loads a grammar definition file and stores it in the parser service.
     *
     * <p>This method:
     *
     * <ol>
     *   <li>Opens a file chooser for selecting grammar files (*.txt, *.grammar)
     *   <li>Updates the grammar file label with the selected filename
     *   <li>Loads the grammar asynchronously, showing progress feedback
     *   <li>Sets the grammar loaded flag on success
     *   <li>Updates UI state to enable grammar-dependent operations
     * </ol>
     *
     * <p>Note: Loading a new grammar does NOT automatically invalidate token-related state. The
     * grammar and token rules must be compatible.
     */
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

    /**
     * Loads an input source file and places its content into the input text area.
     *
     * <p>This method:
     *
     * <ol>
     *   <li>Opens a file chooser for selecting input files (*.txt, *.input)
     *   <li>Reads and filters the file content (removing stray characters)
     *   <li>Sets the content in the input area without triggering analysis invalidation
     *   <li>Uses a programmatic change flag to prevent unwanted state resets
     * </ol>
     *
     * <p>The programmatic change flag ensures that the input area's change listener does not wipe
     * analysis state while loading a file programmatically. This allows the user to load a file
     * without losing previously computed analysis results (the results will be invalidated when the
     * user edits the input manually).
     */
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
            state.setProgrammaticChange(
                    true); // Prevent the change listener from wiping analysis state
            ui.getInputArea().setText(content);
            state.setProgrammaticChange(false);
            ui.getOutputArea().setText("Input file loaded successfully.");
        } catch (Exception ex) {
            ui.getOutputArea().setText("Error loading input file: " + ex.getMessage());
        }
    }
}
