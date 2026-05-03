package ui;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import core.lexer.models.atomic.Token;
import core.parser.models.atomic.Symbol;
import java.io.PrintStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.slf4j.LoggerFactory;
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

@Getter
public class Ui implements Initializable {

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

    @FXML private TextArea lineNumbersArea;
    @FXML private TextArea inputArea;
    @FXML private TextArea outputArea;
    @FXML private TextArea consoleArea;
    @FXML private Label tokenFileLabel;
    @FXML private Label grammarFileLabel;
    @FXML private Label inputFileLabel;
    @FXML private Label cursorPositionLabel;
    @FXML private Label parserModeLabel;
    @Getter @FXML private TextArea automataDetailsArea;
    @FXML private TextArea validatorOutputArea;

    @FXML private ComboBox<String> parserComboBox;
    @FXML private ComboBox<String> userModeComboBox;
    @FXML private ComboBox<String> logModeComboBox;
    @FXML private TabPane mainTabPane;
    @FXML private Tab consoleTab;
    @FXML private Tab outputTab;
    @FXML private Tab lexerViewerTab;
    @FXML private Tab validatorTab;
    @FXML private Tab symbolTableTab;
    @FXML private Tab firstFollowTab;
    @FXML private Tab parseTableTab;
    @FXML private Tab syntaxTreeTab;
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

    @Getter @FXML private javafx.scene.layout.BorderPane interactiveGraphContainer;
    @FXML private javafx.scene.layout.BorderPane grammarTreeContainer;
    @FXML private javafx.scene.layout.BorderPane inputTreeContainer;

    @FXML private VBox loadingOverlay;
    @FXML private ProgressIndicator mainSpinner;
    @FXML private Label loadingLabel;
    @FXML private Label loadingTimeLabel;

    private final LexerService lexerService = new LexerService();

    private final ParserService parserService = new ParserService();

    private BackgroundTaskExecutor taskExecutor;

    private FirstFollowTableManager firstFollowTableManager;

    private ParserTableManager parserTableManager;

    private AnalysisState analysisState;

    private UiStateController stateController;

    private FileOperationsHandler fileHandler;

    private ExecutionHandler executionHandler;

    private ExportHandler exportHandler;

    private VisualizationHandler visualizationHandler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Basic UI Setup
        PrintStream ps = new PrintStream(new UiUtils.TextAreaOutputStream(consoleArea), true);
        System.setOut(ps);
        System.setErr(ps);

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

