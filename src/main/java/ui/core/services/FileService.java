package ui.core.services;

import Utils.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Provides common file operations for the UI layer.
 *
 * <p>This service encapsulates file-related functionality including:
 *
 * <ul>
 *   <li>Opening file chooser dialogs for file selection
 *   <li>Reading text files with automatic stray character filtering
 *   <li>Saving strings to files with "Save As" dialogs
 *   <li>CSV field escaping for safe data export
 * </ul>
 *
 * <p>All methods are static for convenient access throughout the UI layer. The service
 * automatically handles UTF-8 encoding for all file operations.
 *
 * <p>Typical usage:
 *
 * <pre>
 * // Select and read a grammar file
 * File selectedFile = FileService.selectFile(window, "Select Grammar", "*.txt", "*.grammar");
 * if (selectedFile != null) {
 *     String content = FileService.readFileContent(selectedFile);
 *     // Process content...
 * }
 *
 * // Save results to a CSV file
 * String csvContent = "Name,Value\n\"Hello, World\",42";
 * File savedFile = FileService.saveStringToFile(window, csvContent, "output.csv", "*.csv");
 * </pre>
 *
 * @see Utils
 * @see StrayCharacterFilter
 */
public class FileService {

    /**
     * Opens a file chooser dialog and returns the selected file.
     *
     * <p>This method creates a platform-native file chooser dialog filtered by the specified
     * extensions. The dialog is modal and will block input to the owner window until the user makes
     * a selection or cancels.
     *
     * @param owner the owner window for the dialog (may be null, in which case a platform-dependent
     *     parent is used)
     * @param title the title to display in the file chooser dialog
     * @param extensions file extension filters to display (e.g., "*.txt", "*.grammar"); if empty,
     *     no filter is applied
     * @return the selected file, or {@code null} if the user cancelled the dialog
     */
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

    /**
     * Reads the entire content of a text file and filters out stray characters.
     *
     * <p>This method reads the file using UTF-8 encoding and then passes the content through {@link
     * StrayCharacterFilter#filter(String)} to remove any non-printable or problematic characters
     * that could break parsing.
     *
     * @param file the file to read; must not be null and must exist
     * @return the file content as a string, with stray characters removed
     * @throws Exception if the file cannot be read (delegated from {@link Utils#readTextFile})
     * @throws NullPointerException if file is null
     * @throws java.nio.file.NoSuchFileException if the file does not exist
     */
    public static String readFileContent(File file) throws Exception {
        String fileContentRaw = Utils.readTextFile(file.getAbsolutePath());
        String clean = StrayCharacterFilter.filter(fileContentRaw);
        return clean;
    }

    /**
     * Escapes a string for safe inclusion in a CSV field.
     *
     * <p>According to CSV standards, double-quote characters within a field must be escaped by
     * doubling them (i.e., {@code "} becomes {@code ""}). This method performs that escaping,
     * preparing the string for use in a CSV cell that will be quoted.
     *
     * <p>Note: This method does NOT add surrounding quotes to the field; it only escapes internal
     * quotes. The caller is responsible for adding quotes if needed.
     *
     * @param data the raw string to escape (may be null)
     * @return the escaped string suitable for CSV inclusion, or an empty string if the input is
     *     null
     * @see <a href="https://tools.ietf.org/html/rfc4180">RFC 4180: CSV Format</a>
     */
    public static String escapeCsv(String data) {
        if (data == null) return "";
        return data.replace("\"", "\"\"");
    }

    /**
     * Shows a "Save As" dialog and writes the given string content to the selected file.
     *
     * <p>This method presents a file save dialog to the user, optionally suggesting an initial
     * filename and filtering by file extensions. If the user selects a file location, the content
     * is written to that file using UTF-8 encoding.
     *
     * <p>The dialog blocks until the user either confirms a file location or cancels. If the
     * selected file already exists, the user will be prompted for confirmation.
     *
     * @param owner the owner window for the dialog (may be null)
     * @param content the string content to write to the file
     * @param initialFileName suggested file name (e.g., "output.txt"); may be null
     * @param extensions optional file extensions for the filter (e.g., "*.txt", "*.csv"); if empty,
     *     no filter is applied
     * @return the saved File object, or {@code null} if the user cancelled the dialog
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
     * Writes a string directly to a file, overwriting any existing content.
     *
     * <p>This method writes the content using UTF-8 encoding. If the file does not exist, it is
     * created. If it does exist, its content is replaced.
     *
     * <p>For a user-interactive save operation with a file chooser, use {@link
     * #saveStringToFile(Window, String, String, String...)} instead.
     *
     * @param file the target file to write to; must not be null
     * @param content the string content to write
     * @throws IOException if writing fails due to permission issues, disk full, or other I/O errors
     * @throws NullPointerException if file is null
     */
    public static void writeStringToFile(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
}
