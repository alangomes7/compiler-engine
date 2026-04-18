package core.parser.core.grammar;

import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.GrammarErrors;
import core.parser.models.atomic.GrammarSemantics;
import core.parser.models.atomic.GrammarType;
import core.parser.models.atomic.ParserAlgorithm;
import core.parser.models.atomic.Symbol;
import java.util.List;
import java.util.Map;

public class GrammarClassificationBuilder {

    private Grammar grammar;
    private ParseTable parseTable;

    public GrammarClassificationBuilder withGrammar(Grammar grammar) {
        this.grammar = grammar;
        return this;
    }

    public GrammarClassificationBuilder withParseTable(ParseTable parseTable) {
        this.parseTable = parseTable;
        return this;
    }

    public GrammarClassification build() {
        if (this.grammar == null || this.parseTable == null) {
            throw new IllegalStateException(
                    "Both Grammar and ParseTable must be provided before building the classification.");
        }

        GrammarType type = GrammarType.TYPE_2_CONTEXT_FREE;
        ParserAlgorithm recommendedParser;
        GrammarSemantics semantics;
        GrammarErrors errors = new GrammarErrors(); // Default to no errors

        if (isLL1(this.parseTable)) {
            recommendedParser = ParserAlgorithm.LL1;
            semantics = GrammarSemantics.LL1_DETERMINISTIC;
        } else {
            recommendedParser = ParserAlgorithm.LALR1_OR_LR1;

            // Retrieve full error details
            errors = GrammarAnalyzer.analyzeGrammar(this.grammar);

            // Assign semantics based on the generated GrammarErrors object
            if (!errors.getLeftRecursionDetails().isEmpty()) {
                semantics = GrammarSemantics.LEFT_RECURSIVE;
            } else if (!errors.getCommonPrefixDetails().isEmpty()) {
                semantics = GrammarSemantics.CONTAINS_CONFLICTS;
            } else {
                semantics = GrammarSemantics.AMBIGUOUS;
            }
        }

        GrammarClassification classification =
                new GrammarClassification(recommendedParser, type, semantics);
        classification.setErrors(errors);

        return classification;
    }

    private boolean isLL1(ParseTable parseTable) {
        if (parseTable == null || parseTable.getTable() == null) {
            return false;
        }

        Map<Symbol, Map<Symbol, List<Production>>> table = parseTable.getTable();

        for (Map<Symbol, List<Production>> row : table.values()) {
            for (List<Production> cell : row.values()) {
                if (cell.size() > 1) {
                    return false; // Conflict found, not LL(1)
                }
            }
        }

        return true;
    }
}
