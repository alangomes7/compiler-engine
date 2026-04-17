package ui.table;

import core.lexer.models.atomic.Token;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import static ui.core.services.FileService.escapeCsv;

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

    public static void exportSymbolTableCsv(String path, javafx.scene.control.TableView<Token> symbolTableViewer) throws java.io.IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(path))) {
            writer.println("Lexeme,Token Type,Line,Column");
            for (Token t : symbolTableViewer.getItems()) {
                writer.printf("\"%s\",\"%s\",%d,%d%n",
                        escapeCsv(t.getLexeme()),
                        escapeCsv(t.getTokenType()),
                        t.getLine(),
                        t.getCol());
            }
        }
    }
}