package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        LexerUI ui = new LexerUI();

        Scene scene = new Scene(ui.getRoot(), 900, 600);

        stage.setTitle("Lexical Analyzer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}