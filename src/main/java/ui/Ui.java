package ui;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import javafx.util.Duration;
import parser.RecursiveDescentParser;
import parser.grammar.FirstFollowCalculator;
import parser.grammar.GrammarBuilder;
import parser.models.FirstFollowRow;
import parser.models.Grammar;
import parser.models.SyntaxTreeNode;
import parser.models.atomic.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import Utils.Utils;
import core.lexer.Lexer;
import core.lexer.models.SymbolTable;
import core.lexer.models.atomic.Token;
import core.lexer.translators.RuleReader;
import graph.AutomataVisualizer;
import graph.InteractiveAutomataView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class Ui {

    @FXML private VBox loadingOverlay;
    @FXML private Label loadingLabel;
    @FXML private Label timerLabel;
    
    @FXML private TextArea lineNumbersArea;
    @FXML private TextArea inputArea;
    @FXML private TextArea outputArea;
    @FXML private TextArea consoleArea;
    @FXML private Label tokenFileLabel;
    @FXML private Label inputFileLabel;
    @FXML private Button loadGrammarBtn;
    @FXML private Label grammarFileLabel;
    @FXML private Button loadTokenBtn;
    @FXML private Button loadInputBtn;
    @FXML private Button runLexerBtn;
    @FXML private Button runSyntaxBtn;
    @FXML private TextArea automataDetailsArea;
    @FXML private javafx.scene.layout.BorderPane interactiveGraphContainer;
    
    @FXML private TableView<Symbol> symbolTableViewer;
    @FXML private TableColumn<Symbol, Integer> lineColumn;
    @FXML private TableColumn<Symbol, Integer> colColumn;
    @FXML private TableColumn<Symbol, String> lexemeColumn;
    @FXML private TableColumn<Symbol, String> tokenTypeColumn;

    @FXML private TableView<FirstFollowRow> firstFollowTable;
    @FXML private TableColumn<FirstFollowRow, Integer> ffLineCol;
    @FXML private TableColumn<FirstFollowRow, Symbol> nonTerminalCol;
    @FXML private TableColumn<FirstFollowRow, List<Symbol>> firstSetCol;
    @FXML private TableColumn<FirstFollowRow, List<Symbol>> followSetCol;

    @FXML private TreeView<String> syntaxTreeView;

    private Lexer lexer;
    private Grammar currentGrammar;

    @FXML
    public void initialize() {
        PrintStream ps = new PrintStream(new TextAreaOutputStream(consoleArea), true);
        System.setOut(ps);
        System.setErr(ps);

        lineNumbersArea.scrollTopProperty().bindBidirectional(inputArea.scrollTopProperty());

        inputArea.textProperty().addListener((obs, oldVal, newVal) -> updateLineNumbers());
        updateLineNumbers(); 

        setupFirstFollowTable();
        setupSymbolTable();
    }

    private <T> void executeHeavyTask(String loadingMsg, 
                                      Callable<T> backgroundTask, 
                                      Consumer<T> onSuccess, 
                                      Consumer<Exception> onError) {
        loadingLabel.setText(loadingMsg);
        timerLabel.setText("0.000s");
        loadingOverlay.setVisible(true);
        setButtonsDisabled(true);

        long startTime = System.currentTimeMillis();

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(50), e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                timerLabel.setText(String.format("%.3fs", elapsed / 1000.0));
            })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        new Thread(() -> {
            try {
                T result = backgroundTask.call();
                Platform.runLater(() -> {
                    try {
                        onSuccess.accept(result);
                    } finally {
                        timeline.stop(); 
                        loadingOverlay.setVisible(false);
                        setButtonsDisabled(false);
                        long totalTime = System.currentTimeMillis() - startTime;
                        consoleArea.appendText(String.format("\n[%s] completed in %d ms\n", loadingMsg, totalTime));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    try {
                        onError.accept(e);
                    } finally {
                        timeline.stop(); 
                        loadingOverlay.setVisible(false);
                        setButtonsDisabled(false);
                    }
                });
            }
        }).start();
    }
    
    private void updateLineNumbers() {
        int lines = inputArea.getText().split("\n", -1).length;
        StringBuilder numbers = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            numbers.append(i).append("\n");
        }
        lineNumbersArea.setText(numbers.toString());
    }

    private void setupFirstFollowTable() {
        nonTerminalCol.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getNonTerminal()));
            
        ffLineCol.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getNonTerminal().getLine()).asObject());
            
        firstSetCol.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getFirstSet()));
            
        followSetCol.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getFollowSet()));

        nonTerminalCol.setCellFactory(column -> new TableCell<FirstFollowRow, Symbol>() {
            @Override protected void updateItem(Symbol item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getLexeme());
            }
        });

        firstSetCol.setCellFactory(column -> new TableCell<FirstFollowRow, List<Symbol>>() {
            @Override protected void updateItem(List<Symbol> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) setText("");
                else setText("{ " + item.stream().map(Symbol::getLexeme).collect(Collectors.joining(", ")) + " }");
            }
        });

        followSetCol.setCellFactory(column -> new TableCell<FirstFollowRow, List<Symbol>>() {
            @Override protected void updateItem(List<Symbol> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) setText("");
                else setText("{ " + item.stream().map(Symbol::getLexeme).collect(Collectors.joining(", ")) + " }");
            }
        });
    }

    @FXML
    private void handleLoadTokenFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Lexer RE and Tokens File");
        Window stage = inputArea.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            tokenFileLabel.setText(file.getName());
            consoleArea.clear(); 

            executeHeavyTask(
                "Building Lexer & Automata Graph...",
                () -> {
                    List<Token> rules = RuleReader.readTokens(file.getAbsolutePath());
                    Lexer newLexer = new Lexer(rules);
                    AutomataVisualizer.exportToImage(newLexer.getMasterAutomaton(), "master_lexer_automata.png");
                    return newLexer;
                },
                (newLexer) -> {
                    this.lexer = newLexer;
                    if (lexer.getMasterAutomaton() != null) {
                        automataDetailsArea.setText(lexer.getMasterAutomaton().toString());
                        InteractiveAutomataView graphView = new InteractiveAutomataView(lexer.getMasterAutomaton());
                        interactiveGraphContainer.setCenter(graphView);
                    } else {
                        interactiveGraphContainer.setCenter(new Label("No Automaton Generated."));
                    }
                    outputArea.setText("✅ Lexer successfully built from: " + file.getName());
                },
                (error) -> {
                    outputArea.setText("❌ Error building Lexer: " + error.getMessage());
                    this.lexer = null;
                    interactiveGraphContainer.setCenter(new Label("Failed to load graph."));
                }
            );
        }
    }

    @FXML
    private void handleLoadInputFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Input Text File");
        Window stage = inputArea.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            inputFileLabel.setText(file.getName());

            executeHeavyTask(
                "Loading Input File...",
                () -> Utils.readTextFile(file.getAbsolutePath()),
                (content) -> {
                    inputArea.setText(content);
                    outputArea.setText("✅ Input file loaded. Ready for analysis.");
                },
                (error) -> outputArea.setText("❌ Error reading input file: " + error.getMessage())
            );
        }
    }

    @FXML
    private void handleLoadGrammarFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Grammar File (BNF/EBNF)");
        Window stage = inputArea.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            grammarFileLabel.setText(file.getName());

            executeHeavyTask(
                "Loading Grammar...",
                () -> GrammarBuilder.buildFromBnfFile(file.getAbsolutePath()),
                (grammar) -> {
                    this.currentGrammar = grammar;
                    outputArea.setText("✅ Grammar loaded successfully. Ready for Syntax Analysis.");
                },
                (error) -> {
                    outputArea.setText("❌ Error parsing grammar: " + error.getMessage());
                    this.currentGrammar = null;
                }
            );
        }
    }

    private static class AnalysisResult {
        String scanOutput;
        List<Symbol> symbols;
        List<FirstFollowRow> firstFollowRows;
    }

    @FXML
    private void handleRunLexer() {
        if (lexer == null) {
            outputArea.setText("⚠️ Please load a Lexer RE and Tokens file first.");
            return;
        }

        String input = inputArea.getText();
        if (input == null || input.trim().isEmpty()) {
            outputArea.setText("⚠️ No input code found to analyze.");
            return;
        }

        executeHeavyTask(
            "Running Analysis (Lexical & Syntax)...",
            () -> {
                AnalysisResult result = new AnalysisResult();
                
                result.scanOutput = lexer.scan(input);
                if (lexer.getSymbolTable() != null) {
                    result.symbols = new ArrayList<>(lexer.getSymbolTable().getTable());
                }
                
                return result;
            },
            (result) -> {
                outputArea.setText(result.scanOutput);
                
                if (result.symbols != null) {
                    symbolTableViewer.setItems(FXCollections.observableArrayList(result.symbols));
                    symbolTableViewer.refresh();
                }
                if (result.firstFollowRows != null) {
                    firstFollowTable.setItems(FXCollections.observableArrayList(result.firstFollowRows));
                    firstFollowTable.refresh();
                }
            },
            (error) -> {
                outputArea.setText("❌ Analysis Error: " + error.getMessage());
            }
        );
    }

    @FXML
    private void handleRunSyntaxAnalysis() {
        if (currentGrammar == null) {
            outputArea.setText("⚠️ Please load a Grammar file (BNF/EBNF) first.");
            return;
        }

        if (lexer == null || lexer.getSymbolTable() == null || lexer.getSymbolTable().getTable().isEmpty()) {
            outputArea.setText("⚠️ Please run Lexer Analysis first to generate tokens.");
            return;
        }

        executeHeavyTask(
            "Running Syntax Analysis (FIRST/FOLLOW & Syntax Tree)...",
            () -> {
                // 1. Calculate FIRST and FOLLOW sets
                FirstFollowCalculator calculator = new FirstFollowCalculator(currentGrammar);
                calculator.computeSets();
                List<FirstFollowRow> firstFollowData = new ArrayList<>(calculator.getResultsTable().getTable().values());

                // 2. Build the Syntax Tree (AST) using the Recursive Descent Parser
                RecursiveDescentParser parser = new RecursiveDescentParser(lexer.getSymbolTable().getTable());
                SyntaxTreeNode astRoot = parser.parse();

                if (parser.hasErrors()) {
                    throw new RuntimeException(String.join("\n", parser.getErrors()));
                }

                // Return a combined result object
                return new SyntaxAnalysisResult(firstFollowData, astRoot);
            },
            (result) -> {
                // 1. Populate the First & Follow UI Table
                firstFollowTable.setItems(FXCollections.observableArrayList(result.firstFollowRows));
                firstFollowTable.refresh();

                // 2. Populate the Syntax Tree View
                if (result.astRoot != null && syntaxTreeView != null) {
                    syntaxTreeView.setRoot(buildTreeItem(result.astRoot));
                }

                outputArea.setText("✅ Syntax Analysis complete. FIRST/FOLLOW sets and AST updated.");
            },
            (error) -> {
                outputArea.setText("❌ Syntax Analysis Error:\n" + error.getMessage());
            }
        );
    }

    // --- HELPER CLASSES AND METHODS ---

    // Temporary object to pass both sets of data out of the background thread safely
    private static class SyntaxAnalysisResult {
        List<FirstFollowRow> firstFollowRows;
        SyntaxTreeNode astRoot;

        SyntaxAnalysisResult(List<FirstFollowRow> firstFollowRows, SyntaxTreeNode astRoot) {
            this.firstFollowRows = firstFollowRows;
            this.astRoot = astRoot;
        }
    }

    // Recursively converts your custom AST nodes into JavaFX TreeItems for the UI
    private javafx.scene.control.TreeItem<String> buildTreeItem(SyntaxTreeNode node) {
        javafx.scene.control.TreeItem<String> item = new javafx.scene.control.TreeItem<>(node.getLabel());
        item.setExpanded(true); // Expand tree by default so it's readable immediately
        
        for (SyntaxTreeNode child : node.getChildren()) {
            item.getChildren().add(buildTreeItem(child));
        }
        
        return item;
    }

    private void setButtonsDisabled(boolean disabled) {
        if(loadTokenBtn != null) loadTokenBtn.setDisable(disabled);
        if(loadInputBtn != null) loadInputBtn.setDisable(disabled);
        if(runLexerBtn != null) runLexerBtn.setDisable(disabled);
        if(loadGrammarBtn != null) loadGrammarBtn.setDisable(disabled);
        if(runSyntaxBtn != null) runSyntaxBtn.setDisable(disabled); 
    }

    @FXML
    private void handleExportCSV() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Export CSVs");
        File dir = directoryChooser.showDialog(inputArea.getScene().getWindow());

        if (dir != null) {
            File symbolsFile = new File(dir, "symbol_table.csv");
            File firstFollowFile = new File(dir, "first_follow_sets.csv");

            try {
                // Export Symbol Table
                try (PrintWriter writer = new PrintWriter(symbolsFile, "UTF-8")) {
                    writer.println("Lexeme,Token Type,Line,Column");
                    if (symbolTableViewer.getItems() != null) {
                        for (Symbol s : symbolTableViewer.getItems()) {
                            // Properly escape to avoid breaking columns
                            String lexeme = escapeCSV(s.getLexeme());
                            String tokenType = escapeCSV(s.getTokenType());
                            writer.println(lexeme + "," + tokenType + "," + s.getLine() + "," + s.getCol());
                        }
                    }
                }

                // Export First and Follow Sets
                try (PrintWriter writer = new PrintWriter(firstFollowFile, "UTF-8")) {
                    writer.println("Non-Terminal,First,Follow");
                    if (firstFollowTable.getItems() != null) {
                        for (FirstFollowRow row : firstFollowTable.getItems()) {
                            String nt = escapeCSV(row.getNonTerminal() != null ? row.getNonTerminal().getLexeme() : "");
                            
                            // Safe stream mapping with null checks
                            String firstStr = row.getFirstSet() != null ? 
                                row.getFirstSet().stream().map(Symbol::getLexeme).collect(Collectors.joining(", ")) : "";
                            String followStr = row.getFollowSet() != null ? 
                                row.getFollowSet().stream().map(Symbol::getLexeme).collect(Collectors.joining(", ")) : "";
                            
                            // Wrap the sets in curly braces and escape them securely
                            writer.println(nt + "," + escapeCSV("{ " + firstStr + " }") + "," + escapeCSV("{ " + followStr + " }"));
                        }
                    }
                }

                outputArea.setText("✅ Data exported successfully to: " + dir.getAbsolutePath());
            } catch (Exception e) {
                outputArea.setText("❌ Export Error: " + e.getMessage());
                e.printStackTrace(); // Helpful for debugging console
            }
        }
    }

    /**
     * Helper method to securely format strings for CSVs.
     * Prevents commas, newlines, and quotes inside lexemes from breaking the CSV layout.
     */
    private String escapeCSV(String data) {
        if (data == null) {
            return "";
        }
        // Remove line breaks to keep data confined to a single row
        String escapedData = data.replaceAll("\\R", " ");
        
        // If data contains commas or quotes, it must be wrapped in double-quotes
        if (escapedData.contains(",") || escapedData.contains("\"") || escapedData.contains("'")) {
            escapedData = escapedData.replace("\"", "\"\""); // Escape internal double quotes
            escapedData = "\"" + escapedData + "\""; // Wrap the whole field in quotes
        }
        return escapedData;
    }

    @FXML
    private void handleSaveConsoleLog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Console Log");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        Window stage = consoleArea.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                Utils.writeTextFile(file.getAbsolutePath(), consoleArea.getText());
                outputArea.setText("✅ Console log successfully saved to: " + file.getName());
            } catch (IOException e) {
                outputArea.setText("❌ Error saving console log: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleClearTables() {
        if (symbolTableViewer != null) symbolTableViewer.getItems().clear();
        if (firstFollowTable != null) firstFollowTable.getItems().clear();
        if (lexer != null && lexer.getSymbolTable() != null) lexer.getSymbolTable().clearTable();
        
        outputArea.setText("Tables cleared.");
        consoleArea.appendText("\n[UI] Tables and internal symbol cache have been cleared.\n");
    }

    private void setupSymbolTable() {
        if (lexemeColumn != null && tokenTypeColumn != null && lineColumn != null && colColumn != null) {
            lexemeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getLexeme()));
                
            tokenTypeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getTokenType()));
                
            lineColumn.setCellValueFactory(cellData -> 
                new SimpleIntegerProperty(cellData.getValue().getLine()).asObject());

            colColumn.setCellValueFactory(cellData -> 
                new SimpleIntegerProperty(cellData.getValue().getCol()).asObject());
        } else {
            System.err.println("ERROR: Symbol Table columns failed to inject! Check FXML fx:id matches.");
        }
    }

    private static class TextAreaOutputStream extends OutputStream {
        private final TextArea console;

        public TextAreaOutputStream(TextArea console) {
            this.console = console;
        }

        @Override
        public void write(int b) {
            Platform.runLater(() -> console.appendText(String.valueOf((char) b)));
        }

        @Override
        public void write(byte[] b, int off, int len) {
            String text = new String(b, off, len);
            Platform.runLater(() -> console.appendText(text));
        }
    }
}