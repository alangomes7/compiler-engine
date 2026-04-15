package graph;

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

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import models.atomic.Constants;
import models.atomic.State;
import models.atomic.Transition;
import models.automata.AFD;

public class AutomataVisualizer {

    public static String generateDotFormat(AFD afd) {
        StringBuilder dot = new StringBuilder();
        
        dot.append("digraph ").append(afd.getTokenName()).append(" {\n");
        dot.append("    rankdir=LR;\n");
        dot.append("    node [shape = circle];\n");

        Set<State> finalStates = afd.getFinalStates();
        if (finalStates != null && !finalStates.isEmpty()) {
            dot.append("    node [shape = doublecircle]; ");
            for (State s : finalStates) {
                dot.append("\"q").append(s.getId()).append("\" ");
            }
            dot.append(";\n");
        }

        dot.append("    node [shape = circle];\n");

        State startState = afd.getStartState();
        if (startState != null) {
            dot.append("    start [shape=point];\n");
            dot.append("    start -> \"q").append(startState.getId()).append("\";\n");
        }

        Set<State> visited = new HashSet<>();
        Queue<State> queue = new LinkedList<>();

        if (startState != null) {
            queue.add(startState);
            visited.add(startState);
        }

        while (!queue.isEmpty()) {
            State current = queue.poll();
            
            // 1. Group raw symbols by Target State
            Map<State, List<String>> groupedTransitions = new HashMap<>();
            
            for (Transition t : current.getTransitions()) {
                State target = t.getTarget();
                String symbol = t.getSymbol();
                
                // Handle null/empty as Epsilon before grouping
                if (symbol == null || symbol.isEmpty()) {
                    symbol = Constants.EPSILON;
                }
                
                groupedTransitions.computeIfAbsent(target, k -> new ArrayList<>()).add(symbol);
                
                if (!visited.contains(target)) {
                    visited.add(target);
                    queue.add(target);
                }
            }
            
            // 2. Compress the grouped symbols into Regex ranges and write the edge
            for (Map.Entry<State, List<String>> entry : groupedTransitions.entrySet()) {
                State target = entry.getKey();
                List<String> rawSymbols = entry.getValue();
                
                // Summarize "a","b","c","d" into "[a-d]"
                String summarizedRegex = summarizeToRegex(rawSymbols);
                
                // Safely convert to a Graphviz HTML label (re-using our Sanitizer from the previous step)
                String safeHtmlLabel = GraphvizSanitizer.sanitizeLabel(summarizedRegex);
                
                dot.append("    \"q").append(current.getId()).append("\" -> \"q")
                   .append(target.getId()).append("\" [label=").append(safeHtmlLabel).append("];\n");
            }
        }

        dot.append("}\n");
        return dot.toString();
    }

    /**
     * Takes a list of raw transition symbols and compresses them into a human-readable 
     * regular expression class (e.g., replacing individual letters with [a-zA-Z]).
     */
    private static String summarizeToRegex(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) return "";
        
        Set<Character> charSet = new HashSet<>();
        List<String> specialSymbols = new ArrayList<>();
        
        // Separate single characters from multi-character constants/tokens
        for (String s : symbols) {
            if (s.length() == 1) {
                charSet.add(s.charAt(0));
            } else {
                specialSymbols.add(s);
            }
        }

        // If there are no compressible characters, just return the special tokens
        if (charSet.isEmpty()) {
            return String.join(", ", specialSymbols);
        }

        // If there's only exactly ONE character, don't wrap it in brackets
        if (charSet.size() == 1 && specialSymbols.isEmpty()) {
            return String.valueOf(charSet.iterator().next());
        }

        // Sort characters to find contiguous ASCII sequences
        List<Character> sorted = new ArrayList<>(charSet);
        Collections.sort(sorted);

        StringBuilder regex = new StringBuilder("[");
        int i = 0;
        
        while (i < sorted.size()) {
            char start = sorted.get(i);
            char end = start;

            // Find contiguous range
            while (i + 1 < sorted.size() && sorted.get(i + 1) == end + 1) {
                end = sorted.get(i + 1);
                i++;
            }

            // If range is 3 or more (e.g., a,b,c), compress to a-c
            if (end - start >= 2) {
                regex.append(formatRegexChar(start)).append("-").append(formatRegexChar(end));
            } 
            // If range is 2 (e.g., a,b), just write ab
            else if (end - start == 1) {
                regex.append(formatRegexChar(start)).append(formatRegexChar(end));
            } 
            // Isolated character
            else {
                regex.append(formatRegexChar(start));
            }
            i++;
        }
        regex.append("]");

        // Append any special string tokens if they existed
        if (!specialSymbols.isEmpty()) {
            return regex.toString() + ", " + String.join(", ", specialSymbols);
        }

        return regex.toString();
    }

    /**
     * Helper to escape visual characters purely for the Regex string representation
     * (HTML tag escaping is handled independently by GraphvizSanitizer)
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
                if (c < 32 || c == 127) {
                    return String.format("\\x%02X", (int) c); // Hex representation for invisibles
                }
                return String.valueOf(c);
            }
        }
    }

    public static void exportToImage(AFD afd, String outputFilename) {
        try {
            String dotFormat = generateDotFormat(afd);
            MutableGraph g = new Parser().read(dotFormat);
            
            File outputDir = new File("output");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            File outputFile = new File(outputDir, outputFilename);
            Graphviz.fromGraph(g).width(2400).render(Format.PNG).toFile(outputFile);
            System.out.println("\n[Graphviz] AFD successfully exported to " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("\n[Graphviz Error] Failed to export image: " + e.getMessage());
        }
    }
}