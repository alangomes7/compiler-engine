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

public class ExportHandler {
    private final Ui ui;
    private final AnalysisState state;

    public ExportHandler(Ui ui, AnalysisState state) {
        this.ui = ui;
        this.state = state;
    }

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

    public void handleExportGrammarTreeImage() {
        if (!state.isHasGrammarTree()) return;
        exportTreeSnapshot(
                (InteractiveTreeView) ui.getGrammarTreeContainer().getCenter(), "grammar_tree");
    }

    public void handleExportInputTreeImage() {
        if (!state.isHasInputTree()) return;
        exportTreeSnapshot(
                (InteractiveTreeView) ui.getInputTreeContainer().getCenter(), "input_tree");
    }

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

    public void handleSaveConsoleLog() {
        saveTextArea(ui.getConsoleArea(), "console_log", "*.txt");
    }

    public void handleSaveOutput() {
        saveTextArea(ui.getOutputArea(), "output_log", "*.txt");
    }

    public void handleSaveValidation() {
        saveTextArea(ui.getValidatorOutputArea(), "validation_report", "*.txt");
    }

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
