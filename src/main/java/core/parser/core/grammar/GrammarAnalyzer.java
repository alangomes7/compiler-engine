package core.parser.core.grammar;

import core.parser.models.Grammar;
import core.parser.models.Production;
import core.parser.models.atomic.GrammarErrors;
import core.parser.models.atomic.Symbol;
import java.util.ArrayList;
import java.util.List;

public class GrammarAnalyzer {

    private GrammarAnalyzer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /** Analyzes the grammar and returns a structured GrammarErrors object. */
    public static GrammarErrors analyzeGrammar(Grammar grammar) {
        return new GrammarErrors(getLeftRecursionDetails(grammar), getCommonPrefixDetails(grammar));
    }

    public static List<String> getLeftRecursionDetails(Grammar grammar) {
        List<String> details = new ArrayList<>();
        for (Production p : grammar.getProductions()) {
            if (!p.getRhs().isEmpty()) {
                Symbol firstSymbol = p.getRhs().get(0);
                if (p.getLhs().equals(firstSymbol)) {
                    details.add(
                            String.format(
                                    "Direct Left-Recursion on Non-Terminal '%s': [%s]",
                                    p.getLhs().getName(), p.toString()));
                }
            }
        }
        return details;
    }

    public static List<String> getCommonPrefixDetails(Grammar grammar) {
        List<String> details = new ArrayList<>();
        for (Symbol nonTerminal : grammar.getNonTerminals()) {
            if (nonTerminal.getName().isEmpty()) continue;
            List<Production> productions = grammar.getProductionsFor(nonTerminal);

            for (int i = 0; i < productions.size(); i++) {
                for (int j = i + 1; j < productions.size(); j++) {
                    List<Symbol> rhs1 = productions.get(i).getRhs();
                    List<Symbol> rhs2 = productions.get(j).getRhs();

                    if (!rhs1.isEmpty() && !rhs2.isEmpty() && !rhs1.get(0).equals(Symbol.EPSILON)) {
                        if (rhs1.get(0).equals(rhs2.get(0))) {
                            details.add(
                                    String.format(
                                            "Common Prefix '%s' on Non-Terminal '%s' between: [%s] and [%s]",
                                            rhs1.get(0).getName(),
                                            nonTerminal.getName(),
                                            productions.get(i).toString(),
                                            productions.get(j).toString()));
                        }
                    }
                }
            }
        }
        return details;
    }

    public static boolean hasLeftRecursion(Grammar grammar) {
        return !getLeftRecursionDetails(grammar).isEmpty();
    }

    public static boolean hasCommonPrefixes(Grammar grammar) {
        return !getCommonPrefixDetails(grammar).isEmpty();
    }
}
