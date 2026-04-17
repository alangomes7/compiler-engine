package ui;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import core.lexer.models.atomic.Token;
import core.lexer.models.automata.AFD;
import core.parser.core.grammar.GrammarClassification;
import core.parser.models.FirstFollowTable;
import core.parser.models.ParseTable;
import core.parser.models.tree.ParseTree;
import core.parser.models.atomic.Symbol;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import lombok.Getter;
import ui.core.graph.automata.AutomataVisualizer;
import ui.core.graph.automata.InteractiveAutomataView;
import ui.core.graph.tree.InteractiveTreeView;
import ui.core.services.FileService;
import ui.core.services.LexerService;
import ui.core.services.ParserService;
import ui.table.FirstFollowTableManager;
import static ui.table.FirstFollowTableManager.exportFirstFollowCsv;
import ui.table.ParserTableManager;
import static ui.table.ParserTableManager.exportParseTableCsv;
import ui.table.SymbolTableManager;
import static ui.table.SymbolTableManager.exportSymbolTableCsv;
import ui.util.BackgroundTaskExecutor;
import ui.util.UiUtils;

public class Ui {

    // --- FXML UI Components ---
    @FXML private VBox loadingOverlay;
    @FXML private Label loadingLabel;
    @FXML private Label loadingTimeLabel;

    @FXML private TextArea lineNumbersArea;
    @FXML private TextArea inputArea;
    @FXML private TextArea outputArea;
    @FXML private TextArea consoleArea;

    @FXML private Label tokenFileLabel;
    @FXML private Label grammarFileLabel;
    @FXML private Label inputFileLabel;

    @Getter @FXML private TextArea automataDetailsArea;
    @Getter @FXML private javafx.scene.layout.BorderPane interactiveGraphContainer;

    // Symbol Table
    @FXML private TableView<Token> symbolTableViewer;
    @FXML private TableColumn<Token, Integer> symbolTableLineColumn;
    @FXML private TableColumn<Token, Integer> symbolTableColColumn;
    @FXML private TableColumn<Token, String> symbolTableLexemeColumn;
    @FXML private TableColumn<Token, String> symbolTableTokenTypeColumn;

    // First Follow Table
    @Getter @FXML private TableView<Symbol> firstFollowTable;
    @FXML private TableColumn<Symbol, String> firstFollowTableNonTerminalCol;
    @FXML private TableColumn<Symbol, java.util.Set<Symbol>> firstFollowTableFirstSetCol;
    @FXML private TableColumn<Symbol, java.util.Set<Symbol>> firstFollowTableFollowSetCol;

    // Parser Table
    @FXML private TableView<Symbol> parserTable;
    @FXML private TableColumn<Symbol, String> parserTableNonTerminalCol;

    // Grammar classification area
    @FXML private TextArea grammarClassificationArea;
    @FXML private javafx.scene.layout.BorderPane interactiveTreeContainer;

    // --- Services ---
    private final LexerService lexerService = new LexerService();
    private final ParserService parserService = new ParserService();

    // --- Managers / Helpers ---
    private BackgroundTaskExecutor taskExecutor;
    private FirstFollowTableManager firstFollowTableManager;
    private ParserTableManager parserTableManager;

    // Current data references
    private FirstFollowTable currentFirstFollowTable;
    private ParseTable currentParseTable;

    @FXML
    public void initialize() {
        // Redirect System.out/err to console
        PrintStream ps = new PrintStream(new UiUtils.TextAreaOutputStream(consoleArea), true);
        System.setOut(ps);
        System.setErr(ps);

        // Synchronize line numbers
        inputArea.textProperty().addListener((obs, old, newVal) ->
                UiUtils.updateLineNumbers(inputArea, lineNumbersArea));

        // Initialize helpers
        taskExecutor = new BackgroundTaskExecutor(loadingOverlay, loadingLabel, loadingTimeLabel, consoleArea);
        SymbolTableManager.setupColumns(symbolTableLineColumn, symbolTableColColumn,
                symbolTableLexemeColumn, symbolTableTokenTypeColumn);
        firstFollowTableManager = new FirstFollowTableManager(firstFollowTable, () -> currentFirstFollowTable);
        firstFollowTableManager.setupColumns(firstFollowTableNonTerminalCol,
                firstFollowTableFirstSetCol, firstFollowTableFollowSetCol);
        parserTableManager = new ParserTableManager(parserTable, parserTableNonTerminalCol);
    }

    // --- Action Handlers ---

