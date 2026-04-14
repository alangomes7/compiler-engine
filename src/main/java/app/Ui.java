package app;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import models.atomic.Symbol;
import models.atomic.Token;
import models.others.FirstFollowRow;
import models.others.Grammar;
import models.tree.SyntaxTreeNode;
import core.lexer.Lexer;
import core.lexer.translators.TokenReader;

import core.parser.FirstFollowCalculator;
import core.parser.GrammarBuilder;

import Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

public class Ui {

    @FXML private TextArea lineNumbersArea;
    @FXML private TextArea inputArea;
    @FXML private TextArea outputArea;
    @FXML private TextArea consoleArea;
    @FXML private TableView<Symbol> symbolTableViewer;
    @FXML private Label tokenFileLabel;
    @FXML private Label inputFileLabel;
    @FXML private Button loadTokenBtn;
    @FXML private Button loadInputBtn;
    @FXML private Button runLexerBtn;
    @FXML private TextArea automataDetailsArea;
    @FXML private TreeView<String> syntaxTreeView;
    
    @FXML private TableView<FirstFollowRow> firstFollowTable;
    
    // Updated generics to match the new FirstFollowRow data types
    @FXML private TableColumn<FirstFollowRow, Symbol> nonTerminalCol;
    @FXML private TableColumn<FirstFollowRow, List<Symbol>> firstSetCol;
    @FXML private TableColumn<FirstFollowRow, List<Symbol>> followSetCol;

    private Lexer lexer;

    @FXML
    public void initialize() {
        // Redirect System.out and System.err to the Console Log TextArea
        PrintStream ps = new PrintStream(new TextAreaOutputStream(consoleArea), true);
        System.setOut(ps);
        System.setErr(ps);

        // Synchronize scrolling between line numbers and input area
        lineNumbersArea.scrollTopProperty().bindBidirectional(inputArea.scrollTopProperty());

        // Update line numbers whenever the text changes
        inputArea.textProperty().addListener((obs, oldVal, newVal) -> updateLineNumbers());
        updateLineNumbers(); // Initialize line numbers to 1

        setupFirstFollowTable();
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
        // 1. Bind columns to the properties in FirstFollowRow
        nonTerminalCol.setCellValueFactory(new PropertyValueFactory<>("nonTerminal"));
        firstSetCol.setCellValueFactory(new PropertyValueFactory<>("firstSet"));
        followSetCol.setCellValueFactory(new PropertyValueFactory<>("followSet"));

        // 2. Set Custom Cell Factories to format the Symbol objects nicely
        nonTerminalCol.setCellFactory(column -> new TableCell<FirstFollowRow, Symbol>() {
            @Override
            protected void updateItem(Symbol item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getLexeme());
            }
        });

