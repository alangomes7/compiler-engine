package ui.core.state;

import core.lexer.models.automata.DFA;
import core.parser.models.FirstFollowTable;
import core.parser.models.ParseTable;
import lombok.Data;
import ui.core.services.ParserService.ParseResult;

/**
 * Central state holder for the entire application.
 *
 * <p>This class serves as the single source of truth for the application's current analysis state.
 * It stores both state flags indicating what data is available and the actual data objects
 * themselves (automaton, parse tables, parse results).
 *
 * <p>The state is used to:
 *
 * <ul>
 *   <li>Drive UI enablement through {@link UiStateController}
 *   <li>Cache analysis results to avoid redundant computations
 *   <li>Track workflow progress through the compilation pipeline
 *   <li>Coordinate between different UI components and services
 * </ul>
 *
 * <p>This class uses Lombok's {@code @Data} annotation to generate getters, setters, {@code
 * equals()}, {@code hashCode()}, and {@code toString()} methods.
 *
 * <p><b>Typical Workflow State Progression:</b>
 *
 * <ol>
 *   <li>Token file loaded → {@code tokenLoaded = true}, {@code tokenFilePath} set
 *   <li>Grammar loaded → {@code grammarLoaded = true}
 *   <li>Lexer executed → {@code lexerRunSuccess = true}, {@code hasSymbolTableData = true}
 *   <li>Grammar compatibility validated → {@code hasFirstFollowData = true}, {@code
 *       hasParseTableData = true}
 *   <li>Parser executed → {@code parseRunSuccess = true}
 * </ol>
 *
 * <p><b>Lazy Lexer Building Design:</b> The lexer is NOT built when the token file is loaded.
 * Instead, the token file path is stored, and the {@code lexerNeedsRebuild} flag indicates whether
 * the lexer needs to be built (or rebuilt) before scanning. This approach:
 *
 * <ul>
 *   <li>Avoids unnecessary work if the user never runs the lexer
 *   <li>Ensures the lexer is always built with the latest token rules
 *   <li>Prevents outdated lexer instances from being used when token rules change
 *   <li>Improves application startup and file loading responsiveness
 * </ul>
 *
 * @see UiStateController
 * @see core.lexer.models.automata.DFA
 * @see core.parser.models.FirstFollowTable
 * @see core.parser.models.ParseTable
 * @see ui.core.services.ParserService.ParseResult
 */
@Data
public class AnalysisState {

    // ==================== STATE FLAGS ====================

    /**
     * Indicates whether a token specification file has been successfully loaded. This is the
     * foundational prerequisite for all lexer operations.
     *
     * <p>When {@code true}, {@link #tokenFilePath} contains the valid file path.
     */
    private boolean tokenLoaded = false;

    /**
     * Indicates whether a grammar file has been successfully loaded. Required for grammar analysis
     * and parser operations.
     *
     * <p>When {@code true}, {@link #grammarFilePath} contains the valid file path.
     */
    private boolean grammarLoaded = false;

    /**
     * Indicates whether the lexer has been run successfully on the current input. Must be {@code
     * true} before parser operations can be executed.
     *
     * <p>Set to {@code false} when:
     *
     * <ul>
     *   <li>Input text changes
     *   <li>Token file is reloaded
     *   <li>Lexer operation fails
     * </ul>
     */
    private boolean lexerRunSuccess = false;

    /**
     * Indicates whether the parser has been run successfully on the current input. Set to {@code
     * true} after successful parsing of input tokens.
     *
     * <p>Set to {@code false} when:
     *
     * <ul>
     *   <li>Grammar is reloaded
     *   <li>Lexer is re-run
     *   <li>Parser operation fails
     * </ul>
     */
    private boolean parseRunSuccess = false;

    /**
     * Indicates whether symbol table data from lexer analysis is currently available. When {@code
     * true}, the symbol table can be displayed and exported.
     */
    private boolean hasSymbolTableData = false;

    /**
     * Indicates whether FIRST/FOLLOW table data is currently available. Generated during grammar
     * compatibility validation.
     */
    private boolean hasFirstFollowData = false;

    /**
     * Indicates whether parsing table data is currently available. Generated during grammar
     * compatibility validation.
     */
    private boolean hasParseTableData = false;

    /**
     * Indicates whether a grammar tree visualization is currently available. Generated when the
     * user requests grammar tree display.
     */
    private boolean hasGrammarTree = false;

    /**
     * Indicates whether an input parse tree visualization is currently available. Generated when
     * the user requests input tree display after successful parsing.
     */
    private boolean hasInputTree = false;

    /**
     * Indicates whether validation output data is currently available. Generated during grammar
     * compatibility validation.
     */
    private boolean hasValidationData = false;

    /**
     * Flag used to distinguish between user-initiated changes and programmatic changes. Helps
     * prevent recursive UI updates and event loops.
     *
     * <p>When {@code true}, UI change listeners should ignore the change (e.g., when loading a file
     * programmatically into the input area).
     */
    private boolean isProgrammaticChange = false;

