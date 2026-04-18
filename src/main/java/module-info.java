module compiler.engine {
    // JavaFX core modules
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.swing; // Required for SwingFXUtils in Utils class

    // External dependencies
    requires guru.nidi.graphviz; // For automata visualizer
    requires org.slf4j; // For simple logging

    // Compile-time only dependency
    requires static lombok;

    // Allow JavaFX FXML to access your UI controllers reflectively
    opens ui to
            javafx.fxml;

    // Allow JavaFX to instantiate your main application class
    opens app to
            javafx.graphics;

    // Export core packages if they need to be visible to other modules
    exports app;
    exports ui;
}
