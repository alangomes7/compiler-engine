module compiler.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    
    requires javafx.swing;
    requires java.desktop; 

    requires guru.nidi.graphviz;
    requires org.slf4j;
    requires org.slf4j.simple;
    
    // Use 'requires static' for compile-time only dependencies
    requires static lombok;

    // Open packages containing TableView models to javafx.base
    opens core.lexer.models.atomic to javafx.base;
    opens core.parser.models.atomic to javafx.base;

    // Merge duplicate opens
    opens ui to javafx.base, javafx.fxml;

    // Maintain standard app exports
    opens app to javafx.fxml;
    exports app;
}