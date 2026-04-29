package ui;

import core.lexer.models.atomic.Token;
import core.parser.models.atomic.Symbol;
import java.io.PrintStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import lombok.Getter;
import ui.core.controllers.UiStateController;
import ui.core.handlers.ExecutionHandler;
import ui.core.handlers.ExportHandler;
import ui.core.handlers.FileOperationsHandler;
import ui.core.handlers.VisualizationHandler;
import ui.core.services.LexerService;
import ui.core.services.ParserService;
import ui.core.state.AnalysisState;
import ui.core.tables.FirstFollowTableManager;
import ui.core.tables.ParserTableManager;
import ui.core.tables.SymbolTableManager;
import ui.util.BackgroundTaskExecutor;
import ui.util.UiUtils;

/**
 * Main JavaFX controller for the Compiler UI.
 *
 * <p>This class serves as the central coordinator for the entire application's user interface. It
 * manages all UI components, coordinates between services and handlers, and maintains the
 * application state throughout the compilation pipeline.
 *
 * <p><b>Architecture Overview:</b>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                         Ui (Controller)                      │
 * ├─────────────────────────────────────────────────────────────┤
 * │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
 * │  │    Services  │  │   Handlers   │  │   State & Controllers│
 * │  ├──────────────┤  ├──────────────┤  ├──────────────────┤  │
 * │  │ LexerService │  │FileOperations│  │  AnalysisState   │  │
 * │  │ ParserService│  │ Execution    │  │  UiStateController│  │
 * │  │              │  │ Export       │  │                  │  │
 * │  │              │  │ Visualization│  │                  │  │
 * │  └──────────────┘  └──────────────┘  └──────────────────┘  │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p><b>Typical User Workflow:</b>
 *
 * <ol>
 *   <li>Load token rule file → stores file path, does NOT build lexer immediately
 *   <li>Load grammar file → prepares parser
 *   <li>Enter or load input text
 *   <li>Run Lexer Analysis → builds lexer (if needed) and tokenizes input
 *   <li>Run Syntax Analysis → parses tokens, builds parse table
 *   <li>Validate Compatibility → checks if grammar is LL(1)
 *   <li>Generate visualizations (grammar tree, parse tree)
 *   <li>Export results (images, CSV, reports)
 * </ol>
 *
 * <p>This class is annotated with {@code @Getter} (Lombok) to generate getters for all
 * FXML-injected components, allowing handlers to access UI elements.
 *
 * @see javafx.fxml.Initializable
 * @see AnalysisState
 * @see UiStateController
 * @see FileOperationsHandler
 * @see ExecutionHandler
 * @see ExportHandler
 * @see VisualizationHandler
 */
@Getter
public class Ui implements Initializable {

    // ==========================================
    // FXML Injections (all interactive components)
    // ==========================================

    // -------------------- Buttons --------------------
    @FXML private Button loadTokenBtn;
    @FXML private Button loadGrammarBtn;
    @FXML private Button loadInputBtn;
    @FXML private Button runLexerBtn;
    @FXML private Button runSyntaxBtn;
    @FXML private Button validateCompatibilityBtn;
    @FXML private Button clearTablesBtn;
    @FXML private Button exportCsvBtn;
    @FXML private Button generateReportBtn;
    @FXML private Button saveConsoleLogBtn;
    @FXML private Button clearConsoleLogBtn;
    @FXML private Button saveOutputBtn;
    @FXML private Button clearOutputBtn;
    @FXML private Button exportGraphTextBtn;
    @FXML private Button exportGraphImageBtn;
    @FXML private Button generateGrammarTreeBtn;
    @FXML private Button exportGrammarTreeBtn;
    @FXML private Button clearGrammarTreeBtn;
    @FXML private Button generateInputTreeBtn;
    @FXML private Button exportInputTreeBtn;
    @FXML private Button clearInputTreeBtn;
    @FXML private Button saveValidationBtn;
    @FXML private Button clearValidationBtn;
    @FXML private Button cancelOperationBtn;
    @FXML private Button overlayCancelBtn;

