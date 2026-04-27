package ui.core.handlers;

import java.io.File;
import java.io.IOException;
import javafx.scene.image.WritableImage;
import ui.Ui;
import ui.core.graph.automata.AutomataVisualizer;
import ui.core.graph.tree.InteractiveTreeView;
import ui.core.services.FileService;
import ui.core.state.AnalysisState;
import ui.core.tables.FirstFollowTableManager;
import ui.core.tables.ParserTableManager;
import ui.core.tables.SymbolTableManager;
import ui.util.UiUtils;

/**
 * Handles exporting various analysis artifacts: automaton images, grammar/parse tree snapshots, CSV
 * tables, console logs, and full reports.
 *
 * @author Generated
 * @version 1.0
 */
public class ExportHandler {
    private final Ui ui;
    private final AnalysisState state;

    /**
     * Constructs an ExportHandler with references to the main UI and analysis state.
     *
     * @param ui the main UI instance
     * @param state the shared analysis state
     */
    public ExportHandler(Ui ui, AnalysisState state) {
        this.ui = ui;
        this.state = state;
    }

    /**
     * Exports the current DFA automaton as a PNG image using Graphviz. The image is saved in the
     * "output" directory with a timestamped filename.
     */
    public void handleExportGraphImage() {
        if (state.getCurrentAutomaton() == null) return;
        try {
            String fileName = UiUtils.timestampFileName("automata", "png", true);
            AutomataVisualizer.exportToImage(state.getCurrentAutomaton(), fileName);
            ui.getOutputArea().setText("Automata image exported natively as: " + fileName);
        } catch (Exception e) {
            ui.getOutputArea().setText("Export failed: " + e.getMessage());
        }
    }

    /** Exports the grammar tree (visualisation of the grammar structure) as a PNG image. */
    public void handleExportGrammarTreeImage() {
        if (!state.isHasGrammarTree()) return;
        exportTreeSnapshot(
                (InteractiveTreeView) ui.getGrammarTreeContainer().getCenter(), "grammar_tree");
    }

    /** Exports the parse tree of the last successfully parsed input as a PNG image. */
    public void handleExportInputTreeImage() {
        if (!state.isHasInputTree()) return;
        exportTreeSnapshot(
                (InteractiveTreeView) ui.getInputTreeContainer().getCenter(), "input_tree");
    }

    /**
     * Helper method to export an interactive tree view to a PNG file via a save dialog.
     *
     * @param view the tree view (must be an InteractiveTreeView)
     * @param baseName the base name for the suggested file
     */
    private void exportTreeSnapshot(InteractiveTreeView view, String baseName) {
        try {
            File file =
                    FileService.saveStringToFile(
                            ui.getInputArea().getScene().getWindow(),
                            "",
                            baseName + ".png",
                            "*.png");
            if (file != null) {
                WritableImage snapshot = view.generateSnapshot();
                UiUtils.saveSnapshot(snapshot, file);
                ui.getOutputArea().setText("Tree exported successfully to " + file.getName());
            }
        } catch (IOException e) {
            ui.getOutputArea().setText("Export failed: " + e.getMessage());
        }
    }

    /**
     * Exports the symbol table, FIRST/FOLLOW sets, and parse table as CSV files (saved in the
     * project root).
     */
    public void handleExportCSV() {
        try {
            if (state.isHasSymbolTableData()) {
                SymbolTableManager.exportSymbolTableCsv(
                        "symbol_table.csv", ui.getSymbolTableViewer());
            }
            if (state.isHasFirstFollowData()) {
                FirstFollowTableManager.exportFirstFollowCsv(
                        "first_follow.csv",
                        state.getCurrentFirstFollowTable(),
                        ui.getFirstFollowTable());
            }
            if (state.isHasParseTableData()) {
                ParserTableManager.exportParseTableCsv(
                        "parse_table.csv", state.getCurrentParseTable(), ui.getParserTable());
            }
            ui.getOutputArea().setText("Tables successfully exported to CSVs in project root.");
        } catch (IOException e) {
            ui.getOutputArea().setText("CSV Export failed: " + e.getMessage());
        }
    }

    /** Exports the Graphviz DOT representation of the current DFA to a text file. */
    public void handleExportGraphText() {
        if (state.getCurrentAutomaton() == null) return;
        try {
            String dotFormat = AutomataVisualizer.generateDotFormat(state.getCurrentAutomaton());
            File file =
                    FileService.saveStringToFile(
                            ui.getInputArea().getScene().getWindow(),
                            dotFormat,
                            "automata.dot",
                            "*.dot",
                            "*.txt");
            if (file != null) {
                ui.getOutputArea().setText("Graph definition exported to " + file.getName());
            }
        } catch (IOException e) {
            ui.getOutputArea().setText("Export failed: " + e.getMessage());
        }
    }

    /** Saves the contents of the console log area to a text file. */
    public void handleSaveConsoleLog() {
        saveTextArea(ui.getConsoleArea(), "console_log", "*.txt");
    }

    /** Saves the contents of the output area to a text file. */
    public void handleSaveOutput() {
        saveTextArea(ui.getOutputArea(), "output_log", "*.txt");
    }

    /** Saves the contents of the validation output area to a text file. */
    public void handleSaveValidation() {
        saveTextArea(ui.getValidatorOutputArea(), "validation_report", "*.txt");
    }

    /**
     * Helper to save a TextArea's content to a file via a save dialog.
     *
     * @param area the TextArea to save
     * @param baseName the base name for the suggested file
     * @param extension the file extension filter (e.g., "*.txt")
     */
    private void saveTextArea(
            javafx.scene.control.TextArea area, String baseName, String extension) {
        try {
            File file =
                    FileService.saveStringToFile(
                            area.getScene().getWindow(),
                            area.getText(),
                            baseName + ".txt",
                            extension);
            if (file != null) ui.getOutputArea().setText(baseName + " saved to " + file.getName());
        } catch (IOException e) {
            ui.getOutputArea().setText("Save failed: " + e.getMessage());
        }
    }

    /**
     * Generates a comprehensive report combining output log, console log, and validation results.
     * Prompts the user to save the report as a text file.
     */
    public void handleGenerateFullReport() {
        try {
            StringBuilder report = new StringBuilder();
            report.append("=== COMPILER REPORT ===\n\n");
            report.append("--- Output Log ---\n")
                    .append(ui.getOutputArea().getText())
                    .append("\n\n");
            report.append("--- Console Log ---\n")
                    .append(ui.getConsoleArea().getText())
                    .append("\n\n");
            if (state.isHasValidationData()) {
                report.append("--- Grammar Validation ---\n")
                        .append(ui.getValidatorOutputArea().getText())
                        .append("\n\n");
            }
            FileService.saveStringToFile(
                    ui.getInputArea().getScene().getWindow(),
                    report.toString(),
                    "full_report.txt",
                    "*.txt");
        } catch (IOException e) {
            ui.getOutputArea().setText("Report generation failed: " + e.getMessage());
        }
    }
}
