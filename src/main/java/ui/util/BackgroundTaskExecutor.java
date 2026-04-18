package ui.util;

import static ui.util.UiUtils.getDisplayTimestamp;

import java.util.function.Consumer;
import java.util.function.Function;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Executes long-running tasks in a background thread while showing a loading overlay and updating a
 * live timer.
 */
public class BackgroundTaskExecutor {

    private final VBox loadingOverlay;
    private final Label loadingLabel;
    private final Label loadingTimeLabel;
    private final TextArea consoleArea;

    public BackgroundTaskExecutor(
            VBox loadingOverlay, Label loadingLabel, Label loadingTimeLabel, TextArea consoleArea) {
        this.loadingOverlay = loadingOverlay;
        this.loadingLabel = loadingLabel;
        this.loadingTimeLabel = loadingTimeLabel;
        this.consoleArea = consoleArea;
    }

    /**
     * Executes a heavy task asynchronously.
     *
     * @param initialMessage message shown on the loading overlay
     * @param task function that receives a log consumer and returns a result
     * @param onSuccess called on FX thread with the result
     * @param onError called on FX thread with any exception
     * @param <T> result type
     */
    public <T> void execute(
            String initialMessage,
            Function<Consumer<String>, T> task,
            Consumer<T> onSuccess,
            Consumer<Exception> onError) {
        final long startTime = System.currentTimeMillis();

        AnimationTimer timer =
                new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        long hours = elapsed / (1000 * 60 * 60);
                        long minutes = (elapsed / (1000 * 60)) % 60;
                        long seconds = (elapsed / 1000) % 60;
                        long millis = elapsed % 1000;
                        Platform.runLater(
                                () ->
                                        loadingTimeLabel.setText(
                                                String.format(
                                                        "processing... %02d:%02d:%02d.%03d",
                                                        hours, minutes, seconds, millis)));
                    }
                };

        Platform.runLater(
                () -> {
                    loadingLabel.setText(initialMessage);
                    loadingOverlay.setVisible(true);
                    timer.start();
                });

        Thread thread =
                new Thread(
                        () -> {
                            try {
                                T result = task.apply(this::appendLog);
                                Platform.runLater(
                                        () -> {
                                            timer.stop();
                                            onSuccess.accept(result);
                                            loadingOverlay.setVisible(false);
                                        });
                            } catch (Exception e) {
                                Platform.runLater(
                                        () -> {
                                            timer.stop();
                                            onError.accept(e);
                                            loadingOverlay.setVisible(false);
                                        });
                            }
                        });
        thread.setDaemon(true);
        thread.start();
    }

    private void appendLog(String message) {
        Platform.runLater(
                () -> {
                    String time = getDisplayTimestamp();
                    consoleArea.appendText("[" + time + "] " + message + "\n");
                    consoleArea.setScrollTop(Double.MAX_VALUE);
                    if (loadingOverlay.isVisible()) {
                        loadingLabel.setText(message);
                    }
                });
    }
}