    // -------------------- Text Areas & Labels --------------------
    @FXML private TextArea lineNumbersArea;
    @FXML private TextArea inputArea;
    @FXML private TextArea outputArea;
    @FXML private TextArea consoleArea;
    @FXML private Label tokenFileLabel;
    @FXML private Label grammarFileLabel;
    @FXML private Label inputFileLabel;
    @Getter @FXML private TextArea automataDetailsArea;
    @FXML private TextArea validatorOutputArea;

    // -------------------- Tabs --------------------
    @FXML private TabPane mainTabPane;
    @FXML private Tab consoleTab;

    // -------------------- Tables --------------------
    @FXML private TableView<Token> symbolTableViewer;
    @FXML private TableColumn<Token, Integer> symbolTableLineColumn;
    @FXML private TableColumn<Token, Integer> symbolTableColColumn;
    @FXML private TableColumn<Token, String> symbolTableLexemeColumn;
    @FXML private TableColumn<Token, String> symbolTableTokenTypeColumn;
    @Getter @FXML private TableView<Symbol> firstFollowTable;
    @FXML private TableColumn<Symbol, String> firstFollowTableNonTerminalCol;
    @FXML private TableColumn<Symbol, Set<Symbol>> firstFollowTableFirstSetCol;
    @FXML private TableColumn<Symbol, Set<Symbol>> firstFollowTableFollowSetCol;
    @FXML private TableView<Symbol> parserTable;
    @FXML private TableColumn<Symbol, String> parserTableNonTerminalCol;

    // -------------------- Containers for Graphs/Trees --------------------
    @Getter @FXML private javafx.scene.layout.BorderPane interactiveGraphContainer;
    @FXML private javafx.scene.layout.BorderPane grammarTreeContainer;
    @FXML private javafx.scene.layout.BorderPane inputTreeContainer;

    // -------------------- Overlay --------------------
    @FXML private VBox loadingOverlay;
    @FXML private Label loadingLabel;
    @FXML private Label loadingTimeLabel;

    // ==========================================
    // Services
    // ==========================================

    /** Service for lexical analysis operations (tokenization). */
    private final LexerService lexerService = new LexerService();

    /** Service for parsing operations (grammar loading, parse tables, tree building). */
    private final ParserService parserService = new ParserService();

    /** Executor for running long-running tasks in background threads with UI feedback. */
    private BackgroundTaskExecutor taskExecutor;

    /** Manager for the FIRST/FOLLOW table UI. */
    private FirstFollowTableManager firstFollowTableManager;

    /** Manager for the parse table UI. */
    private ParserTableManager parserTableManager;

    // ==========================================
    // Modular Handlers & State
    // ==========================================

    /** Central state holder for application analysis data. */
    private AnalysisState analysisState;

    /** Controller for UI state management (button enable/disable). */
    private UiStateController stateController;

    /** Handler for file loading operations. */
    private FileOperationsHandler fileHandler;

    /** Handler for lexer, parser, and validation execution. */
    private ExecutionHandler executionHandler;

    /** Handler for exporting analysis results. */
    private ExportHandler exportHandler;

    /** Handler for generating tree visualizations. */
    private VisualizationHandler visualizationHandler;

