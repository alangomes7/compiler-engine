package app.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import models.FirstFollowRow;
import models.Grammar;
import models.Symbol;
import models.TokenRule;
import core.lexer.Lexer;
import core.lexer.TokenReader;
import core.parser.FirstFollowCalculator;
import core.parser.GrammarBuilder;
import Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

public class LexerUI {

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
    
    @FXML private TableView<FirstFollowRow> firstFollowTable;
    @FXML private TableColumn<FirstFollowRow, String> nonTerminalCol;
    @FXML private TableColumn<FirstFollowRow, String> firstSetCol;
    @FXML private TableColumn<FirstFollowRow, String> followSetCol;

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
        nonTerminalCol.setCellValueFactory(new PropertyValueFactory<>("nonTerminal"));
        firstSetCol.setCellValueFactory(new PropertyValueFactory<>("firstSet"));
        followSetCol.setCellValueFactory(new PropertyValueFactory<>("followSet"));
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
                    writer.println(row.getNonTerminal() + ",\"" + row.getFirstSet() + "\",\"" + row.getFollowSet() + "\"");
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
            
            // 1. Notify user and disable buttons
            outputArea.setText("⏳ Building Lexer... Please wait.");
            setButtonsDisabled(true);

            // 2. Run the heavy build process on a background thread
            new Thread(() -> {
                try {
                    List<TokenRule> rules = TokenReader.readTokens(file.getAbsolutePath());
                    Lexer newLexer = new Lexer(rules);

                    // 3. Update UI back on the JavaFX Application Thread on success
                    Platform.runLater(() -> {
                        this.lexer = newLexer;
                        outputArea.setText("✅ Lexer successfully built from: " + file.getName());
                        setButtonsDisabled(false);
                    });
                } catch (Exception e) {
                    // 3. Update UI back on the JavaFX Application Thread on failure
                    Platform.runLater(() -> {
                        outputArea.setText("❌ Error building Lexer: " + e.getMessage());
                        this.lexer = null;
                        setButtonsDisabled(false);
                    });
                }
            }).start();
        }
    }

    /**
     * Helper method to toggle the disabled state of main action buttons.
     */
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
            // Run the Lexer analysis
            String result = lexer.scan(input);
            outputArea.setText(result);

            // Populate the Symbol Table View
            if (lexer.getSymbolTable() != null) {
                ObservableList<Symbol> symbols = FXCollections.observableArrayList(
                        lexer.getSymbolTable().getTable().values()
                );
                symbolTableViewer.setItems(symbols);
            }

            // Run Parser calculations
            try {
                Grammar grammar = GrammarBuilder.buildFromBnfFile("src/main/resources/core/lexer/awk-bnf.txt"); 
                FirstFollowCalculator calculator = new FirstFollowCalculator(grammar);
                calculator.computeSets();

                ObservableList<FirstFollowRow> rows = FXCollections.observableArrayList();
                
                // Populate rows dynamically using the calculator
                for (String nt : grammar.getNonTerminals()) {
                    String firstStr = calculator.getFirstSets().get(nt).toString();
                    String followStr = calculator.getFollowSets().get(nt).toString();
                    rows.add(new FirstFollowRow(nt, firstStr, followStr));
                }
                
                firstFollowTable.setItems(rows);
            } catch (Exception e) {
                System.err.println("Parser Error: " + e.getMessage());
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

    /**
     * Inner class to redirect output streams to the JavaFX TextArea
     */
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