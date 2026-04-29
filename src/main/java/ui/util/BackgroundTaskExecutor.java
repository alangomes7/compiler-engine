package ui.util;

import static ui.util.UiUtils.getDisplayTimestamp;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class BackgroundTaskExecutor {

    private final VBox loadingOverlay;
    private final Label loadingLabel;
    private final Label loadingTimeLabel;
    private final TextArea consoleArea;
    private final Button cancelButton;

    private Thread currentThread;
    private AnimationTimer currentTimer;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private volatile boolean isRunning = false;

    public BackgroundTaskExecutor(
            VBox loadingOverlay,
            Label loadingLabel,
            Label loadingTimeLabel,
            TextArea consoleArea,
            Button cancelButton) {
        this.loadingOverlay = loadingOverlay;
        this.loadingLabel = loadingLabel;
        this.loadingTimeLabel = loadingTimeLabel;
        this.consoleArea = consoleArea;
        this.cancelButton = cancelButton;
    }

    public <T> void execute(
            String initialMessage,
            Function<Consumer<String>, T> task,
            Consumer<T> onSuccess,
            Consumer<Exception> onError) {

        cancelCurrentTask();

        isCancelled.set(false);
        isRunning = true;
        final long startTime = System.currentTimeMillis();

        currentTimer =
                new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        if (!isRunning) {
                            this.stop();
                            return;
                        }
                        long elapsed = System.currentTimeMillis() - startTime;
                        long hours = elapsed / (1000 * 60 * 60);
                        long minutes = (elapsed / (1000 * 60)) % 60;
                        long seconds = (elapsed / 1000) % 60;
                        long millis = elapsed % 1000;
                        Platform.runLater(
                                () -> {
                                    if (isRunning) {
                                        loadingTimeLabel.setText(
                                                String.format(
                                                        "processing... %02d:%02d:%02d.%03d",
                                                        hours, minutes, seconds, millis));
                                    }
                                });
                    }
                };

        Platform.runLater(
                () -> {
                    loadingLabel.setText(initialMessage);
                    loadingOverlay.setVisible(true);
                    loadingTimeLabel.setText("processing... 00:00:00.000");
                    cancelButton.setDisable(false);
                    cancelButton.setVisible(true);
                    currentTimer.start();
                });

        currentThread =
                new Thread(
                        () -> {
                            try {
                                T result = task.apply(this::appendLog);

                                if (!isCancelled.get()) {
                                    Platform.runLater(
                                            () -> {
                                                currentTimer.stop();
                                                onSuccess.accept(result);
                                                finishOperation();
                                            });
                                } else {
                                    Platform.runLater(() -> finishOperation());
                                }
                            } catch (Exception e) {
                                if (!isCancelled.get()) {
                                    Platform.runLater(
                                            () -> {
                                                currentTimer.stop();
                                                onError.accept(e);
                                                finishOperation();
                                            });
                                } else {
                                    Platform.runLater(() -> finishOperation());
                                }
                            }
                        });
        currentThread.setDaemon(true);
        currentThread.start();
    }

    public void cancelCurrentTask() {
        if (isRunning) {
            isCancelled.set(true);
            isRunning = false;

            if (currentThread != null) {
                currentThread.interrupt();
                currentThread = null;
            }

            if (currentTimer != null) {
                Platform.runLater(
                        () -> {
                            currentTimer.stop();
                            currentTimer = null;
                        });
            }

            Platform.runLater(this::finishOperation);
            appendLog("⚠️ Operation cancelled by user.");
        }
    }

    private void finishOperation() {
        isRunning = false;
        loadingOverlay.setVisible(false);
        cancelButton.setDisable(true);
        cancelButton.setVisible(false);
        currentThread = null;
        currentTimer = null;
    }

    public boolean isRunning() {
        return isRunning;
    }

    private void appendLog(String message) {
        Platform.runLater(
                () -> {
                    if (!isCancelled.get()) {
                        String time = getDisplayTimestamp();
                        consoleArea.appendText("[" + time + "] " + message + "\n");
                        consoleArea.setScrollTop(Double.MAX_VALUE);
                        if (loadingOverlay.isVisible()) {
                            loadingLabel.setText(message);
                        }
                    }
                });
    }
}
