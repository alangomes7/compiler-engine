module compiler.app {
    // These require the JavaFX modules you are using
    requires javafx.controls;
    requires javafx.fxml;

    // This allows JavaFX to use reflection to load your FXML file
    opens app to javafx.fxml;
    
    // This exports your app package so it can be run
    exports app;
}