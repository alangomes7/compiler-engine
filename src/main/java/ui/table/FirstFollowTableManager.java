package ui.table;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import core.parser.models.FirstFollowTable;
import core.parser.models.atomic.Symbol;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 * Configures the First/Follow table columns and cell factories.
 * The table data depends on a dynamically changing FirstFollowTable instance.
 */
public class FirstFollowTableManager {

    private final TableView<Symbol> table;
    private final Supplier<FirstFollowTable> currentTableSupplier;

    public FirstFollowTableManager(TableView<Symbol> table, Supplier<FirstFollowTable> currentTableSupplier) {
        this.table = table;
        this.currentTableSupplier = currentTableSupplier;
    }

    public void setupColumns(TableColumn<Symbol, String> nonTerminalCol,
                             TableColumn<Symbol, Set<Symbol>> firstSetCol,
                             TableColumn<Symbol, Set<Symbol>> followSetCol) {
        nonTerminalCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        firstSetCol.setCellValueFactory(data -> {
            FirstFollowTable ff = currentTableSupplier.get();
            return ff == null ? new SimpleObjectProperty<>(null) : new SimpleObjectProperty<>(ff.getFirst(data.getValue()));
        });

        followSetCol.setCellValueFactory(data -> {
            FirstFollowTable ff = currentTableSupplier.get();
            return ff == null ? new SimpleObjectProperty<>(null) : new SimpleObjectProperty<>(ff.getFollow(data.getValue()));
        });

        Callback<TableColumn<Symbol, Set<Symbol>>, TableCell<Symbol, Set<Symbol>>> cellFactory = column -> new TableCell<>() {
            @Override
            protected void updateItem(Set<Symbol> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText("");
                else setText("{ " + item.stream().map(Symbol::getName).collect(Collectors.joining(", ")) + " }");
            }
        };

        firstSetCol.setCellFactory(cellFactory);
        followSetCol.setCellFactory(cellFactory);
    }
}