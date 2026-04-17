package ui;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import core.lexer.models.atomic.Token;
import core.lexer.models.automata.AFD;
import core.parser.core.grammar.GrammarClassification;
import core.parser.models.FirstFollowTable;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import ui.core.graph.AutomataVisualizer;
import ui.core.graph.InteractiveAutomataView;
import ui.core.services.FileService;
import ui.core.services.LexerService;
import ui.core.services.ParserService;
import ui.utils.UiUtils;

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

    @Getter
    @FXML private TextArea automataDetailsArea;

    @Getter
    @FXML private javafx.scene.layout.BorderPane interactiveGraphContainer;
    
    // Symbol Table
    @FXML private TableView<Token> symbolTableViewer;
    @FXML private TableColumn<Token, Integer> symbolTableLineColumn;
    @FXML private TableColumn<Token, Integer> symbolTableColColumn;
    @FXML private TableColumn<Token, String> symbolTableLexemeColumn ;
    @FXML private TableColumn<Token, String> symbolTableTokenTypeColumn;

    // First Follow Table
    @Getter
    @FXML private TableView<Symbol> firstFollowTable;
    @FXML private TableColumn<Symbol, String> firstFollowTableNonTerminalCol;
    @FXML private TableColumn<Symbol, Set<Symbol>> firstFollowTableFirstSetCol;
    @FXML private TableColumn<Symbol, Set<Symbol>> firstFollowTableFollowSetCol;

    // Parser Table
    @FXML private TableView<Symbol> parserTable;
    @FXML private TableColumn<Symbol, String> parserTableNonTerminalCol;
    
    // Store a reference to the current table to resolve sets in cell factories
    private FirstFollowTable currentFirstFollowTable;
    private ParseTable currentParseTable;

    // Grammar classification are
    @FXML private TextArea grammarClassificationArea;

    @FXML private TreeView<String> syntaxTreeView;

    // --- Services ---
    private final LexerService lexerService = new LexerService();
    private final ParserService parserService = new ParserService();

    @FXML
    public void initialize() {
        // Redirect System.out/err to the console TextArea using utility class
        PrintStream ps = new PrintStream(new UiUtils.TextAreaOutputStream(consoleArea), true);
        System.setOut(ps);
        System.setErr(ps);

        // Synchronize line numbers
        inputArea.textProperty().addListener((obs, old, newVal) -> 
            UiUtils.updateLineNumbers(inputArea, lineNumbersArea));
        
        setupFirstFollowTable();
        setupSymbolTable();
        setupParserTable();
    }

    // --- Action Handlers ---

   @FXML
    private void handleLoadTokenFile() {
        File file = FileService.selectFile(
            inputArea.getScene().getWindow(),
            "Select Lexer Rules",
            "*.txt",
            "*.lexer"
        );

        if (file == null) return;

        tokenFileLabel.setText(file.getName());

        executeHeavyTask(
            "Building Lexer...",
            log -> {

                AFD automaton;
                try {
                    automaton = lexerService.buildLexer(file.getAbsolutePath(), log);
                } catch (Exception e) {
                    System.getLogger(Ui.class.getName()).log(System.Logger.Level.ERROR, "Lexer build failed", e);
                    throw new RuntimeException("Failed to build lexer: " + e.getMessage(), e);
                }
                
                log.accept("Creating Lexer Automaton Image...");
                
                // Assuming exportToImage just writes to disk, it is safe here.
                AutomataVisualizer.exportToImage(automaton, "lexer_automata.png");

                // Return the automaton so it gets passed to the onSuccess block
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
        File file = FileService.selectFile(inputArea.getScene().getWindow(), "Select Grammar", "*.txt", "*.grammar");
        if (file == null) return;

        grammarFileLabel.setText(file.getName());
        
        executeHeavyTask(
            "Loading Grammar...",
            log -> {
                log.accept("Reading grammar file...");

                try {
                    parserService.loadGrammar(file.getAbsolutePath());
                } catch (Exception ex) {
                    System.getLogger(Ui.class.getName())
                        .log(System.Logger.Level.ERROR, "Grammar load failed", ex);

                    throw new RuntimeException("Failed to load grammar: " + ex.getMessage(), ex);
                }

                log.accept("✅ Grammar loaded.");
                return null;
            },
            res -> outputArea.setText("✅ Grammar loaded."),
            err -> outputArea.setText("❌ Error: " + err.getMessage())
        );
    }

    @FXML
    private void handleLoadInputFile() {
        File file = FileService.selectFile(inputArea.getScene().getWindow(), "Select Input File");
        if (file == null) return;

        executeHeavyTask(
            "Loading File...",
            log -> {
                log.accept("📂 Reading input file...");
                String content;
                try {
                    content = FileService.readFileContent(file);
                } catch (Exception ex) {
                    // 1. Log the error
                    System.getLogger(Ui.class.getName()).log(System.Logger.Level.ERROR, "Input file load failed", ex);
                    
                    // 2. Rethrow to trigger the onError consumer
                    throw new RuntimeException("Failed to read input file: " + ex.getMessage(), ex);
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

        // Extract text on the JavaFX Main Thread FIRST
        final String inputText = inputArea.getText();

        executeHeavyTask(
            "Scanning...",
            log -> {
                log.accept("🔍 Scanning input...");
                
                // Pass the extracted string into the background task
                String result = lexerService.scan(inputText); 
                
                log.accept("✅ Scan complete.");
                return result;
            },
            msg -> {
                outputArea.setText(msg);
                symbolTableViewer.setItems(
                    FXCollections.observableArrayList(lexerService.getSymbolTable())
                );
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

        executeHeavyTask(
            "Populating FirstFollowTable",
        log ->{
                var firstFollowTable = parserService.buildFirstFollowTable();
                return firstFollowTable;
            },
            result ->{
                this.currentFirstFollowTable = result;
                List<Symbol> nonTerminals =
                    List.copyOf(currentFirstFollowTable.getAllFirstSets().keySet());

                firstFollowTable.setItems(
                    FXCollections.observableArrayList(nonTerminals));

                outputArea.setText("✅ Syntax Analysis complete.");

                }, error ->{
                    outputArea.setText("FirstFollow Table Error:\n" + error.getMessage());
        });

        executeHeavyTask("Building Syntax Tree",
            log -> {
                var parserTableResult = (ParseTable) parserService.buildParseTable(this.currentFirstFollowTable, this.lexerService.getSymbolTable());
                return parserTableResult;
            },
            result -> {
                this.currentParseTable = result;
                
                // Populate the dynamic columns and rows
                populateParserTable(result);
                
                outputArea.setText(outputArea.getText() + "\nParse Table generated.");
            },
            error -> {
                currentParseTable.getTable().clear();
                currentFirstFollowTable = null;
                outputArea.setText(outputArea.getText() + "\nParse Table Error:\n" + error.getMessage());
            }
        );
    }

    @FXML
    private void handleClearTables() {
        symbolTableViewer.getItems().clear();
        firstFollowTable.getItems().clear();
        parserTable.getItems().clear();
        parserTable.getColumns().setAll(List.of(parserTableNonTerminalCol));
        syntaxTreeView.setRoot(null);
        outputArea.clear();
        consoleArea.clear();
        
        // Clear references
        currentFirstFollowTable = null;
        currentParseTable = null;
    }

    // --- UI Logic & Helpers ---

    private void setupSymbolTable() {
        symbolTableLexemeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLexeme()));
        symbolTableTokenTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTokenType()));
        symbolTableLineColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getLine()).asObject());
        symbolTableColColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCol()).asObject());
    }

    private void setupFirstFollowTable() {
        // Non-terminal column just shows the symbol name
        firstFollowTableNonTerminalCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getName()));
            
        // First set column fetches data from the currentFirstFollowTable reference
        firstFollowTableFirstSetCol.setCellValueFactory(data -> {
            if (currentFirstFollowTable == null) return new SimpleObjectProperty<>(null);
            return new SimpleObjectProperty<>(currentFirstFollowTable.getFirst(data.getValue()));
        });
            
        firstFollowTableFollowSetCol.setCellValueFactory(data -> {
            if (currentFirstFollowTable == null) return new SimpleObjectProperty<>(null);
            return new SimpleObjectProperty<>(currentFirstFollowTable.getFollow(data.getValue()));
        });

        // Set formatter for displaying sets as strings
        Callback<TableColumn<Symbol, Set<Symbol>>, TableCell<Symbol, Set<Symbol>>> setCellFactory = 
            column -> new TableCell<>() {
                @Override protected void updateItem(Set<Symbol> item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) setText("");
                    else setText("{ " + item.stream().map(Symbol::getName).collect(Collectors.joining(", ")) + " }");
                }
            };

        firstFollowTableFirstSetCol.setCellFactory(setCellFactory);
        firstFollowTableFollowSetCol.setCellFactory(setCellFactory);
    }

    private void setupParserTable() {
        // Bind the base Non-Terminal column to the Symbol's name
        parserTableNonTerminalCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getName()));
    }

    private void populateParserTable(ParseTable parseTableResult) {
        // 1. Reset columns to just the Non-Terminal base column
        this.parserTable.getColumns().setAll(List.of(parserTableNonTerminalCol));

        if (parseTableResult == null || parseTableResult.getTable().isEmpty()) {
            parserTable.getItems().clear();
            return;
        }

        Map<Symbol, Map<Symbol, List<Production>>> parseTable = parseTableResult.getTable();

        // 2. Extract all unique terminal symbols to create dynamic columns
        Set<Symbol> terminalSet = new java.util.HashSet<>();
        for (Map<Symbol, List<Production>> row : parseTable.values()) {
            terminalSet.addAll(row.keySet());
        }

        List<Symbol> terminals = new java.util.ArrayList<>(terminalSet);
        terminals.sort((t1, t2) -> {
            // Optional: Keep EOF symbol (e.g., "$") at the far right
            if (t1.getName().equals("$")) return 1;
            if (t2.getName().equals("$")) return -1;
            return t1.getName().compareTo(t2.getName());
        });

        // 3. Create a column for each Terminal
        for (Symbol terminal : terminals) {
            TableColumn<Symbol, String> terminalCol = new TableColumn<>(terminal.getName());

            terminalCol.setCellValueFactory(data -> {
                Symbol nonTerminal = data.getValue();

                List<Production> productions =
                    currentParseTable.getEntry(nonTerminal, terminal);

                if (productions == null || productions.isEmpty()) {
                    return new SimpleStringProperty("");
                }

                String text = productions.stream()
                        .map(Production::toString)
                        .collect(Collectors.joining("\n"));

                return new SimpleStringProperty(text);
            });

            
            terminalCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);

                        if (item.contains("\n")) {
                            setStyle("-fx-background-color: #ffe6e6; -fx-text-fill: #d8000c; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            });

            parserTable.getColumns().add(terminalCol);
        }
            

        // 4. Set the Non-Terminals as the rows
        this.parserTable.setItems(FXCollections.observableArrayList(parseTable.keySet()));
    }

    @FXML
    private void handleSaveConsoleLog() {
        // Implementation for saving the consoleArea content to a file
        System.out.println("Saving console log... (Feature not yet implemented)");
    }

    @FXML
    private void handleExportCSV() {
        // Implementation for exporting symbol tables or sets to CSV
        System.out.println("Exporting to CSV... (Feature not yet implemented)");
    }

    @FXML
    private void handleClassifyGrammar() {
        executeHeavyTask("Classifying grammar...", 
        log ->{
            var grammarCheck = (GrammarClassification) parserService.classifyGrammarWithParserTable(this.currentParseTable);
            return grammarCheck;
        }, result ->{
            grammarClassificationArea.setText(result.toString());
            outputArea.setText(result.toString());
        }, onError ->{
            outputArea.setText(onError.getMessage());
        });
    }

    @FXML
    private void handleClearGrammarClassification() {
        grammarClassificationArea.clear();
    }

    private void appendLog(String message) {
        Platform.runLater(() -> {
            String time = java.time.LocalTime.now().withNano(0).toString();
            consoleArea.appendText("[" + time + "] " + message + "\n");
            consoleArea.setScrollTop(Double.MAX_VALUE);
            
            if (loadingOverlay.isVisible()) {
                loadingLabel.setText(message);
            }
        });
    }

    private <T> void executeHeavyTask(
        String msg,
        java.util.function.Function<Consumer<String>, T> task,
        Consumer<T> onSuccess,
        Consumer<Exception> onError
    ) {
        // 1. Capture start time and define the UI timer
        final long startTime = System.currentTimeMillis();
        
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                // Calculate elapsed time
                long elapsedMillis = System.currentTimeMillis() - startTime;
                long seconds = (elapsedMillis / 1000) % 60;
                long millis = (elapsedMillis % 1000) / 10; // get 2 digits for milliseconds
                
                // Safely update the UI (AnimationTimer inherently runs on the FX Thread)
                if (loadingTimeLabel != null) {
                    loadingTimeLabel.setText(String.format("processing... %d.%03ds", seconds, millis));
                }
            }
        };

        Platform.runLater(() -> {
            loadingLabel.setText(msg);
            loadingOverlay.setVisible(true);
            
            // 2. Start the timer precisely when the overlay becomes visible
            timer.start();
        });

        Thread thread = new Thread(() -> {
            try {
                T result = task.apply(this::appendLog);

                Platform.runLater(() -> {
                    // 3. Stop the timer on success
                    timer.stop(); 
                    onSuccess.accept(result);
                    loadingOverlay.setVisible(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    // 4. Stop the timer on failure
                    timer.stop(); 
                    onError.accept(e);
                    loadingOverlay.setVisible(false);
                });
            }
        });

        thread.setDaemon(true);
        thread.start();
    }
}