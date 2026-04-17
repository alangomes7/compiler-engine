package core.parser.core.grammar;

import java.util.List;
import java.util.Map;

import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.GrammarSemantics;
import core.parser.models.atomic.GrammarType;
import core.parser.models.atomic.ParserAlgorithm;
import core.parser.models.atomic.Symbol;

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
            throw new IllegalStateException("Both Grammar and ParseTable must be provided before building the classification.");
        }

        // Parsing tables natively deal with Type-2 grammars
        GrammarType type = GrammarType.TYPE_2_CONTEXT_FREE;
        ParserAlgorithm recommendedParser;
        GrammarSemantics semantics;

        if (isLL1(this.parseTable)) {
            recommendedParser = ParserAlgorithm.LL1;
            semantics = GrammarSemantics.LL1_DETERMINISTIC;
        } else {
            recommendedParser = ParserAlgorithm.LALR1_OR_LR1;
            semantics = GrammarSemantics.CONTAINS_CONFLICTS;
        }

        return new GrammarClassification(recommendedParser, type, semantics);
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