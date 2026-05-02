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

    public static String readGrammar(String filePath) throws IOException {
        return Files.lines(Path.of(filePath))
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.joining("\n"));
    }

    public static String readTextFile(String filePath) throws IOException {
        return Files.readString(Path.of(filePath), java.nio.charset.StandardCharsets.UTF_8);
    }

    public static void writeTextFile(String filePath, String content) throws IOException {
        Path path = Path.of(filePath);
        Files.writeString(path, content, java.nio.charset.StandardCharsets.UTF_8);
    }

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

    public static void createDirectories(String filePath) throws IOException {
        Path path = Path.of(filePath);
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }
}
