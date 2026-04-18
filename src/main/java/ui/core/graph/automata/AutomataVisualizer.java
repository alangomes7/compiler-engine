package ui.core.graph.automata;

import core.lexer.models.atomic.State;
import core.lexer.models.atomic.Transition;
import core.lexer.models.automata.AFD;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class AutomataVisualizer {

    /**
     * Generates a coloured, high‑quality DOT representation of the AFD. Nodes are coloured like the
     * JavaFX interactive view: - Start state: light green fill - Final states: double circle, red
     * border - Normal states: light blue fill
     */
    public static String generateDotFormat(AFD afd) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph ").append(afd.getTokenName()).append(" {\n");
        dot.append("    rankdir=LR;\n");
        dot.append("    node [shape=circle];\n");

        // Collect all reachable states via BFS
        Set<State> allStates = new HashSet<>();
        Queue<State> queue = new LinkedList<>();
        State startState = afd.getStartState();
        if (startState != null) {
            queue.add(startState);
            allStates.add(startState);
        }
        while (!queue.isEmpty()) {
            State curr = queue.poll();
            for (Transition t : curr.getTransitions()) {
                State target = t.getTarget();
                if (!allStates.contains(target)) {
                    allStates.add(target);
                    queue.add(target);
                }
            }
        }

        Set<State> finalStates = afd.getFinalStates();
        // Emit each node with its specific attributes
        for (State s : allStates) {
            boolean isStart = s.equals(startState);
            boolean isFinal = finalStates != null && finalStates.contains(s);
            String nodeId = "\"q" + s.getId() + "\"";

            if (isStart && isFinal) {
                // Start + Final: green fill, double circle, red border
                dot.append("    ")
                        .append(nodeId)
                        .append(
                                " [shape=doublecircle, fillcolor=lightgreen, style=filled, color=red, penwidth=2];\n");
            } else if (isStart) {
                // Only start: green fill, single circle
                dot.append("    ")
                        .append(nodeId)
                        .append(" [shape=circle, fillcolor=lightgreen, style=filled];\n");
            } else if (isFinal) {
                // Only final: double circle, red border, light blue fill
                dot.append("    ")
                        .append(nodeId)
                        .append(
                                " [shape=doublecircle, fillcolor=lightblue, style=filled, color=red, penwidth=2];\n");
            } else {
                // Normal state: light blue fill
                dot.append("    ")
                        .append(nodeId)
                        .append(" [shape=circle, fillcolor=lightblue, style=filled];\n");
            }
        }

        // Start arrow (invisible start node)
        if (startState != null) {
            dot.append("    start [shape=point];\n");
            dot.append("    start -> \"q").append(startState.getId()).append("\";\n");
        }

        // Edges with compressed labels
        Set<State> visited = new HashSet<>();
        queue.clear();
        if (startState != null) {
            queue.add(startState);
            visited.add(startState);
        }

        while (!queue.isEmpty()) {
            State current = queue.poll();

            // Group transitions by target state
            Map<State, List<String>> grouped = new HashMap<>();
            for (Transition t : current.getTransitions()) {
                State target = t.getTarget();
                String sym = t.getSymbol().getValue();
                if (sym == null || sym.isEmpty()) sym = "ε";
                grouped.computeIfAbsent(target, k -> new ArrayList<>()).add(sym);

                if (!visited.contains(target)) {
                    visited.add(target);
                    queue.add(target);
                }
            }

            // Emit each edge with compressed label
            for (Map.Entry<State, List<String>> entry : grouped.entrySet()) {
                State target = entry.getKey();
                String rawLabel = summarizeToRegex(entry.getValue());
                String safeLabel =
                        GraphvizSanitizer.sanitizeLabel(rawLabel); // produces <...> HTML label

                dot.append("    \"q")
                        .append(current.getId())
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
     * Compresses a list of symbols into a compact regex‑like representation. (unchanged from
     * original)
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

    private static String formatRegexChar(char c) {
        switch (c) {
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";
            case ' ':
                return "\\s";
            case ']':
                return "\\]";
            case '-':
                return "\\-";
            case '^':
                return "\\^";
            case '\\':
                return "\\\\";
            default:
                if (c < 32 || c == 127) return String.format("\\x%02X", (int) c);
                return String.valueOf(c);
        }
    }

    /**
     * Exports the AFD to a PNG image with high resolution (full width up to 3840 px) and coloured
     * nodes matching the interactive view.
     */
    public static void exportToImage(AFD afd, String outputFilename) {
        try {
            String dotFormat = generateDotFormat(afd);
            MutableGraph g = new Parser().read(dotFormat);

            File outputDir = new File("output");
            if (!outputDir.exists()) outputDir.mkdirs();

            File outputFile = new File(outputDir, outputFilename);
            // High resolution: set width to 3840 pixels (or adjust as needed)
            Graphviz.fromGraph(g).width(3840).render(Format.PNG).toFile(outputFile);

            System.out.println(
                    "\n[Graphviz] Coloured AFD exported to " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("\n[Graphviz Error] Failed to export image: " + e.getMessage());
        }
    }
}