    @FXML
    private void handleLoadTokenFile() {
        File file = FileService.selectFile(inputArea.getScene().getWindow(),
                "Select Lexer Rules", "*.txt", "*.lexer");
        if (file == null) return;

        tokenFileLabel.setText(file.getName());
        invalidateParserState();

        taskExecutor.execute("Building Lexer...",
                log -> {
                    AFD automaton = null;
            try {
                automaton = lexerService.buildLexer(file.getAbsolutePath(), log);
            } catch (Exception ex) {
                System.getLogger(Ui.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
                    log.accept("Creating Lexer Automaton Image...");
                    AutomataVisualizer.exportToImage(automaton, "lexer_automata.png");
                    return automaton;
                },
                automaton -> {
                    automataDetailsArea.setText(automaton.toString());
                    interactiveGraphContainer.setCenter(new InteractiveAutomataView(automaton));
                    outputArea.setText("Lexer successfully built.");
                },
                err -> outputArea.setText("Error: " + err.getMessage())
        );
    }

    @FXML
    private void handleLoadGrammarFile() {
        File file = FileService.selectFile(inputArea.getScene().getWindow(),
                "Select Grammar", "*.txt", "*.grammar");
        if (file == null) return;

        grammarFileLabel.setText(file.getName());
        invalidateParserState();

        taskExecutor.execute("Loading Grammar...",
                log -> {
            try {
                parserService.loadGrammar(file.getAbsolutePath());
            } catch (Exception ex) {
                System.getLogger(Ui.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
                    log.accept("Grammar loaded.");
                    return null;
                },
                res -> outputArea.setText("Grammar loaded."),
                err -> outputArea.setText("Error: " + err.getMessage())
        );
    }

    @FXML
    private void handleLoadInputFile() {
        File file = FileService.selectFile(inputArea.getScene().getWindow(), "Select Input File");
        if (file == null) return;

        inputFileLabel.setText(file.getName());
        invalidateInputState();

        taskExecutor.execute("Loading File...",
                log -> {
                    log.accept("📂 Reading input file...");
                    String content = null;
            try {
                content = FileService.readFileContent(file);
            } catch (Exception ex) {
                System.getLogger(Ui.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
                    log.accept("✅ File loaded.");
                    return content;
                },
                content -> inputArea.setText(content),
                err -> outputArea.setText("❌ Load Error: " + err.getMessage())
        );
    }

    @FXML
    private void handleRunLexer() {
        if (!lexerService.isInitialized()) {
            outputArea.setText("⚠️ Load a token file first.");
            return;
        }

        final String inputText = inputArea.getText();
        taskExecutor.execute("Scanning...",
                log -> {
                    log.accept("🔍 Scanning input...");
                    String result = lexerService.scan(inputText);
                    log.accept("✅ Scan complete.");
                    return result;
                },
                msg -> {
                    outputArea.setText(msg);
                    symbolTableViewer.setItems(FXCollections.observableArrayList(lexerService.getSymbolTable()));
                },
                err -> outputArea.setText("❌ Lexical Error: " + err.getMessage())
        );
    }

    @FXML
    private void handleRunSyntaxAnalysis() {
        if (!parserService.isGrammarLoaded() || !lexerService.isInitialized()) {
            outputArea.setText("⚠️ Load Grammar and run Lexer first.");
            return;
        }

        taskExecutor.execute("Populating FirstFollowTable",
                log -> parserService.buildFirstFollowTable(),
                firstFollowResult -> {
                    this.currentFirstFollowTable = firstFollowResult;
                    List<Symbol> nonTerminals = List.copyOf(currentFirstFollowTable.getAllFirstSets().keySet());
                    firstFollowTable.setItems(FXCollections.observableArrayList(nonTerminals));
                    outputArea.setText("✅ First/Follow computed.");

                    // Second step: build parse table
                    taskExecutor.execute("Building Parse Table",
                            log -> parserService.buildParseTable(this.currentFirstFollowTable,
                                    this.lexerService.getSymbolTable()),
                            parseTableResult -> {
                                this.currentParseTable = parseTableResult;
                                parserTableManager.populate(parseTableResult);
                                outputArea.setText(outputArea.getText() + "\n✅ Parse Table generated.");

                                // Third step: run parser
                                try {
                                    ParseTree tree = parserService.parseTokens(this.currentParseTable,
                                            this.lexerService.getSymbolTable());
                                    interactiveTreeContainer.setCenter(new InteractiveTreeView(tree.getRoot()));
                                    outputArea.setText(outputArea.getText() + "\n✅ Derivation Tree successfully generated.");
                                } catch (Exception e) {
                                    outputArea.setText(outputArea.getText() + "\n❌ Syntax Error during parsing:\n" + e.getMessage());
                                    interactiveTreeContainer.setCenter(null);
                                }
                            },
                            error -> {
                                this.currentParseTable = null;
                                this.currentFirstFollowTable = null;
                                outputArea.setText(outputArea.getText() + "\n❌ Parse Table Error:\n" + error.getMessage());
                            }
                    );
                },
                error -> outputArea.setText("❌ FirstFollow Table Error:\n" + error.getMessage())
        );
    }

    @FXML
    private void handleClearTables() {
        symbolTableViewer.getItems().clear();
        firstFollowTable.getItems().clear();
        parserTableManager.clear();
        currentFirstFollowTable = null;
        currentParseTable = null;
        interactiveTreeContainer.setCenter(null);
        outputArea.clear();
        consoleArea.clear();
        grammarClassificationArea.clear();
    }

    @FXML
    private void handleExportGraphImage() {
        if (!(interactiveGraphContainer.getCenter() instanceof InteractiveAutomataView view)) {
            outputArea.setText("No interactive graph available to export.");
            return;
        }
        var snapshot = view.generateSnapshot();
        taskExecutor.execute("Exporting graph image...",
                log -> {
                    log.accept("Converting to high resolution PNG...");
                    String path = "output/graph.png";
            try {
                UiUtils.saveSnapshot(snapshot, new File(path));
            } catch (IOException ex) {
                System.getLogger(Ui.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
                    return path;
                },
                path -> outputArea.setText("✅ Graph exported to " + path),
                err -> outputArea.setText("❌ Export failed: " + err.getMessage())
        );
    }

    @FXML
    private void handleExportTreeImage() {
        if (!(interactiveTreeContainer.getCenter() instanceof InteractiveTreeView view)) {
            outputArea.setText("No interactive tree available to export.");
            return;
        }
        var snapshot = view.generateSnapshot();
        taskExecutor.execute("Exporting tree image...",
                log -> {
                    log.accept("Converting syntax tree to PNG...");
                    String path = "output/syntax_tree.png";
            try {
                UiUtils.saveSnapshot(snapshot, new File(path));
            } catch (IOException ex) {
                System.getLogger(Ui.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
                    return path;
                },
                path -> outputArea.setText("✅ Syntax tree exported to " + path),
                err -> outputArea.setText("❌ Export failed: " + err.getMessage())
        );
    }

    @FXML
    private void handleSaveConsoleLog() {
        System.out.println("Saving console log... (Feature not yet implemented)");
    }

    @FXML
    private void handleExportCSV() {
        taskExecutor.execute("Exporting to CSV...",
                log -> {
                    // Create output directory if it doesn't exist
                    java.io.File outputDir = new java.io.File("output");
                    if (!outputDir.exists()) {
                        outputDir.mkdirs();
                    }
                    
                    try {
                        log.accept("Exporting Symbol Table...");
                        exportSymbolTableCsv("output/symbol_table.csv", symbolTableViewer);
                        
                        log.accept("Exporting First/Follow Table...");
                        exportFirstFollowCsv("output/first_follow_table.csv", currentFirstFollowTable, firstFollowTable);
                        
                        log.accept("Exporting Parse Table...");
                        exportParseTableCsv("output/parse_table.csv", currentParseTable, parserTable);
                        
                        return outputDir.getAbsolutePath();
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to write CSV files: " + ex.getMessage(), ex);
                    }
                },
                path -> outputArea.setText("✅ Tables successfully exported to: " + path),
                err -> outputArea.setText("❌ CSV Export failed: " + err.getMessage())
        );
    }

    @FXML
    private void handleClassifyGrammar() {
        taskExecutor.execute("Classifying grammar...",
                log -> (GrammarClassification) parserService.classifyGrammarWithParserTable(this.currentParseTable),
                result -> {
                    grammarClassificationArea.setText(result.toString());
                    outputArea.setText(result.toString());
                },
                err -> outputArea.setText(err.getMessage())
        );
    }

    @FXML
    private void handleClearGrammarClassification() {
        grammarClassificationArea.clear();
    }

    // --- State invalidation helpers ---

    private void invalidateParserState() {
        parserTableManager.clear();
        currentParseTable = null;
        firstFollowTable.getItems().clear();
        currentFirstFollowTable = null;
        grammarClassificationArea.clear();
        invalidateInputState();
    }

    private void invalidateInputState() {
        symbolTableViewer.getItems().clear();
        interactiveTreeContainer.setCenter(null);
    }
}