module compiler.engine {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.swing;
    requires guru.nidi.graphviz;
    requires org.slf4j;
    requires static lombok;

    opens ui to
            javafx.fxml;
    opens app to
            javafx.graphics;

    exports app;
    exports ui;
    exports ui.util;
}
