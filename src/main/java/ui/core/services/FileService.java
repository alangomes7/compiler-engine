package ui.core.services;

import java.io.File;

import Utils.UiUtils;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class FileService {

    public static File selectFile(Window owner, String title, String... extensions) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        if (extensions.length > 0) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Supported Files", extensions)
            );
        }
        return fileChooser.showOpenDialog(owner);
    }

    public static String readFileContent(File file) throws Exception {
        return UiUtils.readTextFile(file.getAbsolutePath());
    }
}