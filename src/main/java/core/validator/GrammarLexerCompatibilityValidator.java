package core.validator;

import core.lexer.models.atomic.Rule;
import core.parser.models.Grammar;
import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates whether the terminals expected by the Parser's Grammar have corresponding Lexer rules
 * defined.
 */
public class GrammarLexerCompatibilityValidator {

    /**
     * Inspects the provided grammar and lexer rules to identify potential discrepancies. * @param
     * grammar The context-free grammar loaded into the parser.
     *
     * @param lexerRules The list of rules read from the lexer file.
     * @return A formatted report string highlighting compatibility status.
     */
    public static String validate(Grammar grammar, List<Rule> lexerRules) {
        if (grammar == null || lexerRules == null || lexerRules.isEmpty()) {
            return "Cannot validate compatibility: Grammar or Lexer Rules are missing.\n";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== Grammar & Lexer Compatibility Report ===\n");

        Set<String> tokenTypes =
                lexerRules.stream().map(Rule::getTokenType).collect(Collectors.toSet());

        List<String> warnings = new ArrayList<>();

        for (Symbol terminal : grammar.getTerminals()) {
            String tName = terminal.getName();

            // Skip built-in and internal epsilon/EOF markers
            if (tName.equals("ε")
                    || tName.equals("$")
                    || tName.equals("EPSILON")
                    || tName.equals("EOF")) {
                continue;
            }

            // Check LL1Parser normalizations
            if (tName.equals("#") || tName.equals("comment")) {
                if (!tokenTypes.contains("comment") && !tokenTypes.contains("#")) {
                    warnings.add(
                            "Terminal '"
                                    + tName
                                    + "' lacks a corresponding Lexer rule (e.g., 'comment').");
                }
                continue;
            }

            if (tName.equals("number")) {
                boolean found =
                        tokenTypes.stream().anyMatch(t -> t.endsWith("_NUM") || t.equals("number"));
                if (!found) {
                    warnings.add(
                            "Terminal 'number' expects a Lexer rule ending with '_NUM' (e.g., INT_NUM) or exactly 'number'.");
                }
                continue;
            }

            if (tName.equals("identifier")) {
                boolean found =
                        tokenTypes.contains("LOWER")
                                || tokenTypes.contains("UPPER")
                                || tokenTypes.contains("identifier");
                if (!found) {
                    warnings.add(
                            "Terminal 'identifier' expects a Lexer rule named 'LOWER', 'UPPER', or 'identifier'.");
                }
                continue;
            }

            // Standard match check: Verify if it's explicitly a token type, or potentially matched
            // by regex
            if (!tokenTypes.contains(tName)) {
                // Heuristic: Try to see if any rule regex could literally match this terminal
                boolean possibleLiteralMatch = false;
                for (Rule r : lexerRules) {
                    String cleanRegex = r.getRegex().replace("\\", "");
                    if (cleanRegex.equals(tName) || cleanRegex.contains(tName)) {
                        possibleLiteralMatch = true;
                        break;
                    }
                }

                if (!possibleLiteralMatch) {
                    warnings.add(
                            "Terminal '"
                                    + tName
                                    + "' has no matching token type or explicit literal in Lexer rules.");
                }
            }
        }

        if (warnings.isEmpty()) {
            report.append(
                    "✅ Fully Compatible! All grammar terminals appear to have matching lexer rules.\n");
        } else {
            report.append("⚠️ Potential Incompatibilities Found:\n");
            for (String w : warnings) {
                report.append("  - ").append(w).append("\n");
            }
            report.append(
                            "\n*Note: The parser matches terminals either by Token Type or by exact Lexeme.\n")
                    .append(
                            "If a terminal is covered by a general regex rule (e.g., '+' covered by 'OPERATOR'),\n")
                    .append("you can safely disregard its warning.*");
        }

        return report.toString();
    }
}
