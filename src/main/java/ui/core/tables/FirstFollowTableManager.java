package ui.core.tables;

import static ui.core.services.FileService.escapeCsv;

import core.parser.models.FirstFollowTable;
import core.parser.models.atomic.Symbol;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 * Manages the FIRST/FOLLOW table UI component. Dynamically updates the table columns and cell
 * values based on a live FIRST/FOLLOW table supplier.
 *
 * @author Generated
 * @version 1.0
 */
public class FirstFollowTableManager {

    private final Supplier<FirstFollowTable> currentTableSupplier;

    /**
     * Constructs a manager for the FIRST/FOLLOW table.
     *
     * @param table the TableView that will display the data (not stored, only used for column
     *     setup)
     * @param currentTableSupplier supplier that provides the current FirstFollowTable instance
     *     (called on each cell refresh)
     */
    public FirstFollowTableManager(
            TableView<Symbol> table, Supplier<FirstFollowTable> currentTableSupplier) {
        this.currentTableSupplier = currentTableSupplier;
    }

    /**
     * Configures the three columns of the FIRST/FOLLOW table: non‑terminal name, FIRST set, and
     * FOLLOW set. Sets up cell value factories and a custom cell factory for set rendering.
     *
     * @param nonTerminalCol column displaying the non‑terminal name
     * @param firstSetCol column displaying the FIRST set for each non‑terminal
     * @param followSetCol column displaying the FOLLOW set for each non‑terminal
     */
    public void setupColumns(
            TableColumn<Symbol, String> nonTerminalCol,
            TableColumn<Symbol, Set<Symbol>> firstSetCol,
            TableColumn<Symbol, Set<Symbol>> followSetCol) {
        nonTerminalCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getName()));

        firstSetCol.setCellValueFactory(
                data -> {
                    FirstFollowTable ff = currentTableSupplier.get();
                    return ff == null
                            ? new SimpleObjectProperty<>(null)
                            : new SimpleObjectProperty<>(ff.getFirstSets().get(data.getValue()));
                });

        followSetCol.setCellValueFactory(
                data -> {
                    FirstFollowTable ff = currentTableSupplier.get();
                    return ff == null
                            ? new SimpleObjectProperty<>(null)
                            : new SimpleObjectProperty<>(ff.getFollowSets().get(data.getValue()));
                });

        Callback<TableColumn<Symbol, Set<Symbol>>, TableCell<Symbol, Set<Symbol>>> cellFactory =
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(Set<Symbol> item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) setText("");
                                else
                                    setText(
                                            "{ "
                                                    + item.stream()
                                                            .map(Symbol::getName)
                                                            .collect(Collectors.joining(", "))
                                                    + " }");
                            }
                        };

        firstSetCol.setCellFactory(cellFactory);
        followSetCol.setCellFactory(cellFactory);
    }

    /**
     * Exports the FIRST/FOLLOW table data to a CSV file.
     *
     * @param path output file path
     * @param currentFirstFollowTable the current FIRST/FOLLOW table (may be null)
     * @param firstFollowTable the TableView containing the non‑terminals (items)
     * @throws java.io.IOException if writing fails
     */
    public static void exportFirstFollowCsv(
            String path,
            FirstFollowTable currentFirstFollowTable,
            TableView<Symbol> firstFollowTable)
            throws java.io.IOException {
        if (currentFirstFollowTable == null) return;
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(path))) {
            writer.println("Non-Terminal,FIRST,FOLLOW");
            for (Symbol s : firstFollowTable.getItems()) {
                String firstSet =
                        currentFirstFollowTable.getFirstSets().get(s).stream()
                                .map(Symbol::getName)
                                .collect(java.util.stream.Collectors.joining(", "));

                String followSet =
                        currentFirstFollowTable.getFollowSets().get(s).stream()
                                .map(Symbol::getName)
                                .collect(java.util.stream.Collectors.joining(", "));

                writer.printf(
                        "\"%s\",\"%s\",\"%s\"%n",
                        escapeCsv(s.getName()),
                        escapeCsv("{ " + firstSet + " }"),
                        escapeCsv("{ " + followSet + " }"));
            }
        }
    }
}