    /**
     * Initialises the UI controller after the FXML has been loaded.
     *
     * <p>This method performs the complete initialization of the application:
     *
     * <ol>
     *   <li><b>Basic UI Setup:</b> Maximizes the application window, redirects {@code System.out}
     *       and {@code System.err} to the console area
     *   <li><b>Initialize Managers:</b> Creates task executor, sets up symbol table columns
     *   <li><b>Initialize Modular Architecture:</b> Creates state container, state controller, and
     *       all handlers (file, execution, export, visualization)
     *   <li><b>Setup Table Managers:</b> Initializes FIRST/FOLLOW and parse table managers with
     *       their column configurations
     *   <li><b>Setup Listeners:</b>
     *       <ul>
     *         <li>Binds line numbers area scroll to input area scroll
     *         <li>Adds change listener to input area to invalidate analysis on user edits
     *       </ul>
     *   <li><b>Initial UI State:</b> Disables syntax analysis button until lexer runs, updates all
     *       UI controls based on initial state
     * </ol>
     *
     * <p>The programmatic change flag in {@link AnalysisState} prevents unnecessary invalidation
     * when the input area is updated programmatically (e.g., when loading a file).
     *
     * @param location the location used to resolve relative paths for the root object, or {@code
     *     null} if unknown (unused in this implementation)
     * @param resources the resources used to localize the root object, or {@code null} if not
     *     localized (unused in this implementation)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Basic UI Setup
        javafx.application.Platform.runLater(
                () -> {
                    ((javafx.stage.Stage) consoleArea.getScene().getWindow()).setMaximized(true);
                });

        PrintStream ps = new PrintStream(new UiUtils.TextAreaOutputStream(consoleArea), true);
        System.setOut(ps);
        System.setErr(ps);

        // 2. Initialize Managers
        taskExecutor =
                new BackgroundTaskExecutor(
                        loadingOverlay,
                        loadingLabel,
                        loadingTimeLabel,
                        consoleArea,
                        cancelOperationBtn);
        SymbolTableManager.setupColumns(
                symbolTableLineColumn,
                symbolTableColColumn,
                symbolTableLexemeColumn,
                symbolTableTokenTypeColumn);

        // 3. Initialize Modular Architecture
        analysisState = new AnalysisState();
        stateController = new UiStateController(this, analysisState);
        fileHandler = new FileOperationsHandler(this, analysisState, stateController);
        executionHandler = new ExecutionHandler(this, analysisState, stateController);
        exportHandler = new ExportHandler(this, analysisState);
        visualizationHandler = new VisualizationHandler(this, analysisState, stateController);

        firstFollowTableManager =
                new FirstFollowTableManager(
                        firstFollowTable, () -> analysisState.getCurrentFirstFollowTable());
        firstFollowTableManager.setupColumns(
                firstFollowTableNonTerminalCol,
                firstFollowTableFirstSetCol,
                firstFollowTableFollowSetCol);
        parserTableManager = new ParserTableManager(parserTable, parserTableNonTerminalCol);

        // 4. Setup Listeners
        lineNumbersArea.scrollTopProperty().bindBidirectional(inputArea.scrollTopProperty());
        inputArea
                .textProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            UiUtils.updateLineNumbers(inputArea, lineNumbersArea);
                            if (!analysisState.isProgrammaticChange() && !newVal.equals(oldVal)) {
                                stateController.invalidateInputState();
                                outputArea.setText("⚠️ Input changed. Please run Lexer again.");
                            }
                        });

        runSyntaxBtn.setDisable(true);
        cancelOperationBtn.setDisable(true);
        cancelOperationBtn.setVisible(false);
        stateController.updateUIState();
    }

    /**
     * Returns the background task executor for running long-running operations.
     *
     * @return the BackgroundTaskExecutor instance
     */
    public BackgroundTaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    /**
     * Handles the "Cancel Operation" button click.
     *
     * <p>Cancels any currently running background operation and resets the UI state.
     */
    @FXML
    private void handleCancelOperation() {
        if (taskExecutor != null) {
            taskExecutor.cancelCurrentTask();
        }
    }

    // ==========================================
    // FXML Event Delegates (Routing)
    // ==========================================

