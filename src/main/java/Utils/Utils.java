package Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

/**
 * A utility class providing common file I/O operations and utility methods for reading grammar
 * files, handling text files with UTF-8 encoding, and saving JavaFX snapshots as PNG images.
 *
 * <p>This class is final and has a private constructor to prevent instantiation. All methods are
 * static and designed for convenient access.
 */
public final class Utils {

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always thrown to indicate that this class cannot be
     *     instantiated
     */
    private Utils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Reads a grammar file, ignoring comments and empty lines.
     *
     * <p>Lines that start with '#' are treated as comments and ignored. Empty lines (after
     * trimming) are also filtered out. All remaining lines are joined together with newline
     * characters.
     *
     * @param filePath the path to the grammar file to read
     * @return a single string containing all non-comment, non-empty grammar rules, with each rule
     *     separated by a newline character
     * @throws IOException if an I/O error occurs while reading the file
     * @throws java.nio.file.NoSuchFileException if the file does not exist
     */
    public static String readGrammar(String filePath) throws IOException {
        return Files.lines(Path.of(filePath))
                .map(String::trim)
                // Filter out empty lines and lines starting with #
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Reads the entire content of a text file using strict UTF-8 encoding.
     *
     * <p>This method reads all characters from the specified file using UTF-8 character encoding.
     * The content is returned as a single string.
     *
     * @param filePath the path to the text file to read
     * @return the complete content of the file as a string
     * @throws IOException if an I/O error occurs while reading the file
     * @throws java.nio.file.NoSuchFileException if the file does not exist
     * @throws java.nio.charset.MalformedInputException if the file contains bytes that are invalid
     *     for UTF-8 encoding
     */
    public static String readTextFile(String filePath) throws IOException {
        return Files.readString(Path.of(filePath), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Writes a string to a text file using strict UTF-8 encoding.
     *
     * <p>This method writes the specified content to the given file using UTF-8 character encoding.
     * If the file already exists, it will be overwritten. If the file does not exist, it will be
     * created.
     *
     * @param filePath the path to the text file to write to
     * @param content the string content to write to the file
     * @throws IOException if an I/O error occurs while writing to the file
     * @throws java.nio.file.AccessDeniedException if the file is read-only or cannot be opened for
     *     writing
     */
    public static void writeTextFile(String filePath, String content) throws IOException {
        Path path = Path.of(filePath);
        Files.writeString(path, content, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Appends a string to the end of an existing text file using strict UTF-8 encoding.
     *
     * <p>This method adds the specified content to the end of the given file using UTF-8 character
     * encoding. If the file does not exist, it will be created automatically. The existing content
     * of the file is preserved.
     *
     * @param filePath the path to the text file to append to
     * @param content the string content to append to the file
     * @throws IOException if an I/O error occurs while writing to the file
     * @throws java.nio.file.AccessDeniedException if the file is read-only or cannot be opened for
     *     writing
     */
    public static void appendToTextFile(String filePath, String content) throws IOException {
        Files.writeString(
                Path.of(filePath),
                content,
                java.nio.charset.StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    /**
     * Saves a JavaFX WritableImage snapshot as a PNG file.
     *
     * <p>This method converts a JavaFX image to a BufferedImage and then writes it to the specified
     * file in PNG format. Useful for saving screenshots or rendered scenes.
     *
     * @param snapshot the JavaFX WritableImage to save (typically from a snapshot operation)
     * @param outputFile the file where the PNG image will be written
     * @throws IOException if an I/O error occurs while writing the image file
     * @throws NullPointerException if either parameter is null
     * @throws javax.imageio.IIOException if the output file cannot be created or written, or if the
     *     PNG format is not supported
     */
    public static void saveSnapshot(javafx.scene.image.WritableImage snapshot, File outputFile)
            throws IOException {
        java.awt.image.BufferedImage bufferedImage =
                javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null);
        javax.imageio.ImageIO.write(bufferedImage, "png", outputFile);
    }
}
