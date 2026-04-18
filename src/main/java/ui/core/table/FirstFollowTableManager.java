package ui.core.table;

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
 * Configures the First/Follow table columns and cell factories. The table data depends on a
 * dynamically changing FirstFollowTable instance.
 */
public class FirstFollowTableManager {

    private final Supplier<FirstFollowTable> currentTableSupplier;

    public FirstFollowTableManager(
            TableView<Symbol> table, Supplier<FirstFollowTable> currentTableSupplier) {
        this.currentTableSupplier = currentTableSupplier;
    }

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
                            : new SimpleObjectProperty<>(ff.getFirst(data.getValue()));
                });

        followSetCol.setCellValueFactory(
                data -> {
                    FirstFollowTable ff = currentTableSupplier.get();
                    return ff == null
                            ? new SimpleObjectProperty<>(null)
                            : new SimpleObjectProperty<>(ff.getFollow(data.getValue()));
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
                        currentFirstFollowTable.getFirst(s).stream()
                                .map(Symbol::getName)
                                .collect(java.util.stream.Collectors.joining(", "));

                String followSet =
                        currentFirstFollowTable.getFollow(s).stream()
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
