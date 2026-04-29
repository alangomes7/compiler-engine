package ui.core.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class StrayCharacterFilter {

    public static String filter(String input) {
        if (input == null) return "";
        StringBuilder cleaned = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (isAllowed(c)) {
                cleaned.append(c);
            }
        }
        return cleaned.toString();
    }

    public static String filterFile(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String content = new String(bytes, StandardCharsets.UTF_8);
        return filter(content);
    }

    public static void cleanFileInPlace(File file) throws IOException {
        String filtered = filterFile(file);
        Files.write(file.toPath(), filtered.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isAllowed(char c) {
        if (c == 9 || c == 10 || c == 13) return true;
        return c >= 32 && c <= 126;
    }
}
