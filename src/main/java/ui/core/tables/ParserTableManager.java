package ui.core.tables;

import static ui.core.services.FileService.escapeCsv;

import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** Dynamically builds and populates the LL(1) parsing table with columns for each terminal. */
public class ParserTableManager {

    private final TableView<Symbol> table;
    private final TableColumn<Symbol, String> nonTerminalCol;

    public ParserTableManager(TableView<Symbol> table, TableColumn<Symbol, String> nonTerminalCol) {
        this.table = table;
        this.nonTerminalCol = nonTerminalCol;
        setupBaseColumn();
    }

    private void setupBaseColumn() {
        nonTerminalCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getName()));
    }

    /**
     * Populates the table with the given parse table data. Dynamically adds a column for each
     * terminal symbol.
     */
    public void populate(ParseTable parseTable) {
        // Reset to only the non‑terminal column
        table.getColumns().setAll(List.of(nonTerminalCol));
        if (parseTable == null || parseTable.getTable().isEmpty()) {
            table.getItems().clear();
            return;
        }

        Map<Symbol, Map<Symbol, List<Production>>> innerTable = parseTable.getTable();

        // Collect all terminal symbols that appear in the table
        Set<Symbol> terminals =
                innerTable.values().stream()
                        .flatMap(row -> row.keySet().stream())
                        .collect(Collectors.toSet());

        List<Symbol> sortedTerminals = new ArrayList<>(terminals);
        sortedTerminals.sort(
                (t1, t2) -> {
                    if (t1.getName().equals("$")) return 1;
                    if (t2.getName().equals("$")) return -1;
                    return t1.getName().compareTo(t2.getName());
                });

        // Create a column for each terminal
        for (Symbol terminal : sortedTerminals) {
            TableColumn<Symbol, String> terminalCol = new TableColumn<>(terminal.getName());
            terminalCol.setCellValueFactory(
                    data -> {
                        Symbol nonTerminal = data.getValue();
                        List<Production> prods = parseTable.getEntry(nonTerminal, terminal);
                        if (prods == null || prods.isEmpty()) {
                            return new SimpleStringProperty("");
                        }
                        String text =
                                prods.stream()
                                        .map(Production::toString)
                                        .collect(Collectors.joining("\n"));
                        return new SimpleStringProperty(text);
                    });

            terminalCol.setCellFactory(
                    col ->
                            new TableCell<>() {
                                @Override
                                protected void updateItem(String item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (empty || item == null) {
                                        setText(null);
                                        setStyle("");
                                    } else {
                                        setText(item);
                                        if (item.contains("\n")) {
                                            setStyle(
                                                    "-fx-background-color: #ffe6e6; -fx-text-fill: #d8000c; -fx-font-weight: bold;");
                                        } else {
                                            setStyle("");
                                        }
                                    }
                                }
                            });

            table.getColumns().add(terminalCol);
        }

        table.setItems(FXCollections.observableArrayList(innerTable.keySet()));
    }

    public void clear() {
        table.getItems().clear();
        table.getColumns().setAll(List.of(nonTerminalCol));
    }

    public static void exportParseTableCsv(
            String path, ParseTable currentParseTable, TableView<Symbol> parserTable)
            throws java.io.IOException {
        if (currentParseTable == null) return;
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(path))) {

            // The parse table generates columns dynamically, so we fetch them directly from the UI
            java.util.List<javafx.scene.control.TableColumn<Symbol, ?>> columns =
                    parserTable.getColumns();

            // Write Headers
            java.util.List<String> headers = new java.util.ArrayList<>();
            for (javafx.scene.control.TableColumn<Symbol, ?> col : columns) {
                headers.add("\"" + escapeCsv(col.getText()) + "\"");
            }
            writer.println(String.join(",", headers));

            // Write Data Row by Row
            for (Symbol s : parserTable.getItems()) {
                java.util.List<String> rowData = new java.util.ArrayList<>();
                for (javafx.scene.control.TableColumn<Symbol, ?> col : columns) {
                    Object cellData = col.getCellData(s);
                    String cellString = cellData != null ? cellData.toString() : "";
                    rowData.add("\"" + escapeCsv(cellString) + "\"");
                }
                writer.println(String.join(",", rowData));
            }
        }
    }
}
