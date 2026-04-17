package ui.core.services;

import java.util.List;

import core.lexer.models.atomic.Token;
import core.parser.core.FirstFollowTableBuilder;
import core.parser.core.GrammarReader;
import core.parser.core.ParserTableBuilder;
import core.parser.core.grammar.GrammarClassification;
import core.parser.core.grammar.GrammarClassificationBuilder;
import core.parser.models.FirstFollowTable;
import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.tree.ParseTree;

public class ParserService {
    private Grammar grammar;

    public void loadGrammar(String path) throws Exception {
        this.grammar = GrammarReader.readFromFile(path, "Program");
    }

    public FirstFollowTable buildFirstFollowTable(){
        if (grammar == null) throw new IllegalStateException("Grammar not loaded");

        FirstFollowTable firstFollowTable = FirstFollowTableBuilder.build(grammar);
        return firstFollowTable;
    }

    public ParseTable buildParseTable(FirstFollowTable firstFollowTable, List<Token> tokens){
        ParseTable parseTable = ParserTableBuilder.build(grammar, firstFollowTable);
        return parseTable;
    }

    public GrammarClassification classifyGrammarWithParserTable(ParseTable parseTable) {
        if (parseTable == null) throw new IllegalStateException("ParseTable must be not null before classifying the grammar.");

        GrammarClassification classification = new GrammarClassificationBuilder().withGrammar(grammar).withParseTable(parseTable).build();

        return classification;
    }


    public boolean isGrammarLoaded() {
        return grammar != null;
    }

    public static class SyntaxAnalysisResult {
        public final FirstFollowTable firstFollowTable;
        public final ParseTree parseTree;

        public SyntaxAnalysisResult(FirstFollowTable table, ParseTree tree) {
            this.firstFollowTable = table;
            this.parseTree = tree;
        }
    }
}