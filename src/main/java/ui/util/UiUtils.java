package ui.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UiUtils {

    private static final Logger log = LoggerFactory.getLogger(UiUtils.class);

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

        @Override
        public void write(byte[] b, int off, int len) {
            for (int i = off; i < off + len; i++) {
                write(b[i]);
            }
        }

        private void flushBuffer() {
            String text = buffer.toString();
            buffer.setLength(0);
            Platform.runLater(
                    () -> {
                        console.appendText(text);
                        console.positionCaret(console.getLength());
                    });
        }
    }

    public static void updateLineNumbers(TextArea input, TextArea lineNumbers) {
        int lines = input.getText().split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) sb.append(i).append("\n");
        lineNumbers.setText(sb.toString());
    }

    public static void saveSnapshot(javafx.scene.image.WritableImage snapshot, File outputFile)
            throws IOException {
        java.awt.image.BufferedImage bufferedImage =
                javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null);
        javax.imageio.ImageIO.write(bufferedImage, "png", outputFile);
    }

    public static String getDisplayTimestamp() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public static String getFileTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

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

    public static void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) {
            log.error("Operation cancelled by user.");
        }
    }
}
