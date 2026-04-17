package Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

public final class UiUtils {

    private UiUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Reads a grammar file, ignoring comments starting with '#' and empty lines.
     * Returns a cleaned, single string of the grammar rules.
     */
    public static String readGrammar(String filePath) throws IOException {
        return Files.lines(Path.of(filePath))
                .map(String::trim)
                // Filter out empty lines and lines starting with #
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Reads the content of a .txt file.
     */
    public static String readTextFile(String filePath) throws IOException {
        return Files.readString(Path.of(filePath));
    }

    /**
     * Writes a string to a .txt file. 
     * If the file exists, it overwrites it. If not, it creates it.
     * @param filePath The destination path.
     * @param content  The string to write.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeTextFile(String filePath, String content) throws IOException {
        Path path = Path.of(filePath);
        // StandardOpenOption.CREATE and TRUNCATE_EXISTING are default behaviors
        Files.writeString(path, content);
    }

    /**
     * Appends a string to the end of an existing .txt file.
     */
    public static void appendToTextFile(String filePath, String content) throws IOException {
        Files.writeString(
            Path.of(filePath), 
            content, 
            StandardOpenOption.CREATE, 
            StandardOpenOption.APPEND
        );
    }

    public static void saveSnapshot(javafx.scene.image.WritableImage snapshot, File outputFile) throws IOException {
        java.awt.image.BufferedImage bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null);
        javax.imageio.ImageIO.write(bufferedImage, "png", outputFile);
    }
}