package ui.utils;

import java.io.OutputStream;

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

            // Accumulate characters
            buffer.append(c);

            // Flush on newline
            if (c == '\n') {
                flushBuffer();
            }
        }

        private void flushBuffer() {
            String text = buffer.toString();
            buffer.setLength(0);

            Platform.runLater(() -> {
                console.appendText(text);
                console.setScrollTop(Double.MAX_VALUE);
            });
        }
    }

    /**
     * Logic for updating line numbers in a TextArea
     */
    public static void updateLineNumbers(TextArea input, TextArea lineNumbers) {
        int lines = input.getText().split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) sb.append(i).append("\n");
        lineNumbers.setText(sb.toString());
    }
}