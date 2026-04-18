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
import ui.core.table.FirstFollowTableManager;
import ui.core.table.ParserTableManager;
import ui.core.table.SymbolTableManager;
import ui.util.BackgroundTaskExecutor;
import ui.util.UiUtils;

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
        inputArea
                .textProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            UiUtils.updateLineNumbers(inputArea, lineNumbersArea);
                            if (!analysisState.isProgrammaticChange() && !newVal.equals(oldVal)) {
                                stateController.invalidateAnalysisState();
                                outputArea.setText("⚠️ Input changed. Please run Lexer again.");
                            }
                        });

        runSyntaxBtn.setDisable(true);
        stateController.updateUIState();
    }

    // ==========================================
    // FXML Event Delegates (Routing)
    // ==========================================
    @FXML
    private void handleLoadTokenFile() {
        fileHandler.handleLoadTokenFile();
    }

    @FXML
    private void handleLoadGrammarFile() {
        fileHandler.handleLoadGrammarFile();
    }

    @FXML
    private void handleLoadInputFile() {
        fileHandler.handleLoadInputFile();
    }

    @FXML
    private void handleRunLexer() {
        executionHandler.handleRunLexer();
    }

    @FXML
    private void handleRunSyntaxAnalysis() {
        executionHandler.handleRunSyntaxAnalysis();
    }

    @FXML
    private void handleValidateCompatibility() {
        executionHandler.handleValidateCompatibility();
    }

    @FXML
    private void handleGenerateGrammarTree() {
        visualizationHandler.handleGenerateGrammarTree();
    }

    @FXML
    private void handleGenerateInputTree() {
        visualizationHandler.handleGenerateInputTree();
    }

    @FXML
    private void handleExportGraphImage() {
        exportHandler.handleExportGraphImage();
    }

    @FXML
    private void handleExportGrammarTreeImage() {
        exportHandler.handleExportGrammarTreeImage();
    }

    @FXML
    private void handleExportInputTreeImage() {
        exportHandler.handleExportInputTreeImage();
    }

    @FXML
    private void handleExportCSV() {
        exportHandler.handleExportCSV();
    }

    @FXML
    private void handleExportGraphText() {
        exportHandler.handleExportGraphText();
    }

    @FXML
    private void handleSaveConsoleLog() {
        exportHandler.handleSaveConsoleLog();
    }

    @FXML
    private void handleSaveOutput() {
        exportHandler.handleSaveOutput();
    }

    @FXML
    private void handleSaveValidation() {
        exportHandler.handleSaveValidation();
    }

    @FXML
    private void handleGenerateFullReport() {
        exportHandler.handleGenerateFullReport();
    }

    @FXML
    private void handleClearTables() {
        stateController.invalidateAnalysisState();
        outputArea.clear();
        consoleArea.clear();
        stateController.updateUIState();
    }

    @FXML
    private void handleClearConsoleLog() {
        consoleArea.clear();
    }

    @FXML
    private void handleClearOutput() {
        outputArea.clear();
    }

    @FXML
    private void handleClearValidation() {
        validatorOutputArea.clear();
        analysisState.setHasValidationData(false);
        stateController.updateUIState();
    }
}
