package ui.core.graph.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.DFA;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for generating Graphviz DOT representations and exporting images of deterministic
 * finite automata (DFA).
 *
 * <p>This class provides functionality to visualize DFAs as high-quality, colored diagrams suitable
 * for documentation or presentation. Key features include:
 *
 * <ul>
 *   <li>Grouped transitions with compact regex-like labels
 *   <li>Visual distinction for start states (light green) and final states (light blue/red outline)
 *   <li>Support for large-scale exports up to 4K resolution (3840px width)
 *   <li>Character range collapsing (e.g., "a", "b", "c" becomes "[a-c]")
 * </ul>
 *
 * <p>Typical usage:
 *
 * <pre>
 * // Generate DOT format string
 * String dot = AutomataVisualizer.generateDotFormat(dfa);
 * System.out.println(dot);
 *
 * // Export to PNG image
 * AutomataVisualizer.exportToImage(dfa, "my_automaton.png");
 * </pre>
 *
 * @see DFA
 * @see GraphvizSanitizer
 */
public class AutomataVisualizer {

    /**
     * Generates a coloured, high‑quality DOT representation of the DFA.
     *
     * <p>The DOT format is a plain text graph description language understood by Graphviz tools.
     * The generated representation includes:
     *
     * <ul>
     *   <li>Node shapes: circles with color coding (light green for start, light blue for others)
     *   <li>Double circles for final states with red borders
     *   <li>Invisible start nodes with arrows pointing to initial states
     *   <li>Grouped transitions that merge multiple symbols between the same source and target
     * </ul>
     *
     * @param dfa the deterministic finite automaton to visualise; must not be null
     * @return a string in Graphviz DOT format describing the automaton
     * @throws NullPointerException if dfa is null
     */
    public static String generateDotFormat(DFA dfa) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph ").append(dfa.getTokenName()).append(" {\n");
        dot.append("    rankdir=LR;\n");
        dot.append("    node [shape=circle];\n");

        Set<State> allStates = dfa.getStates();
        Set<State> initialStates = dfa.getInitialStates();
        Set<State> finalStates = dfa.getFinalStates();

        // Emit each node with its specific attributes
        for (State s : allStates) {
            boolean isStart = initialStates.contains(s);
            boolean isFinal = finalStates.contains(s);
            String nodeId = "\"q" + s.getId() + "\"";

            if (isStart && isFinal) {
                dot.append("    ")
                        .append(nodeId)
                        .append(
                                " [shape=doublecircle, fillcolor=lightgreen, style=filled, color=red, penwidth=2];\n");
            } else if (isStart) {
                dot.append("    ")
                        .append(nodeId)
                        .append(" [shape=circle, fillcolor=lightgreen, style=filled];\n");
            } else if (isFinal) {
                dot.append("    ")
                        .append(nodeId)
                        .append(
                                " [shape=doublecircle, fillcolor=lightblue, style=filled, color=red, penwidth=2];\n");
            } else {
                dot.append("    ")
                        .append(nodeId)
                        .append(" [shape=circle, fillcolor=lightblue, style=filled];\n");
            }
        }

        // Start arrow (invisible start nodes for all initial states)
        for (State startState : initialStates) {
            dot.append("    start_").append(startState.getId()).append(" [shape=point];\n");
            dot.append("    start_")
                    .append(startState.getId())
                    .append(" -> \"q")
                    .append(startState.getId())
                    .append("\";\n");
        }

        // Group transitions by Source AND Target
        Map<State, Map<State, List<String>>> groupedTransitions = new HashMap<>();

        for (Transition t : dfa.getTransitions()) {
            String sym = t.getSymbol().getValue();
            if (sym == null || sym.isEmpty()) sym = "ε";

            groupedTransitions
                    .computeIfAbsent(t.getSource(), k -> new HashMap<>())
                    .computeIfAbsent(t.getTarget(), k -> new ArrayList<>())
                    .add(sym);
        }

        // Emit edges
        for (Map.Entry<State, Map<State, List<String>>> sourceEntry :
                groupedTransitions.entrySet()) {
            State source = sourceEntry.getKey();

            for (Map.Entry<State, List<String>> targetEntry : sourceEntry.getValue().entrySet()) {
                State target = targetEntry.getKey();
                String rawLabel = summarizeToRegex(targetEntry.getValue());
                String safeLabel = GraphvizSanitizer.sanitizeLabel(rawLabel);

                dot.append("    \"q")
                        .append(source.getId())
                        .append("\" -> \"q")
                        .append(target.getId())
                        .append("\" [label=")
                        .append(safeLabel)
                        .append("];\n");
            }
        }

        dot.append("}\n");
        return dot.toString();
    }

