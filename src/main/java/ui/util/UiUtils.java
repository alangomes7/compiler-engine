package ui.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

/**
 * Collection of utility methods for the UI layer.
 *
 * <p>This class provides various helper methods for common UI operations including:
 *
 * <ul>
 *   <li>Redirecting standard output streams to a JavaFX TextArea
 *   <li>Updating line numbers in a companion text area
 *   <li>Saving JavaFX snapshots to PNG files
 *   <li>Generating timestamps for console logs and filenames
 * </ul>
 *
 * <p>All methods are static and thread-safe where appropriate.
 */
public class UiUtils {

    /**
     * An OutputStream that writes text to a TextArea on the JavaFX application thread.
     *
     * <p>This class can be used to redirect System.out or System.err to a console UI component in a
     * JavaFX application. It buffers output line-by-line to ensure that each line is displayed as a
     * complete unit.
     *
     * <p>Typical usage:
     *
     * <pre>
     * TextArea console = new TextArea();
     * PrintStream printStream = new PrintStream(new UiUtils.TextAreaOutputStream(console));
     * System.setOut(printStream);
     * System.setErr(printStream);
     * </pre>
     */
    public static class TextAreaOutputStream extends OutputStream {
        private final TextArea console;
        private final StringBuilder buffer = new StringBuilder();

        /**
         * Constructs a new TextAreaOutputStream that writes to the specified TextArea.
         *
         * @param console the target TextArea that will receive the output; must not be null
         */
        public TextAreaOutputStream(TextArea console) {
            this.console = console;
        }

        /**
         * Writes a single byte to the output stream.
         *
         * <p>The byte is interpreted as a character. When a newline character ('\n') is
         * encountered, the accumulated buffer is flushed to the TextArea on the JavaFX application
         * thread.
         *
         * @param b the byte to write (converted to char)
         */
        @Override
        public void write(int b) {
            char c = (char) b;
            buffer.append(c);
            if (c == '\n') {
                flushBuffer();
            }
        }

        /**
         * Writes a portion of a byte array to the output stream.
         *
         * <p>This implementation processes each byte individually to properly handle line
         * buffering.
         *
         * @param b the byte array containing the data to write
         * @param off the start offset in the array
         * @param len the number of bytes to write
         */
        @Override
        public void write(byte[] b, int off, int len) {
            for (int i = off; i < off + len; i++) {
                write(b[i]);
            }
        }

        /**
         * Flushes the internal buffer by writing its content to the TextArea.
         *
         * <p>The actual UI update is scheduled on the JavaFX application thread. After writing, the
         * TextArea automatically scrolls to the bottom to show the most recent content.
         */
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

    /**
     * Updates line numbers displayed in a companion TextArea based on the content of the input
     * TextArea.
     *
     * <p>This method counts the number of lines in the input text area (using newline characters as
     * delimiters) and updates the line numbers text area with sequential numbers starting from 1.
     * The line numbers are automatically synchronized with the input content.
     *
     * @param input the main text area whose content determines the line count
     * @param lineNumbers the text area that will display line numbers (1, 2, 3, ...); this area
     *     should typically be styled to be read-only and right-aligned
     */
    public static void updateLineNumbers(TextArea input, TextArea lineNumbers) {
        int lines = input.getText().split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) sb.append(i).append("\n");
        lineNumbers.setText(sb.toString());
    }

    /**
     * Saves a JavaFX WritableImage snapshot to a PNG file.
     *
     * <p>This method converts a JavaFX image to an AWT BufferedImage and writes it to the specified
     * file in PNG format. This is useful for saving screenshots, rendered scenes, or node
     * snapshots.
     *
     * @param snapshot the image to save (typically captured using {@code Node.snapshot()})
     * @param outputFile the destination file; will be created or overwritten
     * @throws IOException if an I/O error occurs while writing the image file
     * @throws NullPointerException if either parameter is null
     * @throws IllegalArgumentException if the snapshot cannot be converted
     */
    public static void saveSnapshot(javafx.scene.image.WritableImage snapshot, File outputFile)
            throws IOException {
        java.awt.image.BufferedImage bufferedImage =
                javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null);
        javax.imageio.ImageIO.write(bufferedImage, "png", outputFile);
    }

    // ==================== TIMESTAMP UTILITIES ====================

    /**
     * Returns a timestamp formatted for console or log display.
     *
     * <p>The format used is HH:mm:ss (24-hour clock). Example output: "14:30:22"
     *
     * @return the current system time as a formatted string
     */
    public static String getDisplayTimestamp() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * Returns a timestamp suitable for use in filenames.
     *
     * <p>The format used is yyyyMMdd_HHmmss. Example output: "20250418_143022"
     *
     * <p>This format is sortable by name and avoids illegal filename characters on all major
     * operating systems.
     *
     * @return the current date and time as a formatted string
     */
    public static String getFileTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    /**
     * Generates a timestamped filename with a configurable placement of the timestamp.
     *
     * <p>This convenience method helps create standardized filenames for exports, logs, or other
     * generated files.
     *
     * @param baseName the base name of the file (e.g., "export", "log", "screenshot")
     * @param extension the file extension without the dot (e.g., "txt", "png", "csv")
     * @param addTimestampBeforeExtension if {@code true}, the timestamp is placed before the
     *     extension resulting in "baseName_timestamp.extension"; if {@code false}, the timestamp is
     *     placed before the base name resulting in "timestamp_baseName.extension"
     * @return the complete filename as a string
     * @throws NullPointerException if baseName or extension is null
     * @throws IllegalArgumentException if baseName or extension is empty
     * @see #getFileTimestamp()
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