    /**
     * Handles the "Load Token File" button click.
     *
     * <p>This method delegates to {@link FileOperationsHandler#handleLoadTokenFile()} after
     * switching to the console tab to show progress feedback.
     *
     * <p><b>Note:</b> Unlike the previous behavior, loading a token file only stores the file path
     * and validates it. The actual lexer building is deferred until {@link #handleRunLexer()} is
     * called. This prevents unnecessary work and ensures the lexer is always built with the latest
     * token rules when needed.
     */
    @FXML
    private void handleLoadTokenFile() {
        mainTabPane.getSelectionModel().select(consoleTab);
        fileHandler.handleLoadTokenFile();
    }

    /**
     * Handles the "Load Grammar File" button click.
     *
     * <p>This method delegates to {@link FileOperationsHandler#handleLoadGrammarFile()} after
     * switching to the console tab to show progress feedback.
     */
    @FXML
    private void handleLoadGrammarFile() {
        mainTabPane.getSelectionModel().select(consoleTab);
        fileHandler.handleLoadGrammarFile();
    }

    /**
     * Handles the "Load Input File" button click.
     *
     * <p>This method delegates to {@link FileOperationsHandler#handleLoadInputFile()} after
     * switching to the console tab to show progress feedback.
     */
    @FXML
    private void handleLoadInputFile() {
        mainTabPane.getSelectionModel().select(consoleTab);
        fileHandler.handleLoadInputFile();
    }

    /**
     * Handles the "Run Lexer Analysis" button click.
     *
     * <p>This method delegates to {@link ExecutionHandler#handleRunLexer()} after switching to the
     * console tab to show progress feedback.
     *
     * <p><b>Behavior:</b>
     *
     * <ul>
     *   <li>If the lexer has not been built yet, it will be built from the loaded token file
     *   <li>If the token file has changed since the last lexer build, the lexer is rebuilt
     *   <li>If the lexer is already built and up-to-date, only scanning is performed
     * </ul>
     */
    @FXML
    private void handleRunLexer() {
        mainTabPane.getSelectionModel().select(consoleTab);
        executionHandler.handleRunLexer();
    }

    /**
     * Handles the "Run Syntax Analysis" button click.
     *
     * <p>This method delegates to {@link ExecutionHandler#handleRunSyntaxAnalysis()} after
     * switching to the console tab to show progress feedback.
     */
    @FXML
    private void handleRunSyntaxAnalysis() {
        mainTabPane.getSelectionModel().select(consoleTab);
        executionHandler.handleRunSyntaxAnalysis();
    }

    /**
     * Handles the "Validate Compatibility" button click.
     *
     * <p>This method delegates to {@link ExecutionHandler#handleValidateCompatibility()}. The
     * console tab is not automatically selected to allow the user to view validation results in the
     * validation output area.
     */
    @FXML
    private void handleValidateCompatibility() {
        executionHandler.handleValidateCompatibility();
    }

    /**
     * Handles the "Generate Grammar Tree" button click.
     *
     * <p>This method delegates to {@link VisualizationHandler#handleGenerateGrammarTree()} after
     * switching to the console tab to show progress feedback.
     */
    @FXML
    private void handleGenerateGrammarTree() {
        mainTabPane.getSelectionModel().select(consoleTab);
        visualizationHandler.handleGenerateGrammarTree();
    }

    /**
     * Handles the "Generate Input Tree" button click.
     *
     * <p>This method delegates to {@link VisualizationHandler#handleGenerateInputTree()} after
     * switching to the console tab to show progress feedback.
     */
    @FXML
    private void handleGenerateInputTree() {
        mainTabPane.getSelectionModel().select(consoleTab);
        visualizationHandler.handleGenerateInputTree();
    }

    /**
     * Handles the "Export Graph Image" button click.
     *
     * <p>This method delegates to {@link ExportHandler#handleExportGraphImage()}.
     */
    @FXML
    private void handleExportGraphImage() {
        exportHandler.handleExportGraphImage();
    }

    /**
     * Handles the "Export Grammar Tree Image" button click.
     *
     * <p>This method delegates to {@link ExportHandler#handleExportGrammarTreeImage()}.
     */
    @FXML
    private void handleExportGrammarTreeImage() {
        exportHandler.handleExportGrammarTreeImage();
    }

