package ui.core.services;

import Utils.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class FileService {

    public static File selectFile(Window owner, String title, String... extensions) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        if (extensions.length > 0) {
            fileChooser
                    .getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Supported Files", extensions));
        }
        return fileChooser.showOpenDialog(owner);
    }

    public static String readFileContent(File file) throws Exception {
        return Utils.readTextFile(file.getAbsolutePath());
    }

    /**
     * Helper to correctly escape internal double quotes. Wrapping values in quotes handles internal
     * commas and line-breaks.
     */
    public static String escapeCsv(String data) {
        if (data == null) return "";
        return data.replace("\"", "\"\"");
    }

    /**
     * Shows a "Save As" dialog and writes the given string content to the selected file.
     *
     * @param owner the owner window for the dialog
     * @param content the string to write
     * @param initialFileName suggested file name (e.g. "output.txt")
     * @param extensions optional file extensions for the filter (e.g. "*.txt")
     * @return the saved File, or null if the user cancelled the dialog
     * @throws IOException if an I/O error occurs while writing the file
     */
    public static File saveStringToFile(
            Window owner, String content, String initialFileName, String... extensions)
            throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        if (initialFileName != null) {
            fileChooser.setInitialFileName(initialFileName);
        }
        if (extensions.length > 0) {
            fileChooser
                    .getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Text Files", extensions));
        }
        File file = fileChooser.showSaveDialog(owner);
        if (file != null) {
            writeStringToFile(file, content);
        }
        return file;
    }

    /**
     * Writes a string directly to a file (overwrites existing content).
     *
     * @param file the target file
     * @param content the string to write
     * @throws IOException if writing fails
     */
    public static void writeStringToFile(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
}
