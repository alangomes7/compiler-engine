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
 * Main JavaFX controller for the Compiler UI. Manages all UI components, coordinates between
 * services and handlers, and maintains the application state.
 *
 * @author Generated
 * @version 1.0
 */
@Getter
public class Ui implements Initializable {

    // ==========================================
    // FXML Injections (all interactive components)
    // ==========================================
    // Buttons
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
    @FXML private Button generateInputTreeBtn;
    @FXML private Button exportInputTreeBtn;
    @FXML private Button saveValidationBtn;
    @FXML private Button clearValidationBtn;

    // Text areas & labels
    @FXML private TextArea lineNumbersArea;
    @FXML private TextArea inputArea;
    @FXML private TextArea outputArea;
    @FXML private TextArea consoleArea;
    @FXML private Label tokenFileLabel;
    @FXML private Label grammarFileLabel;
    @FXML private Label inputFileLabel;
    @Getter @FXML private TextArea automataDetailsArea;
    @FXML private TextArea validatorOutputArea;

    // Tabs
    @FXML private TabPane mainTabPane;
    @FXML private Tab consoleTab;

    // Tables
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

    // Containers for graphs/trees
    @Getter @FXML private javafx.scene.layout.BorderPane interactiveGraphContainer;
    @FXML private javafx.scene.layout.BorderPane grammarTreeContainer;
    @FXML private javafx.scene.layout.BorderPane inputTreeContainer;

    // Overlay
    @FXML private VBox loadingOverlay;
    @FXML private Label loadingLabel;
    @FXML private Label loadingTimeLabel;

    // ==========================================
    // Services
    // ==========================================
    private final LexerService lexerService = new LexerService();
    private final ParserService parserService = new ParserService();
    private BackgroundTaskExecutor taskExecutor;
    private FirstFollowTableManager firstFollowTableManager;
    private ParserTableManager parserTableManager;

    // ==========================================
    // Modular Handlers & State
    // ==========================================
    private AnalysisState analysisState;
    private UiStateController stateController;
    private FileOperationsHandler fileHandler;
    private ExecutionHandler executionHandler;
    private ExportHandler exportHandler;
    private VisualizationHandler visualizationHandler;

    /**
     * Initialises the UI controller after the FXML has been loaded. Sets up the window to be
     * maximised, redirects System.out/err to the console area, initialises table managers,
     * handlers, and sets up listeners.
     *
     * @param location the location used to resolve relative paths (unused)
     * @param resources the resources used to localise the root object (unused)
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
                        loadingOverlay, loadingLabel, loadingTimeLabel, consoleArea);
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
        stateController.updateUIState();
    }

    // ==========================================
    // FXML Event Delegates (Routing)
    // ==========================================

    /**
     * Handles the "Load Token File" button click. Delegates to {@link
     * FileOperationsHandler#handleLoadTokenFile()}.
     */
    @FXML
    private void handleLoadTokenFile() {
        mainTabPane.getSelectionModel().select(consoleTab);
        fileHandler.handleLoadTokenFile();
    }

    /**
     * Handles the "Load Grammar File" button click. Delegates to {@link
     * FileOperationsHandler#handleLoadGrammarFile()}.
     */
    @FXML
    private void handleLoadGrammarFile() {
        mainTabPane.getSelectionModel().select(consoleTab);
        fileHandler.handleLoadGrammarFile();
    }

    /**
     * Handles the "Load Input File" button click. Delegates to {@link
     * FileOperationsHandler#handleLoadInputFile()}.
     */
    @FXML
    private void handleLoadInputFile() {
        mainTabPane.getSelectionModel().select(consoleTab);
        fileHandler.handleLoadInputFile();
    }

    /**
     * Handles the "Run Lexer Analysis" button click. Delegates to {@link
     * ExecutionHandler#handleRunLexer()}.
     */
    @FXML
    private void handleRunLexer() {
        mainTabPane.getSelectionModel().select(consoleTab);
        executionHandler.handleRunLexer();
    }

    /**
     * Handles the "Run Syntax Analysis" button click. Delegates to {@link
     * ExecutionHandler#handleRunSyntaxAnalysis()}.
     */
    @FXML
    private void handleRunSyntaxAnalysis() {
        mainTabPane.getSelectionModel().select(consoleTab);
        executionHandler.handleRunSyntaxAnalysis();
    }