    /**
     * Handles the "Export Input Tree Image" button click.
     *
     * <p>This method delegates to {@link ExportHandler#handleExportInputTreeImage()}.
     */
    @FXML
    private void handleExportInputTreeImage() {
        exportHandler.handleExportInputTreeImage();
    }

    /**
     * Handles the "Export Tables (.csv)" button click.
     *
     * <p>This method delegates to {@link ExportHandler#handleExportCSV()}.
     */
    @FXML
    private void handleExportCSV() {
        exportHandler.handleExportCSV();
    }

    /**
     * Handles the "Export Graph Text" button click.
     *
     * <p>This method delegates to {@link ExportHandler#handleExportGraphText()}.
     */
    @FXML
    private void handleExportGraphText() {
        exportHandler.handleExportGraphText();
    }

    /**
     * Handles the "Save Console Log" button click.
     *
     * <p>This method delegates to {@link ExportHandler#handleSaveConsoleLog()}.
     */
    @FXML
    private void handleSaveConsoleLog() {
        exportHandler.handleSaveConsoleLog();
    }

    /**
     * Handles the "Save Output" button click.
     *
     * <p>This method delegates to {@link ExportHandler#handleSaveOutput()}.
     */
    @FXML
    private void handleSaveOutput() {
        exportHandler.handleSaveOutput();
    }

    /**
     * Handles the "Save Validation" button click.
     *
     * <p>This method delegates to {@link ExportHandler#handleSaveValidation()}.
     */
    @FXML
    private void handleSaveValidation() {
        exportHandler.handleSaveValidation();
    }

    /**
     * Handles the "Generate Full Report" button click.
     *
     * <p>This method delegates to {@link ExportHandler#handleGenerateFullReport()}.
     */
    @FXML
    private void handleGenerateFullReport() {
        exportHandler.handleGenerateFullReport();
    }

    /**
     * Handles the "Clear Tables" button click.
     *
     * <p>This method invalidates all analysis state (clearing FIRST/FOLLOW, parse table, symbol
     * table, and tree visualizations), then clears the output and console areas, and finally
     * updates the UI state to reflect the cleared data.
     */
    @FXML
    private void handleClearTables() {
        // Cancel any running operation first
        if (taskExecutor != null) {
            taskExecutor.cancelCurrentTask();
        }
        stateController.invalidateAnalysisState();
        outputArea.clear();
        consoleArea.clear();
        stateController.updateUIState();
    }

    /**
     * Handles the "Clear Console Log" button click.
     *
     * <p>Clears all text from the console log area.
     */
    @FXML
    private void handleClearConsoleLog() {
        consoleArea.clear();
    }

    /**
     * Handles the "Clear Output" button click.
     *
     * <p>Clears all text from the output area.
     */
    @FXML
    private void handleClearOutput() {
        outputArea.clear();
    }

    /**
     * Handles the "Clear Grammar Tree" button click.
     *
     * <p>This method delegates to {@link VisualizationHandler#handleClearGrammarTree()} to remove
     * the grammar tree visualization from the UI and free up memory.
     */
    @FXML
    private void handleClearGrammarTree() {
        visualizationHandler.handleClearGrammarTree();
    }

    /**
     * Handles the "Clear Input Tree" button click.
     *
     * <p>This method delegates to {@link VisualizationHandler#handleClearInputTree()} to remove the
     * input parse tree visualization from the UI and free up memory.
     */
    @FXML
    private void handleClearInputTree() {
        visualizationHandler.handleClearInputTree();
    }

    /**
     * Handles the "Clear Validation" button click.
     *
     * <p>This method clears the validation output area and marks validation data as absent in the
     * analysis state, then updates the UI to disable the clear validation button.
     */
    @FXML
    private void handleClearValidation() {
        validatorOutputArea.clear();
        analysisState.setHasValidationData(false);
        stateController.updateUIState();
    }
}
