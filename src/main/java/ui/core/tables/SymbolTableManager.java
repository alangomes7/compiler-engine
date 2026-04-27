package ui.core.tables;

import static ui.core.services.FileService.escapeCsv;

import core.lexer.models.atomic.Token;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;

/**
 * Configures the columns of the symbol table TableView (lexer output). Provides a static method to
 * export the symbol table as CSV.
 */
public class SymbolTableManager {

    /**
     * Sets up the cell value factories for the four columns of the symbol table.
     *
     * @param lineCol column for the token's line number
     * @param colCol column for the token's column number
     * @param lexemeCol column for the token's lexeme (actual text)
     * @param typeCol column for the token's type (e.g., "IDENTIFIER")
     */
    public static void setupColumns(
            TableColumn<Token, Integer> lineCol,
            TableColumn<Token, Integer> colCol,
            TableColumn<Token, String> lexemeCol,
            TableColumn<Token, String> typeCol) {
        lexemeCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getLexeme()));
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        lineCol.setCellValueFactory(
                data -> new SimpleIntegerProperty(data.getValue().getLine()).asObject());
        colCol.setCellValueFactory(
                data -> new SimpleIntegerProperty(data.getValue().getCol()).asObject());
    }

    /**
     * Exports the symbol table data (tokens) to a CSV file.
     *
     * @param path output file path
     * @param symbolTableViewer the TableView containing the tokens (items)
     * @throws java.io.IOException if writing fails
     */
    public static void exportSymbolTableCsv(
            String path, javafx.scene.control.TableView<Token> symbolTableViewer)
            throws java.io.IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(path))) {
            writer.println("Lexeme,Token Type,Line,Column");
            for (Token t : symbolTableViewer.getItems()) {
                writer.printf(
                        "\"%s\",\"%s\",%d,%d%n",
                        escapeCsv(t.getLexeme()), escapeCsv(t.getType()), t.getLine(), t.getCol());
            }
        }
    }
}