    /**
     * Merges a list of symbol strings into a compact regex‑like representation.
     *
     * <p>This method performs intelligent grouping of characters:
     *
     * <ul>
     *   <li>Single characters are grouped into character classes
     *   <li>Consecutive ranges are collapsed (e.g., "a", "b", "c" → "[a-c]")
     *   <li>Multi-character symbols are preserved as separate entries
     * </ul>
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>["a", "b", "c"] → "[a-c]"
     *   <li>["a", "c", "d", "if"] → "[a, c-d], if"
     *   <li>["+", "-", "*"] → "[+\\-*]"? (escaped as needed)
     * </ul>
     *
     * @param symbols the list of symbols to merge; may be null or empty
     * @return a concise string representation, or an empty string if the list is empty
     */
    private static String summarizeToRegex(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) return "";
        Set<Character> charSet = new HashSet<>();
        List<String> specialSymbols = new ArrayList<>();
        for (String s : symbols) {
            if (s.length() == 1) charSet.add(s.charAt(0));
            else specialSymbols.add(s);
        }
        if (charSet.isEmpty()) {
            return String.join(", ", specialSymbols);
        }
        if (charSet.size() == 1 && specialSymbols.isEmpty()) {
            return String.valueOf(charSet.iterator().next());
        }
        List<Character> sorted = new ArrayList<>(charSet);
        Collections.sort(sorted);
        StringBuilder regex = new StringBuilder("[");
        int i = 0;
        while (i < sorted.size()) {
            char start = sorted.get(i);
            char end = start;
            while (i + 1 < sorted.size() && sorted.get(i + 1) == end + 1) {
                end = sorted.get(i + 1);
                i++;
            }
            if (end - start >= 2) {
                regex.append(formatRegexChar(start)).append("-").append(formatRegexChar(end));
            } else if (end - start == 1) {
                regex.append(formatRegexChar(start)).append(formatRegexChar(end));
            } else {
                regex.append(formatRegexChar(start));
            }
            i++;
        }
        regex.append("]");
        if (!specialSymbols.isEmpty()) {
            return regex.toString() + ", " + String.join(", ", specialSymbols);
        }
        return regex.toString();
    }

    /**
     * Formats a single character for safe inclusion inside a regex character class.
     *
     * <p>This method escapes special characters that have meaning inside regex character classes:
     * newline, carriage return, tab, space, ']', '-', '^', and backslash. Non-printable characters
     * are represented as hexadecimal escapes.
     *
     * @param c the character to format
     * @return the escaped string representation safe for inclusion in "[...]"
     */
    private static String formatRegexChar(char c) {
        switch (c) {
            case '\n' -> {
                return "\\n";
            }
            case '\r' -> {
                return "\\r";
            }
            case '\t' -> {
                return "\\t";
            }
            case ' ' -> {
                return "\\s";
            }
            case ']' -> {
                return "\\]";
            }
            case '-' -> {
                return "\\-";
            }
            case '^' -> {
                return "\\^";
            }
            case '\\' -> {
                return "\\\\";
            }
            default -> {
                if (c < 32 || c == 127) return String.format("\\x%02X", (int) c);
                return String.valueOf(c);
            }
        }
    }

    /**
     * Exports the DFA to a PNG image file.
     *
     * <p>The image is saved in the "output" directory with the specified filename. The output
     * directory is created automatically if it does not exist. The image is rendered at 3840px
     * width (4K) for high‑quality output.
     *
     * <p>This method uses the Graphviz library to render the DOT representation and produces a
     * publication-quality diagram.
     *
     * @param dfa the automaton to visualise; must not be null
     * @param outputFilename the filename (e.g., "minimized_dfa.png"); should include the .png
     *     extension
     * @throws IOException if the output directory cannot be created or the image cannot be written
     *     (errors are caught and printed to stderr)
     */
    public static void exportToImage(DFA dfa, String outputFilename) {
        try {
            String dotFormat = generateDotFormat(dfa);
            MutableGraph g = new Parser().read(dotFormat);

            File outputDir = new File("output");
            if (!outputDir.exists()) outputDir.mkdirs();

            File outputFile = new File(outputDir, outputFilename);
            Graphviz.fromGraph(g).width(3840).render(Format.PNG).toFile(outputFile);

            System.out.println(
                    "\n[Graphviz] Coloured DFA exported to " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("\n[Graphviz Error] Failed to export image: " + e.getMessage());
        }
    }
}