    /**
     * Indicates whether the lexer needs to be built (or rebuilt) before scanning.
     *
     * <p>Set to {@code true} when:
     *
     * <ul>
     *   <li>A new token file is loaded
     *   <li>The token file path changes
     *   <li>The lexer is explicitly invalidated
     *   <li>Lexer building fails
     * </ul>
     *
     * <p>Set to {@code false} after successful lexer building.
     *
     * <p>This flag enables lazy lexer building - the lexer is only constructed when actually needed
     * for scanning.
     */
    private boolean lexerNeedsRebuild = true;

    // ==================== PATH STORAGE ====================

    /**
     * The absolute path to the loaded token rule file. Used for lazy lexer building when {@link
     * #handleRunLexer()} is called.
     *
     * <p>Format: absolute file path (e.g., "/home/user/tokens.txt" or "C:\\rules.lexer")
     *
     * <p>May be {@code null} if no token file has been loaded.
     */
    private String tokenFilePath;

    /**
     * The absolute path to the loaded grammar file. Used for grammar operations and potential
     * reloading.
     *
     * <p>Format: absolute file path (e.g., "/home/user/grammar.txt" or "C:\\grammar.grammar")
     *
     * <p>May be {@code null} if no grammar file has been loaded.
     */
    private String grammarFilePath;

    // ==================== DATA HOLDERS ====================

    /**
     * The current FIRST/FOLLOW table computed from the loaded grammar.
     *
     * <p>Contains:
     *
     * <ul>
     *   <li>FIRST sets - terminals that can begin strings derived from each non-terminal
     *   <li>FOLLOW sets - terminals that can appear immediately to the right of each non-terminal
     * </ul>
     *
     * <p>Null if not yet computed or if grammar is invalid/not loaded.
     */
    private FirstFollowTable currentFirstFollowTable;

    /**
     * The current LL(1) parse table computed from the loaded grammar.
     *
     * <p>The parse table maps (non-terminal, terminal) pairs to production rules, enabling
     * deterministic parsing decisions in LL(1) parsers.
     *
     * <p>Null if not yet computed, if grammar is not LL(1) compatible, or if no grammar is loaded.
     */
    private ParseTable currentParseTable;

    /**
     * The result of the most recent parser execution.
     *
     * <p>Contains:
     *
     * <ul>
     *   <li>The parse tree structure (if parsing succeeded)
     *   <li>List of error messages (if parsing failed)
     * </ul>
     *
     * <p>Null if parsing has not been executed or was not successful.
     */
    private ParseResult currentParseResult;

    /**
     * The current DFA (Deterministic Finite Automaton) built from token specifications.
     *
     * <p>Used for lexical analysis to recognize tokens in input text. Built lazily when the user
     * first runs the lexer analysis.
     *
     * <p>Null if:
     *
     * <ul>
     *   <li>No token file has been loaded
     *   <li>Lexer has not been built yet (deferred/lazy building)
     *   <li>Lexer building failed
     * </ul>
     */
    private DFA currentAutomaton;

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Resets all analysis data except token and grammar file paths.
     *
     * <p>This method is called when:
     *
     * <ul>
     *   <li>The input text changes
     *   <li>The user clicks "Clear Tables"
     *   <li>A new token file is loaded
     * </ul>
     *
     * <p>It preserves:
     *
     * <ul>
     *   <li>Token file path and loaded flag
     *   <li>Grammar file path and loaded flag
     * </ul>
     *
     * <p>All computed results (tables, trees, automaton) are cleared.
     */
    public void resetAnalysisData() {
        // Reset computed results
        currentFirstFollowTable = null;
        currentParseTable = null;
        currentParseResult = null;
        currentAutomaton = null;

        // Reset state flags
        lexerRunSuccess = false;
        parseRunSuccess = false;
        hasSymbolTableData = false;
        hasFirstFollowData = false;
        hasParseTableData = false;
        hasGrammarTree = false;
        hasInputTree = false;
        hasValidationData = false;

        // Mark lexer for rebuild
        lexerNeedsRebuild = true;
    }

    /**
     * Resets all state including token and grammar files.
     *
     * <p>This method is called when the application is reset or when the user wants to start
     * completely fresh.
     *
     * <p>All fields are reset to their initial values.
     */
    public void resetAll() {
        resetAnalysisData();

        // Reset file paths and loaded flags
        tokenLoaded = false;
        grammarLoaded = false;
        tokenFilePath = null;
        grammarFilePath = null;

        isProgrammaticChange = false;
    }

    /**
     * Checks if the grammar is ready for validation and parsing.
     *
     * @return {@code true} if grammar is loaded and token file is loaded, {@code false} otherwise
     */
    public boolean isReadyForGrammarAnalysis() {
        return grammarLoaded && tokenLoaded;
    }

    /**
     * Checks if parsing can be performed.
     *
     * @return {@code true} if lexer has run successfully and grammar is loaded, {@code false}
     *     otherwise
     */
    public boolean isReadyForParsing() {
        return lexerRunSuccess && grammarLoaded;
    }

    /**
     * Checks if tree visualizations can be generated.
     *
     * @return {@code true} if grammar is loaded and parse results exist, {@code false} otherwise
     */
    public boolean isReadyForTreeGeneration() {
        return grammarLoaded && currentParseResult != null;
    }
}
