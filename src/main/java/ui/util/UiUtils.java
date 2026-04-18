package ui.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class UiUtils {

    public static class TextAreaOutputStream extends OutputStream {
        private final TextArea console;
        private final StringBuilder buffer = new StringBuilder();

        public TextAreaOutputStream(TextArea console) {
            this.console = console;
        }

        @Override
        public void write(int b) {
            char c = (char) b;
            buffer.append(c);
            if (c == '\n') {
                flushBuffer();
            }
        }

        private void flushBuffer() {
            String text = buffer.toString();
            buffer.setLength(0);
            Platform.runLater(
                    () -> {
                        console.appendText(text);
                        console.setScrollTop(Double.MAX_VALUE);
                    });
        }
    }

    /** Updates line numbers in a companion TextArea based on the input TextArea content. */
    public static void updateLineNumbers(TextArea input, TextArea lineNumbers) {
        int lines = input.getText().split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) sb.append(i).append("\n");
        lineNumbers.setText(sb.toString());
    }

    /** Saves a JavaFX snapshot (WritableImage) to a PNG file. */
    public static void saveSnapshot(javafx.scene.image.WritableImage snapshot, File outputFile)
            throws IOException {
        java.awt.image.BufferedImage bufferedImage =
                javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null);
        javax.imageio.ImageIO.write(bufferedImage, "png", outputFile);
    }

    // ==================== TIMESTAMP UTILITIES ====================

    /** Returns a timestamp formatted for console/log display (HH:mm:ss). Example: "14:30:22" */
    public static String getDisplayTimestamp() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /** Returns a timestamp suitable for filenames (yyyyMMdd_HHmmss). Example: "20250418_143022" */
    public static String getFileTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    /**
     * Convenience method to generate a file name with a timestamp prefix or suffix.
     *
     * @param baseName e.g. "export", "log"
     * @param extension e.g. "txt", "png" (without dot)
     * @param addTimestampBeforeExtension if true: "export_20250418_143022.txt", if false:
     *     "20250418_143022_export.txt"
     * @return complete filename string
     */
    public static String timestampFileName(
            String baseName, String extension, boolean addTimestampBeforeExtension) {
        String ts = getFileTimestamp();
        String fileName;
        if (addTimestampBeforeExtension) {
            fileName = baseName + "_" + ts + "." + extension;
        } else {
            fileName = ts + "_" + baseName + "." + extension;
        }
        return fileName;
    }
}
