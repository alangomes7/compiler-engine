package ui.table;

import core.lexer.models.atomic.Token;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;

/**
 * Configures the columns of the symbol table TableView.
 */
public class SymbolTableManager {

    public static void setupColumns(TableColumn<Token, Integer> lineCol,
                                    TableColumn<Token, Integer> colCol,
                                    TableColumn<Token, String> lexemeCol,
                                    TableColumn<Token, String> typeCol) {
        lexemeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLexeme()));
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTokenType()));
        lineCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getLine()).asObject());
        colCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCol()).asObject());
    }
}