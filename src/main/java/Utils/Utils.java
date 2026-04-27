package Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Reads a grammar file, ignoring comments starting with '#' and empty lines. Returns a cleaned,
     * single string of the grammar rules.
     */
    public static String readGrammar(String filePath) throws IOException {
        return Files.lines(Path.of(filePath))
                .map(String::trim)
                // Filter out empty lines and lines starting with #
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.joining("\n"));
    }

    /** Reads the content of a .txt file using strict UTF-8. */
    public static String readTextFile(String filePath) throws IOException {
        return Files.readString(Path.of(filePath), java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Writes a string to a .txt file using strict UTF-8. */
    public static void writeTextFile(String filePath, String content) throws IOException {
        Path path = Path.of(filePath);
        Files.writeString(path, content, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Appends a string to the end of an existing .txt file using strict UTF-8. */
    public static void appendToTextFile(String filePath, String content) throws IOException {
        Files.writeString(
                Path.of(filePath),
                content,
                java.nio.charset.StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    public static void saveSnapshot(javafx.scene.image.WritableImage snapshot, File outputFile)
            throws IOException {
        java.awt.image.BufferedImage bufferedImage =
                javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null);
        javax.imageio.ImageIO.write(bufferedImage, "png", outputFile);
    }
}
