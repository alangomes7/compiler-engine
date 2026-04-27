package ui.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import static ui.util.UiUtils.getDisplayTimestamp;

/**
 * Executes long-running tasks in a background thread while showing a loading overlay and updating a
 * live timer. Logs progress messages to a console area.
 *
 * <p><b>Cancel Operation Support:</b> This executor supports canceling long-running operations.
 * When the cancel button is clicked, the background thread is interrupted and the operation is
 * terminated. The loading overlay is hidden and the UI is restored.
 */
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

/**
 * Constructs a task executor with references to UI components.
 *
 * @param loadingOverlay the overlay container to show/hide during task execution
 * @param loadingLabel label that displays the current task message
 * @param loadingTimeLabel label that displays the elapsed processing time
 * @param consoleArea text area where log messages will be appended
 * @param cancelButton button that allows canceling the current operation
 */
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

    /**
     * Executes a heavy task asynchronously.
     *
     * @param initialMessage message shown on the loading overlay when the task starts
     * @param task function that receives a log consumer and returns a result
     * @param onSuccess called on the JavaFX thread with the result
     * @param onError called on the JavaFX thread with any exception thrown by the task
     * @param <T> the result type
     */
    public <T> void execute(
            String initialMessage,
            Function<Consumer<String>, T> task,
            Consumer<T> onSuccess,
            Consumer<Exception> onError) {

        // Cancel any existing operation before starting a new one
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

    /**
     * Cancels the currently running task.
     *
     * <p>This method interrupts the background thread and hides the loading overlay. The operation
     * cannot be resumed after cancellation.
     */
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

    /** Finishes the current operation by hiding the loading overlay and resetting UI. */
    private void finishOperation() {
        isRunning = false;
        loadingOverlay.setVisible(false);
        cancelButton.setDisable(true);
        cancelButton.setVisible(false);
        currentThread = null;
        currentTimer = null;
    }

    /**
     * Checks if a task is currently running.
     *
     * @return true if a task is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Appends a message to the console area and optionally updates the loading label. Must be
     * called from any thread; the actual UI update is scheduled on the JavaFX thread.
     *
     * @param message the log message to display
     */
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
