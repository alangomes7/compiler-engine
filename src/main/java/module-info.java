module compiler.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires guru.nidi.graphviz;
    requires org.slf4j;

    opens models.others to javafx.base;
    opens models.atomic to javafx.base;
    
    opens app to javafx.fxml;
    exports app;
}