        userModeComboBox.getItems().addAll("Client", "Developer");
        userModeComboBox
                .valueProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            updateTabVisibility(newVal);
                            updateButtonsVisibility(newVal);
                            refreshTextOutputs();
                        });
        userModeComboBox.setValue("Client");
        logModeComboBox.getItems().addAll("TRACE", "DEBUG", "INFO", "WARN", "ERROR");

        logModeComboBox
                .valueProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            LoggerContext loggerContext =
                                    (LoggerContext) LoggerFactory.getILoggerFactory();

                            ch.qos.logback.classic.Logger rootLogger =
                                    loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

                            rootLogger.setLevel(Level.toLevel(newVal.toUpperCase()));

                            System.out.println("Runtime log level preference set to: " + newVal);
                        });
        logModeComboBox.setValue("INFO");
        clearConsoleLogBtn.setDisable(false);
        saveConsoleLogBtn.setDisable(false);

        parserComboBox.getItems().addAll("LL(1)", "Recursive Descent", "Backtracking");
        parserComboBox.setValue("Backtracking");

        inputArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> {
        updateCursorPosition();
    });
    }

    private void updateTabVisibility(String mode) {
        mainTabPane.getTabs().clear();

        if ("Client".equals(mode)) {
            mainTabPane.getTabs().addAll(outputTab, validatorTab);
        } else {
            mainTabPane
                    .getTabs()
                    .addAll(
                            consoleTab,
                            outputTab,
                            lexerViewerTab,
                            validatorTab,
                            symbolTableTab,
                            firstFollowTab,
                            parseTableTab,
                            syntaxTreeTab);
        }
    }

    private void updateButtonsVisibility(String mode) {
        boolean isClient = "Client".equals(mode);

        clearTablesBtn.setVisible(!isClient);
        exportCsvBtn.setVisible(!isClient);
        generateReportBtn.setVisible(!isClient);
    }

    private void updateCursorPosition() {
    int caretPos = inputArea.getCaretPosition();
    String text = inputArea.getText();

    if (text == null || text.isEmpty() || caretPos < 0) {
        cursorPositionLabel.setText("  Line: 1, Col: 1");
        return;
    }

    // Prevent OutOfBounds if the caret is somehow beyond the text length
    caretPos = Math.min(caretPos, text.length());

    // Extract text up to the current cursor position
    String textBeforeCaret = text.substring(0, caretPos);
    
    // Line number is the number of newlines + 1
    int line = textBeforeCaret.split("\n", -1).length;
    
    // Column number is the distance from the last newline to the caret
    int lastNewLineIndex = textBeforeCaret.lastIndexOf('\n');
    int col = caretPos - lastNewLineIndex;

    cursorPositionLabel.setText(String.format("  Line: %d, Col: %d", line, col));
}

    public void refreshTextOutputs() {
        String mode = userModeComboBox.getValue();
        boolean isDeveloper = "Developer".equals(mode);

        if (analysisState.isHasValidationData()) {
            if (isDeveloper) {
                validatorOutputArea.setText(
                        analysisState.getValidationClassificationReport()
                                + "\n"
                                + analysisState.getValidationCompatibilityReport());
            } else {
                validatorOutputArea.setText(analysisState.getValidationClassificationReport());
            }
        }

        if (analysisState.getCurrentParseResult() != null) {
            boolean hasErrors = !analysisState.getCurrentParseResult().errors.isEmpty();
            StringBuilder sb = new StringBuilder(analysisState.getSyntaxBaseOutput());

            if (isDeveloper || !hasErrors) {
                sb.append(analysisState.getSyntaxTreeOutput());
            }
            outputArea.setText(sb.toString());

            if (!isDeveloper && hasErrors && analysisState.isHasInputTree()) {
                inputTreeContainer.setCenter(null);
                analysisState.setHasInputTree(false);
            }
        }
    }

    public BackgroundTaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    @FXML
    private void handleCancelOperation() {
        if (taskExecutor != null) {
            taskExecutor.cancelCurrentTask();
        }
    }

    @FXML
    private void handleLoadTokenFile() {
        mainTabPane.getSelectionModel().select(consoleTab);
        fileHandler.handleLoadTokenFile();
    }

    @FXML
    private void handleLoadGrammarFile() {
        mainTabPane.getSelectionModel().select(consoleTab);
        fileHandler.handleLoadGrammarFile();
    }

    @FXML
    private void handleLoadInputFile() {
        mainTabPane.getSelectionModel().select(consoleTab);
        fileHandler.handleLoadInputFile();
    }

    @FXML
    private void handleRunLexer() {
        mainTabPane.getSelectionModel().select(consoleTab);
        executionHandler.handleRunLexer();
    }

    @FXML
    private void handleRunSyntaxAnalysis() {
        mainTabPane.getSelectionModel().select(consoleTab);
        executionHandler.handleRunSyntaxAnalysis();
    }

    @FXML
    private void handleValidateCompatibility() {
        executionHandler.handleValidateCompatibility();
    }

    @FXML
    private void handleGenerateGrammarTree() {
        mainTabPane.getSelectionModel().select(consoleTab);
        visualizationHandler.handleGenerateGrammarTree();
    }

    @FXML
    private void handleGenerateInputTree() {
        mainTabPane.getSelectionModel().select(consoleTab);
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
        // Cancel any running operation first
        if (taskExecutor != null) {
            taskExecutor.cancelCurrentTask();
        }
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
    private void handleClearGrammarTree() {
        visualizationHandler.handleClearGrammarTree();
    }

    @FXML
    private void handleClearInputTree() {
        visualizationHandler.handleClearInputTree();
    }

    @FXML
    private void handleClearValidation() {
        validatorOutputArea.clear();
        analysisState.setValidationClassificationReport("");
        analysisState.setValidationCompatibilityReport("");
        analysisState.setHasValidationData(false);
        stateController.updateUIState();
    }
}
