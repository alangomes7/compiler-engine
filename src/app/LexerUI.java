package app;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import service.LexerService;

public class LexerUI {

    private VBox root;
    private LexerService lexerService;

    public LexerUI() {
        lexerService = new LexerService();

        TextArea inputArea = new TextArea();
        inputArea.setPromptText("Paste source code here...");

        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);

        Button runButton = new Button("Run Lexer");

        runButton.setOnAction(e -> {
            String input = inputArea.getText();
            String result = lexerService.analyze(input);
            outputArea.setText(result);
        });

        root = new VBox(10,
                new Label("Input"),
                inputArea,
                runButton,
                new Label("Output"),
                outputArea
        );
    }

    public VBox getRoot() {
        return root;
    }
}