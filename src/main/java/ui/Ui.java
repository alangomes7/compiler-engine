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
import core.parser.models.tree.Node;
import core.parser.models.tree.ParseTree;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
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
import ui.core.graph.automata.AutomataVisualizer;
import ui.core.graph.automata.InteractiveAutomataView;
import ui.core.graph.tree.InteractiveTreeView;
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
    @FXML private Label inputFileLabel;

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

    @FXML private javafx.scene.layout.BorderPane interactiveTreeContainer;

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
        
        invalidateParserState();

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
        
        invalidateParserState();
        
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

        // Update the FXML label
        inputFileLabel.setText(file.getName()); 
        
        // Clear previous input-dependent results
        invalidateInputState();

        executeHeavyTask(
            "Loading File...",
            log -> {
                log.accept("📂 Reading input file...");
                String content;
                try {
                    content = FileService.readFileContent(file);
                } catch (Exception ex) {
                    System.getLogger(Ui.class.getName()).log(System.Logger.Level.ERROR, "Input file load failed", ex);
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
            log -> {
                return parserService.buildFirstFollowTable();
            },
            firstFollowResult -> {
                // ✅ FIRST TASK SUCCESS
                this.currentFirstFollowTable = firstFollowResult;

                List<Symbol> nonTerminals =
                    List.copyOf(currentFirstFollowTable.getAllFirstSets().keySet());

                firstFollowTable.setItems(
                    FXCollections.observableArrayList(nonTerminals)
                );

                outputArea.setText("✅ First/Follow computed.");

                // 🚀 NOW run the SECOND task INSIDE success callback
                executeHeavyTask(
                    "Building Parse Table",
                    log -> {
                        return parserService.buildParseTable(
                            this.currentFirstFollowTable,
                            this.lexerService.getSymbolTable()
                        );
                    },
                    parseTableResult -> {
                        // --- SUCCESS BLOCK FOR PARSE TABLE ---
                        this.currentParseTable = parseTableResult;

                        populateParserTable(parseTableResult);

                        outputArea.setText(outputArea.getText() + "\n✅ Parse Table generated.");

                        // --- RUN THE LL(1) PARSER ---
                        try {
                            ParseTree tree = parserService.parseTokens(
                            this.currentParseTable, 
                            this.lexerService.getSymbolTable()
                        );
                        
                        // Create and attach the Interactive Graph to the UI
                        interactiveTreeContainer.setCenter(new InteractiveTreeView(tree.getRoot()));
                        
                        outputArea.setText(outputArea.getText() + "\n✅ Derivation Tree successfully generated.");
                        
                    } catch (Exception e) {
                        outputArea.setText(outputArea.getText() + "\n❌ Syntax Error during parsing:\n" + e.getMessage());
                        interactiveTreeContainer.setCenter(null); // Clear previous tree on failure
                    }
                    },
                    error -> {
                        // --- ERROR BLOCK FOR PARSE TABLE ---
                        currentParseTable = null;
                        currentFirstFollowTable = null;

                        outputArea.setText(
                            outputArea.getText() + "\n❌ Parse Table Error:\n" + error.getMessage()
                        );
                    }
                );
            },
            error -> {
                // --- ERROR BLOCK FOR FIRST/FOLLOW ---
                outputArea.setText(
                    "❌ FirstFollow Table Error:\n" + error.getMessage()
                );
            }
        );
    }

    @FXML
    private void handleClearTables() {

        // Symbol Table
        if (symbolTableViewer != null) {
            symbolTableViewer.getItems().clear();
            // DO NOT clear columns here, as they are statically bound in setupSymbolTable()
        }
        
        // FirstFollow Table
        if (firstFollowTable != null) {
            firstFollowTable.getItems().clear();
        }
        
        // Safely clear current data references (Let the Garbage Collector handle the sets)
        currentFirstFollowTable = null;

        // Parser Table
        if (parserTable != null) {
            parserTable.getItems().clear();
            // Reset to ONLY the base Non-Terminal column, removing dynamic Terminal columns
            parserTable.getColumns().setAll(List.of(parserTableNonTerminalCol));
        }
        currentParseTable = null;

        // Syntax Tree
        if (interactiveTreeContainer != null) {
            interactiveTreeContainer.setCenter(null);
        }

        // Outputs
        if (outputArea != null) outputArea.clear();
        if (consoleArea != null) consoleArea.clear();
        if (grammarClassificationArea != null) grammarClassificationArea.clear();
        
        // CRITICAL: Do NOT set @FXML UI components to null (e.g., symbolTableViewer = null).
        // JavaFX relies on those references to update the UI on the next run.
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

    /**
     * Clears all tables and trees when the Lexer or Grammar files are updated.
     */
    private void invalidateParserState() {
        if (parserTable != null) {
            parserTable.getItems().clear();
            parserTable.getColumns().setAll(List.of(parserTableNonTerminalCol));
        }
        currentParseTable = null;
        
        if (firstFollowTable != null) firstFollowTable.getItems().clear();
        currentFirstFollowTable = null;

        if (grammarClassificationArea != null) grammarClassificationArea.clear();
        
        // Clear input-dependent views as well
        invalidateInputState(); 
    }

    /**
     * Clears only the views that depend directly on the input code.
     */
    private void invalidateInputState() {
        if (symbolTableViewer != null) symbolTableViewer.getItems().clear();
        if (interactiveTreeContainer != null) interactiveTreeContainer.setCenter(null);
    }

    /**
     * Recursively maps a parser Node to a JavaFX TreeItem for visualization.
     */
    private TreeItem<String> buildTreeItem(Node node) {
        // Show the symbol name (e.g., "Expression", "IDENTIFIER")
        String displayText = node.getSymbol().getName();
        
        // If it's a leaf node with a matched token, append the exact lexeme string
        if (node.getLexeme() != null) {
            displayText += " (\"" + node.getLexeme() + "\")";
        }
        
        TreeItem<String> item = new TreeItem<>(displayText);
        item.setExpanded(true); // Automatically expand the tree view
        
        // Recurse for all children
        for (Node child : node.getChildren()) {
            item.getChildren().add(buildTreeItem(child));
        }
        
        return item;
    }

    @FXML
    private void handleExportGraphImage() {
        // 1. Check if the view is currently loaded
        javafx.scene.Node centerNode = interactiveGraphContainer.getCenter();
        if (!(centerNode instanceof InteractiveAutomataView)) {
            outputArea.setText("No interactive graph available to export.");
            return;
        }
        
        InteractiveAutomataView view = (InteractiveAutomataView) centerNode;

        // 2. Capture the snapshot ON the JavaFX UI Thread (Safe)
        javafx.scene.image.WritableImage snapshotImage = view.generateSnapshot();

        // 3. Delegate the heavy file conversion and saving to the background thread
        executeHeavyTask(
            "Exporting graph image...",
            log -> {
                log.accept("Converting to high resolution PNG and saving to disk...");
                
                String outputFilename = "output/graph.png";
                File outputFile = new File(outputFilename);
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                try {
                    // Heavy tasks: Converting FX Image to Swing Image and Disk I/O
                    java.awt.image.BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshotImage, null);
                    javax.imageio.ImageIO.write(bufferedImage, "png", outputFile);
                    
                    log.accept("✅ Export completed.");
                    return outputFile.getAbsolutePath(); // Return the path for the success callback
                } catch (Exception e) {
                    throw new RuntimeException("Failed to export image: " + e.getMessage(), e);
                }
            },
            resultPath -> {
                outputArea.setText("✅ Full 4K Graph successfully exported to " + resultPath);
            },
            onError -> {
                outputArea.setText("❌ Export failed: " + onError.getMessage());
            }
        );
    }

    @FXML
    private void handleExportTreeImage() {
        javafx.scene.Node centerNode = interactiveTreeContainer.getCenter();
        if (!(centerNode instanceof InteractiveTreeView)) {
            outputArea.setText("No interactive tree graph available to export.");
            return;
        }
        
        InteractiveTreeView view = (InteractiveTreeView) centerNode;
        javafx.scene.image.WritableImage snapshotImage = view.generateSnapshot();

        executeHeavyTask(
            "Exporting tree image...",
            log -> {
                log.accept("Converting to high resolution PNG and saving to disk...");
                
                String outputFilename = "output/syntax_tree.png";
                File outputFile = new File(outputFilename);
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                try {
                    java.awt.image.BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshotImage, null);
                    javax.imageio.ImageIO.write(bufferedImage, "png", outputFile);
                    
                    log.accept("✅ Export completed.");
                    return outputFile.getAbsolutePath();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to export image: " + e.getMessage(), e);
                }
            },
            resultPath -> outputArea.setText("✅ Full 4K Syntax Tree successfully exported to " + resultPath),
            onError -> outputArea.setText("❌ Export failed: " + onError.getMessage())
        );
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
                long hours = elapsedMillis / (1000 * 60 * 60);
                long minutes = (elapsedMillis / (1000 * 60)) % 60;
                long seconds = (elapsedMillis / 1000) % 60;
                long millis = elapsedMillis % 1000;

                // Safely update the UI (AnimationTimer inherently runs on the FX Thread)
                if (loadingTimeLabel != null) {
                    loadingTimeLabel.setText(String.format("processing... %02d:%02d:%02d.%03d", hours, minutes, seconds, millis));
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