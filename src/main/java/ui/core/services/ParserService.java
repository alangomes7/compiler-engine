package ui.core.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import core.lexer.models.atomic.Token;
import core.parser.LL1Parser;
import core.parser.RecursiveDescentParser;
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
import core.parser.utils.TokenFilter;
import lombok.Getter;

/**
 * Service for grammar loading, FIRST/FOLLOW computation, parse table construction, grammar
 * classification, and LL(1) parsing.
 *
 * <p>This service is the central coordinator for all parser-related operations in the application.
 * It provides a complete pipeline for:
 *
 * <ul>
 *   <li>Loading context-free grammars from files
 *   <li>Computing FIRST and FOLLOW sets for grammar analysis
 *   <li>Building LL(1) parse tables
 *   <li>Classifying grammars (LL(1) vs non-LL(1))
 *   <li>Parsing token streams to produce parse trees
 *   <li>Generating grammar tree visualizations
 * </ul>
 *
 * <p>The service maintains the loaded grammar state and must be reinitialized when a new grammar is
 * loaded.
 *
 * <p>Typical workflow:
 *
 * <pre>
 * ParserService parserService = new ParserService();
 *
 * // Load grammar
 * parserService.loadGrammar("grammar.txt");
 *
 * // Build FIRST/FOLLOW table
 * FirstFollowTable ffTable = parserService.buildFirstFollowTable();
 *
 * // Build parse table
 * ParseTable parseTable = parserService.buildParseTable(ffTable, tokens);
 *
 * // Classify grammar
 * GrammarClassification classification = parserService.classifyGrammarWithParserTable(parseTable);
 *
 * // Parse tokens
 * ParseResult result = parserService.parseTokens(parseTable, tokens);
 * </pre>
 *
 * @see Grammar
 * @see FirstFollowTable
 * @see ParseTable
 * @see LL1Parser
 */
public class ParserService {

    /** The currently loaded grammar (may be null if not yet loaded). */
    @Getter private Grammar grammar;

    /**
     * Loads a grammar from a file.
     *
     * <p>This method reads and parses a grammar specification from the given file path. The grammar
     * must be in the expected format with proper production rules and symbols.
     *
     * <p>Any previously loaded grammar is replaced by the new one.
     *
     * @param path the file path to the grammar definition
     * @throws Exception if the grammar file cannot be read or parsed, including syntax errors in
     *     the grammar definition
     */
    public void loadGrammar(String path) throws Exception {
        this.grammar = GrammarReader.readFromFile(path);
    }

    /**
     * Checks whether a grammar has been successfully loaded.
     *
     * @return {@code true} if a grammar is currently loaded, {@code false} otherwise
     */
    public boolean isGrammarLoaded() {
        return grammar != null;
    }

    /**
     * Builds the FIRST and FOLLOW sets for the loaded grammar.
     *
     * <p>FIRST sets contain the terminals that can begin strings derived from a non-terminal.
     * FOLLOW sets contain the terminals that can appear immediately to the right of a non-terminal
     * in some sentential form.
     *
     * <p>These sets are essential for constructing LL(1) parse tables and determining grammar
     * compatibility.
     *
     * @return a FirstFollowTable containing the computed FIRST and FOLLOW sets
     * @throws IllegalStateException if no grammar is loaded
     */
    public FirstFollowTable buildFirstFollowTable() {
        if (grammar == null) throw new IllegalStateException("Grammar not loaded");

        // This line is the critical link between the service and your new Builder logic
        return new FirstFollowTableBuilder(grammar).build();
    }

    /**
     * Builds an LL(1) parse table from the FIRST/FOLLOW table.
     *
     * <p>The parse table maps (non-terminal, terminal) pairs to production rules, enabling
     * deterministic parsing decisions in LL(1) parsers.
     *
     * @param firstFollowTable the precomputed FIRST and FOLLOW sets
     * @param tokens the token stream (not currently used, may be for future enhancements)
     * @return a ParseTable ready for LL(1) parsing
     */
    public ParseTable buildParseTable(FirstFollowTable firstFollowTable, List<Token> tokens) {
        return ParserTableBuilder.build(grammar, firstFollowTable);
    }

    /**
     * Classifies the grammar based on its parse table.
     *
     * <p>This method determines whether the grammar is LL(1) by checking for conflicts in the parse
     * table. A grammar is LL(1) if its parse table has no multiply-defined entries.
     *
     * @param parseTable the parse table to analyze
     * @return a GrammarClassification object containing the classification result and any conflict
     *     information
     * @throws IllegalStateException if the parse table is null
     */
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

    /**
     * Parses a token stream using the given parse table.
     *
     * <p>This method executes the LL(1) parsing algorithm on the token stream, producing a parse
     * tree and collecting any parsing errors encountered.
     *
     * @param parseTable the parse table to guide parsing decisions
     * @param tokens the token stream from lexical analysis
     * @return a ParseResult containing the parse tree and any error messages
     * @throws IllegalStateException if no grammar is loaded
     */
    public ParseResult parseTokens(ParseTable parseTable, List<Token> tokens) {
        if (grammar == null) throw new IllegalStateException("Grammar not loaded");
         ParseTree parseTree = null;
         TokenFilter tokenFilter = new TokenFilter();
         List<Token> cleanedTokens = tokenFilter.filter(tokens);

        // LL1Parser parser = new LL1Parser(grammar, parseTable);
        // parseTree = parser.parse(tokens);

        RecursiveDescentParser parser = new RecursiveDescentParser(grammar, parseTable);
        parseTree = parser.parse(cleanedTokens);

        return new ParseResult(parseTree, parser.getErrors());
    }

    /**
     * Builds a full parse tree representing the grammar structure.
     *
     * <p>This method creates a tree visualization of the grammar itself, showing how production
     * rules expand non-terminals. This is different from an input parse tree and is used for
     * grammar analysis.
     *
     * @return a ParseTree representing the grammar's recursive structure, or {@code null} if no
     *     grammar is loaded
     */
    public ParseTree buildFullGrammarTree() {
        if (grammar == null) return null;
        Node root = buildGrammarNode(grammar.getStartSymbol(), new HashSet<>());
        return new ParseTree(root);
    }

    /**
     * Recursively builds a node in the grammar tree.
     *
     * <p>Expands a symbol into its production rules, following the grammar definition. Terminal
     * symbols are leaf nodes. Visited symbols are tracked to prevent infinite recursion in case of
     * left-recursive grammars.
     *
     * @param symbol the symbol to expand
     * @param visited set of symbols already expanded in this path
     * @return the constructed Node representing the symbol and its expansions
     */
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

    /**
     * Result container for a parsing operation.
     *
     * <p>Encapsulates both the resulting parse tree and any error messages that occurred during
     * parsing. If parsing was successful, the errors list will be empty.
     */
    public static class ParseResult {
        /** The parse tree generated by the parser, or null if parsing failed. */
        public final ParseTree tree;

        /** List of error messages encountered during parsing. */
        public final List<String> errors;

        /**
         * Constructs a new ParseResult.
         *
         * @param tree the parse tree from the parsing operation
         * @param errors the list of error messages (may be empty)
         */
        public ParseResult(ParseTree tree, List<String> errors) {
            this.tree = tree;
            this.errors = errors;
        }
    }
}
