package ui.core.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Removes stray or invisible characters that can break the parser.
 * Keeps only ASCII printable characters plus newline, tab, carriage return.
 *
 * @author Generated
 * @version 1.0
 */
public class StrayCharacterFilter {

    /**
     * Filters a raw input string, removing any forbidden character.
     *
     * @param input the original text (may be null)
     * @return cleaned text containing only allowed characters
     */
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

    /**
     * Reads a file, filters its content, and returns the cleaned string.
     *
     * @param file the file to read
     * @return filtered file content
     * @throws IOException if reading fails
     */
    public static String filterFile(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String content = new String(bytes, StandardCharsets.UTF_8);
        return filter(content);
    }

    /**
     * Overwrites the given file with its filtered content (in‑place cleaning).
     *
     * @param file the file to clean
     * @throws IOException if reading or writing fails
     */
    public static void cleanFileInPlace(File file) throws IOException {
        String filtered = filterFile(file);
        Files.write(file.toPath(), filtered.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isAllowed(char c) {
        // Tab, newline, carriage return
        if (c == 9 || c == 10 || c == 13) return true;
        // ASCII printable characters (space to '~')
        return c >= 32 && c <= 126;
    }
}