        firstSetCol.setCellFactory(column -> new TableCell<FirstFollowRow, List<Symbol>>() {
            @Override
            protected void updateItem(List<Symbol> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText("");
                } else {
                    String text = item.stream().map(Symbol::getLexeme).collect(Collectors.joining(", "));
                    setText("{ " + text + " }");
                }
            }
        });

        followSetCol.setCellFactory(column -> new TableCell<FirstFollowRow, List<Symbol>>() {
            @Override
            protected void updateItem(List<Symbol> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText("");
                } else {
                    String text = item.stream().map(Symbol::getLexeme).collect(Collectors.joining(", "));
                    setText("{ " + text + " }");
                }
            }
        });
    }

    @FXML
    private void handleExportCSV(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Tables to CSV");
        fileChooser.setInitialFileName("analysis_export.csv");
        File file = fileChooser.showSaveDialog(inputArea.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Export Symbol Table
                writer.println("--- SYMBOL TABLE ---");
                writer.println("Lexeme,Token Type");
                for (Symbol s : symbolTableViewer.getItems()) {
                    writer.println(s.getLexeme() + "," + s.getTokenType());
                }

                // Export First/Follow Table
                writer.println("\n--- FIRST AND FOLLOW SETS ---");
                writer.println("Non-Terminal,First,Follow");
                for (FirstFollowRow row : firstFollowTable.getItems()) {
                    // Extracting the pure text out of the Symbol lists
                    String nt = row.getNonTerminal().getLexeme();
                    String firstStr = row.getFirstSet().stream().map(Symbol::getLexeme).collect(Collectors.joining(", "));
                    String followStr = row.getFollowSet().stream().map(Symbol::getLexeme).collect(Collectors.joining(", "));
                    
                    writer.println(nt + ",\"{ " + firstStr + " }\",\"{ " + followStr + " }\"");
                }
                outputArea.setText("✅ Data exported to CSV.");
            } catch (IOException e) {
                outputArea.setText("❌ Export Error: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleLoadTokenFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Lexer RE and Tokens File");

        Window stage = inputArea.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            tokenFileLabel.setText(file.getName());
            consoleArea.clear(); // Clear console for new build
            
            outputArea.setText("⏳ Building Lexer... Please wait.");
            setButtonsDisabled(true);

            new Thread(() -> {
                try {
                    List<Token> rules = TokenReader.readTokens(file.getAbsolutePath());
                    Lexer newLexer = new Lexer(rules);

                    Platform.runLater(() -> {
                        this.lexer = newLexer;
                        if (lexer.getMasterAutomaton() != null) {
                            automataDetailsArea.setText(lexer.getMasterAutomaton().toString());
                        }
                        outputArea.setText("✅ Lexer successfully built from: " + file.getName());
                        setButtonsDisabled(false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        outputArea.setText("❌ Error building Lexer: " + e.getMessage());
                        this.lexer = null;
                        setButtonsDisabled(false);
                    });
                }
            }).start();
        }
    }

    private void setButtonsDisabled(boolean disabled) {
        if(loadTokenBtn != null) loadTokenBtn.setDisable(disabled);
        if(loadInputBtn != null) loadInputBtn.setDisable(disabled);
        if(runLexerBtn != null) runLexerBtn.setDisable(disabled);
    }

    @FXML
    private void handleLoadInputFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Input Text File");

        Window stage = inputArea.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            inputFileLabel.setText(file.getName());

            try {
                String content = Utils.readTextFile(file.getAbsolutePath());
                inputArea.setText(content);
                outputArea.setText("Input file loaded. Ready for analysis.");
            } catch (IOException e) {
                outputArea.setText("❌ Error reading input file: " + e.getMessage());
            }
        }
    }

   @FXML
    private void handleRunLexer(ActionEvent event) {
        if (lexer == null) {
            outputArea.setText("⚠️ Please load a Lexer RE and Tokens file first.");
            return;
        }

        String input = inputArea.getText();

        if (input == null || input.trim().isEmpty()) {
            outputArea.setText("⚠️ No input code found to analyze.");
            return;
        }

        try {
            // 1. Run the Lexer analysis
            String result = lexer.scan(input);
            outputArea.setText(result);

            // 2. Populate the Symbol Table View
            if (lexer.getSymbolTable() != null) {
                ObservableList<Symbol> symbols = FXCollections.observableArrayList(
                        lexer.getSymbolTable().getTable().values()
                );
                symbolTableViewer.setItems(symbols);
            }

            // 3. Run Parser calculations and update First/Follow Table
            try {
                // Re-build grammar and calculate sets
                Grammar grammar = GrammarBuilder.buildFromBnfFile("src/main/resources/core/lexer/awk-bnf.txt"); 
                FirstFollowCalculator calculator = new FirstFollowCalculator(grammar);
                calculator.computeSets();

                // Retrieve the computed rows directly from the calculator's results table
                // This replaces the broken getFirstSets() and getFollowSets() logic
                var tableData = calculator.getResultsTable().getTable();
                ObservableList<FirstFollowRow> rows = FXCollections.observableArrayList(tableData.values());
                
                firstFollowTable.setItems(rows);
                
            } catch (Exception e) {
                System.err.println("Parser Error during UI update: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            outputArea.setText("❌ Analysis Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveConsoleLog(ActionEvent event) {
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

    @FXML
    private void handleClearTables(ActionEvent event) {
        // 1. Clear the Symbol Table View
        if (symbolTableViewer != null) {
            symbolTableViewer.getItems().clear();
        }

        // 2. Clear the First & Follow Table View
        if (firstFollowTable != null) {
            firstFollowTable.getItems().clear();
        }

        // 3. Optional: Clear the backend SymbolTable in the lexer if it exists
        if (lexer != null && lexer.getSymbolTable() != null) {
            lexer.getSymbolTable().clearTable();
        }

        // 4. Clear the UI Output areas
        outputArea.setText("Tables cleared.");
        consoleArea.appendText("\n[UI] Tables and internal symbol cache have been cleared.\n");
    }

    /**
     * Converte recursivamente um SyntaxTreeNode para um TreeItem do JavaFX.
     * @param node O nó da árvore sintática do seu modelo.
     * @return Um TreeItem formatado para exibição na UI.
     */
    private TreeItem<String> convertToTreeItem(SyntaxTreeNode node) {
        if (node == null) return null;

        // Cria o item da árvore com o rótulo (label) do nó
        TreeItem<String> item = new TreeItem<>(node.getLabel());

        // Percorre recursivamente todos os filhos e os adiciona ao item atual
        for (SyntaxTreeNode child : node.getChildren()) {
            item.getChildren().add(convertToTreeItem(child));
        }

        // Define o item como expandido por padrão para facilitar a visualização
        item.setExpanded(true);
        return item;
    }

    private void updateSyntaxTree(SyntaxTreeNode root) {
        if (root != null) {
            syntaxTreeView.setRoot(convertToTreeItem(root));
        }
    }

    /**
     * Método para ser chamado após o Parser gerar a árvore sintática.
     * @param root O nó raiz da árvore sintática gerada.
     */
    private void displaySyntaxTree(SyntaxTreeNode root) {
        if (root != null) {
            // Converte o modelo e define como raiz do componente visual
            syntaxTreeView.setRoot(convertToTreeItem(root));
        } else {
            syntaxTreeView.setRoot(null);
        }
    }
}