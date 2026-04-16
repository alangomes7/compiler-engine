package ui;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import core.lexer.models.atomic.Token;
import core.lexer.models.automata.AFD;
import core.parser.models.FirstFollowTable;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
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
import javafx.scene.control.TreeItem;
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
    
    @FXML private TableView<Token> symbolTableViewer;
    @FXML private TableColumn<Token, Integer> lineColumn;
    @FXML private TableColumn<Token, Integer> colColumn;
    @FXML private TableColumn<Token, String> lexemeColumn;
    @FXML private TableColumn<Token, String> tokenTypeColumn;

    @FXML private TableView<Symbol> firstFollowTable;
    @FXML private TableColumn<Symbol, String> nonTerminalCol;
    @FXML private TableColumn<Symbol, Set<Symbol>> firstSetCol;
    @FXML private TableColumn<Symbol, Set<Symbol>> followSetCol;
    
    // Store a reference to the current table to resolve sets in cell factories
    private FirstFollowTable currentFirstFollowTable;

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
                log.accept("📥 Reading grammar file...");

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
            "Analyzing Syntax...",
            log -> {
                log.accept("📊 Computing FIRST/FOLLOW...");
                var result = parserService.runAnalysis(lexerService.getSymbolTable());
                log.accept("🌳 Building syntax tree...");
                return result;
            },
            result -> {
                this.currentFirstFollowTable = result.firstFollowTable;

                List<Symbol> nonTerminals =
                    List.copyOf(currentFirstFollowTable.getAllFirstSets().keySet());

                firstFollowTable.setItems(
                    FXCollections.observableArrayList(nonTerminals)
                );

                syntaxTreeView.setRoot(buildTreeItem(result.parseTree.getRoot()));
                outputArea.setText("✅ Syntax Analysis complete.");
            },
            err -> outputArea.setText("❌ Syntax Error:\n" + err.getMessage())
        );
    }

    @FXML
    private void handleClearTables() {
        symbolTableViewer.getItems().clear();
        firstFollowTable.getItems().clear();
        syntaxTreeView.setRoot(null);
        outputArea.clear();
        consoleArea.clear();
    }

    // --- UI Logic & Helpers ---

    private void setupSymbolTable() {
        lexemeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLexeme()));
        tokenTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTokenType()));
        lineColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getLine()).asObject());
        colColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCol()).asObject());
    }

    private void setupFirstFollowTable() {
        // Non-terminal column just shows the symbol name
        nonTerminalCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getName()));
            
        // First set column fetches data from the currentFirstFollowTable reference
        firstSetCol.setCellValueFactory(data -> {
            if (currentFirstFollowTable == null) return new SimpleObjectProperty<>(null);
            return new SimpleObjectProperty<>(currentFirstFollowTable.getFirst(data.getValue()));
        });
            
        followSetCol.setCellValueFactory(data -> {
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

        firstSetCol.setCellFactory(setCellFactory);
        followSetCol.setCellFactory(setCellFactory);
    }

    private TreeItem<String> buildTreeItem(Node node) {
        String label = node.getSymbol().getName();
        if (node.getLexeme() != null) {
            label += " (\"" + node.getLexeme() + "\")";
        }
        
        TreeItem<String> item = new TreeItem<>(label);
        item.setExpanded(true);
        for (Node child : node.getChildren()) {
            item.getChildren().add(buildTreeItem(child));
        }
        return item;
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
                    loadingTimeLabel.setText(String.format("processing... %02d.%02ds", seconds, millis));
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