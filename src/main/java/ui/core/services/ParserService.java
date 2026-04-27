package ui.core.services;

import core.lexer.models.atomic.Token;
import core.parser.LL1Parser;
import core.parser.core.FirstFollowTableBuilder;
import core.parser.core.ParserTableBuilder;
import core.parser.core.grammar.GrammarClassification;
import core.parser.core.grammar.GrammarClassificationBuilder;
import core.parser.core.grammar.GrammarReader;
import core.parser.models.FirstFollowTable;
import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import core.parser.models.tree.ParseTree;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

/**
 * Service for grammar loading, FIRST/FOLLOW computation, parse table construction, grammar
 * classification, and LL(1) parsing.
 */
public class ParserService {

    /** The currently loaded grammar (may be null if not yet loaded). */
    @Getter private Grammar grammar;

    public void loadGrammar(String path) throws Exception {
        this.grammar = GrammarReader.readFromFile(path);
    }

    public boolean isGrammarLoaded() {
        return grammar != null;
    }

    /**
     * Builds the FIRST and FOLLOW sets for the loaded grammar.
     *
     * @return a FirstFollowTable containing the computed sets
     * @throws IllegalStateException if no grammar is loaded
     */
    public FirstFollowTable buildFirstFollowTable() {
        if (grammar == null) throw new IllegalStateException("Grammar not loaded");

        // This line is the critical link between the service and your new Builder logic
        return new FirstFollowTableBuilder(grammar).build();
    }

    public ParseTable buildParseTable(FirstFollowTable firstFollowTable, List<Token> tokens) {
        return ParserTableBuilder.build(grammar, firstFollowTable);
    }

    public GrammarClassification classifyGrammarWithParserTable(ParseTable parseTable) {
        if (parseTable == null) {
            throw new IllegalStateException(
                    "ParseTable must be not null before classifying the grammar.");
        }
        return new GrammarClassificationBuilder()
                .withGrammar(grammar)
                .withParseTable(parseTable)
                .build();
    }

    public ParseResult parseTokens(ParseTable parseTable, List<Token> tokens) {
        if (grammar == null) throw new IllegalStateException("Grammar not loaded");

        LL1Parser parser = new LL1Parser(grammar, parseTable);
        ParseTree parseTree = parser.parse(tokens);

        return new ParseResult(parseTree, parser.getErrors());
    }

    public ParseTree buildFullGrammarTree() {
        if (grammar == null) return null;
        Node root = buildGrammarNode(grammar.getStartSymbol(), new HashSet<>());
        return new ParseTree(root);
    }

    private Node buildGrammarNode(Symbol symbol, Set<Symbol> visited) {
        Node node = new Node(symbol);

        if (symbol.isTerminal() || visited.contains(symbol)) {
            return node;
        }

        visited.add(symbol);
        for (Production prod : grammar.getProductionsFor(symbol)) {
            Node prodNode = new Node(new Symbol("::=", false));
            for (Symbol s : prod.getRhs()) {
                prodNode.addChild(buildGrammarNode(s, new HashSet<>(visited)));
            }
            node.addChild(prodNode);
        }
        return node;
    }

    public static class ParseResult {
        public final ParseTree tree;
        public final List<String> errors;

        public ParseResult(ParseTree tree, List<String> errors) {
            this.tree = tree;
            this.errors = errors;
        }
    }
}
