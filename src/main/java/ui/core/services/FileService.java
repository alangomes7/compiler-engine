package ui.core.services;

import Utils.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class FileService {

    public static File selectFile(Window owner, String title, String... extensions) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        if (extensions.length > 0) {
            fileChooser
                    .getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Supported Files", extensions));
        }
        return fileChooser.showOpenDialog(owner);
    }

    public static String readFileContent(File file) throws Exception {
        String fileContentRaw = Utils.readTextFile(file.getAbsolutePath());
        String clean = StrayCharacterFilter.filter(fileContentRaw);
        return clean;
    }

    public static String escapeCsv(String data) {
        if (data == null) return "";
        return data.replace("\"", "\"\"");
    }

    public static File saveStringToFile(
            Window owner, String content, String initialFileName, String... extensions)
            throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        if (initialFileName != null) {
            fileChooser.setInitialFileName(initialFileName);
        }
        if (extensions.length > 0) {
            fileChooser
                    .getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Text Files", extensions));
        }
        File file = fileChooser.showSaveDialog(owner);
        if (file != null) {
            writeStringToFile(file, content);
        }
        return file;
    }

    public static void writeStringToFile(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
}
