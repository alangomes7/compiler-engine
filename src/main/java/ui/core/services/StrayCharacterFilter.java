package ui.core.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Removes stray or invisible characters that can break the parser.
 *
 * <p>This utility class filters text to keep only safe, printable characters that are acceptable
 * for parsing. It removes control characters and other invisible characters that might cause
 * parsing errors or unexpected behavior.
 *
 * <p>The allowed character set includes:
 *
 * <ul>
 *   <li>Whitespace: space (32), tab (9), newline (10), carriage return (13)
 *   <li>Printable ASCII characters: from space (32) to tilde (126)
 * </ul>
 *
 * <p>All other characters are filtered out of the output.
 *
 * <p>Typical usage:
 *
 * <pre>
 * // Filter a string
 * String raw = "Hello\u0000World\u001F!";
 * String clean = StrayCharacterFilter.filter(raw); // "HelloWorld!"
 *
 * // Filter a file in place
 * StrayCharacterFilter.cleanFileInPlace(new File("input.txt"));
 *
 * // Read and filter a file
 * String content = StrayCharacterFilter.filterFile(new File("data.txt"));
 * </pre>
 */
public class StrayCharacterFilter {

    /**
     * Filters a raw input string, removing any forbidden character.
     *
     * <p>This method processes the input character by character, retaining only those that pass the
     * {@link #isAllowed(char)} check.
     *
     * <p>If the input is null, an empty string is returned (fail-safe behavior).
     *
     * @param input the original text (may be null)
     * @return cleaned text containing only allowed characters, or an empty string if input is null
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
     * <p>This method reads the entire file using UTF-8 encoding, then passes the content through
     * {@link #filter(String)} to remove stray characters.
     *
     * <p>The original file is not modified by this method.
     *
     * @param file the file to read; must not be null and must exist
     * @return filtered file content with only allowed characters
     * @throws IOException if reading fails (file not found, permission denied, etc.)
     * @throws NullPointerException if file is null
     */
    public static String filterFile(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String content = new String(bytes, StandardCharsets.UTF_8);
        return filter(content);
    }

    /**
     * Overwrites the given file with its filtered content (in-place cleaning).
     *
     * <p>This method reads the file, filters its content, and then writes the cleaned content back
     * to the same file, replacing the original.
     *
     * <p>Use with caution: this operation is destructive and cannot be undone. Consider backing up
     * important files before cleaning.
     *
     * @param file the file to clean in place; must not be null
     * @throws IOException if reading or writing fails (file not found, permission denied, disk
     *     full, etc.)
     * @throws NullPointerException if file is null
     */
    public static void cleanFileInPlace(File file) throws IOException {
        String filtered = filterFile(file);
        Files.write(file.toPath(), filtered.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Determines whether a character is allowed to remain in filtered output.
     *
     * <p>A character is allowed if it meets any of the following criteria:
     *
     * <ul>
     *   <li>It is a tab character (9)
     *   <li>It is a newline character (10)
     *   <li>It is a carriage return character (13)
     *   <li>It is an ASCII printable character (from 32 to 126 inclusive)
     * </ul>
     *
     * @param c the character to test
     * @return {@code true} if the character is allowed, {@code false} otherwise
     */
    private static boolean isAllowed(char c) {
        // Tab, newline, carriage return
        if (c == 9 || c == 10 || c == 13) return true;
        // ASCII printable characters (space to '~')
        return c >= 32 && c <= 126;
    }
}