    /**
     * Handles the "Validate Compatibility" button click. Delegates to {@link
     * ExecutionHandler#handleValidateCompatibility()}.
     */
    @FXML
    private void handleValidateCompatibility() {
        executionHandler.handleValidateCompatibility();
    }

    /**
     * Handles the "Generate Grammar Tree" button click. Delegates to {@link
     * VisualizationHandler#handleGenerateGrammarTree()}.
     */
    @FXML
    private void handleGenerateGrammarTree() {
        mainTabPane.getSelectionModel().select(consoleTab);
        visualizationHandler.handleGenerateGrammarTree();
    }

    /**
     * Handles the "Generate Input Tree" button click. Delegates to {@link
     * VisualizationHandler#handleGenerateInputTree()}.
     */
    @FXML
    private void handleGenerateInputTree() {
        mainTabPane.getSelectionModel().select(consoleTab);
        visualizationHandler.handleGenerateInputTree();
    }

    /**
     * Handles the "Export Graph Image" button click. Delegates to {@link
     * ExportHandler#handleExportGraphImage()}.
     */
    @FXML
    private void handleExportGraphImage() {
        exportHandler.handleExportGraphImage();
    }

    /**
     * Handles the "Export Grammar Tree Image" button click. Delegates to {@link
     * ExportHandler#handleExportGrammarTreeImage()}.
     */
    @FXML
    private void handleExportGrammarTreeImage() {
        exportHandler.handleExportGrammarTreeImage();
    }

    /**
     * Handles the "Export Input Tree Image" button click. Delegates to {@link
     * ExportHandler#handleExportInputTreeImage()}.
     */
    @FXML
    private void handleExportInputTreeImage() {
        exportHandler.handleExportInputTreeImage();
    }

    /**
     * Handles the "Export Tables (.csv)" button click. Delegates to {@link
     * ExportHandler#handleExportCSV()}.
     */
    @FXML
    private void handleExportCSV() {
        exportHandler.handleExportCSV();
    }

    /**
     * Handles the "Export Graph Text" button click. Delegates to {@link
     * ExportHandler#handleExportGraphText()}.
     */
    @FXML
    private void handleExportGraphText() {
        exportHandler.handleExportGraphText();
    }

    /**
     * Handles the "Save Console Log" button click. Delegates to {@link
     * ExportHandler#handleSaveConsoleLog()}.
     */
    @FXML
    private void handleSaveConsoleLog() {
        exportHandler.handleSaveConsoleLog();
    }

    /**
     * Handles the "Save Output" button click. Delegates to {@link
     * ExportHandler#handleSaveOutput()}.
     */
    @FXML
    private void handleSaveOutput() {
        exportHandler.handleSaveOutput();
    }

    /**
     * Handles the "Save Validation" button click. Delegates to {@link
     * ExportHandler#handleSaveValidation()}.
     */
    @FXML
    private void handleSaveValidation() {
        exportHandler.handleSaveValidation();
    }

    /**
     * Handles the "Generate Full Report" button click. Delegates to {@link
     * ExportHandler#handleGenerateFullReport()}.
     */
    @FXML
    private void handleGenerateFullReport() {
        exportHandler.handleGenerateFullReport();
    }

    /**
     * Handles the "Clear Tables" button click. Invalidates all analysis state and clears
     * output/console.
     */
    @FXML
    private void handleClearTables() {
        stateController.invalidateAnalysisState();
        outputArea.clear();
        consoleArea.clear();
        stateController.updateUIState();
    }

    /** Handles the "Clear Console Log" button click. */
    @FXML
    private void handleClearConsoleLog() {
        consoleArea.clear();
    }

    /** Handles the "Clear Output" button click. */
    @FXML
    private void handleClearOutput() {
        outputArea.clear();
    }

    /**
     * Handles the "Clear Validation" button click. Clears the validation output area and marks
     * validation data as absent.
     */
    @FXML
    private void handleClearValidation() {
        validatorOutputArea.clear();
        analysisState.setHasValidationData(false);
        stateController.updateUIState();
    }
}
