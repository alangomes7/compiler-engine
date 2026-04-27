package ui.core.services;

import core.lexer.Lexer;
import core.lexer.core.translators.RuleReader;
import core.lexer.models.atomic.Rule;
import core.lexer.models.atomic.Token;
import core.lexer.models.automata.DFA;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service for building the lexer from rule files and scanning input.
 *
 * <p>This service encapsulates the complete lexer construction pipeline:
 *
 * <ol>
 *   <li>Reading token rule definitions from a file
 *   <li>Building a DFA (Deterministic Finite Automaton) from the rules
 *   <li>Scanning input text to produce a stream of tokens
 *   <li>Providing access to the resulting symbol table (token stream)
 * </ol>
 *
 * <p>The lexer is built once and can be reused for multiple input scans. Each call to {@link
 * #scan(String)} clears the previous symbol table.
 *
 * <p>Typical usage:
 *
 * <pre>
 * LexerService lexerService = new LexerService();
 *
 * // Build lexer from rules file
 * DFA automaton = lexerService.buildLexer("tokens.txt", System.out::println);
 *
 * // Scan input
 * String result = lexerService.scan("int x = 42;");
 *
 * // Get tokens
 * List&lt;Token&gt; tokens = lexerService.getSymbolTable();
 * </pre>
 *
 * @see Lexer
 * @see RuleReader
 * @see Token
 */
public class LexerService {

    private Lexer lexer;

    /**
     * Builds the lexer from a rule definition file.
     *
     * <p>This method performs the following steps:
     *
     * <ol>
     *   <li>Reads token rule definitions from the specified file
     *   <li>Creates a lexer instance with those rules
     *   <li>Builds and minimizes the DFA for efficient token recognition
     * </ol>
     *
     * <p>Progress messages are sent to the provided log consumer, allowing the UI to display
     * feedback during the (potentially time-consuming) lexer construction process.
     *
     * @param filePath the path to the token rules file; must not be null
     * @param log a consumer for status messages (e.g., UI log area); can be used to display
     *     progress updates
     * @return the minimized DFA used by the lexer (for visualization or export)
     * @throws Exception if rule reading or lexer construction fails, including file I/O errors or
     *     invalid rule syntax
     * @throws IllegalStateException if the lexer cannot be initialized
     */
    public DFA buildLexer(String filePath, Consumer<String> log) throws Exception {
        log.accept("📥 Reading rules...");
        List<Rule> rules = RuleReader.readRules(filePath);

        log.accept("Creating lexer...");
        this.lexer = new Lexer(rules);

        return this.lexer.getMasterAutomaton();
    }

    /**
     * Scans an input string using the previously built lexer.
     *
     * <p>This method processes the input text, identifies tokens according to the rules defined
     * during lexer construction, and populates the symbol table with the resulting token stream.
     *
     * <p>Any previous scan results are cleared at the beginning of this method.
     *
     * <p>If scanning encounters an error (e.g., unrecognized token), the error description is
     * returned and the symbol table may be incomplete.
     *
     * @param input the source code or input text to scan; must not be null
     * @return a status message - either "Scanning complete." on success, or an error description if
     *     scanning fails
     * @throws IllegalStateException if the lexer has not been initialized by calling {@link
     *     #buildLexer(String, Consumer)} first
     */
    public String scan(String input) {
        if (lexer == null) throw new IllegalStateException("Lexer not initialized");
        lexer.getSymbolTable().clearTable();
        return lexer.scan(input);
    }

    /**
     * Returns the full token stream (including duplicates) after scanning.
     *
     * <p>The token stream represents the complete lexical analysis of the most recent input
     * processed by {@link #scan(String)}. Each token includes its type, lexeme, and position
     * information.
     *
     * <p>If no scan has been performed or the symbol table is empty, an empty list is returned.
     *
     * @return list of tokens from the most recent scan, or empty list if none available
     */
    public List<Token> getSymbolTable() {
        return lexer.getSymbolTable().getTable();
    }

    /**
     * Checks whether the lexer has been successfully built and is ready for scanning.
     *
     * <p>This method returns {@code true} only after a successful call to {@link
     * #buildLexer(String, Consumer)}.
     *
     * @return {@code true} if the lexer is ready for scanning, {@code false} otherwise
     */
    public boolean isInitialized() {
        return lexer != null;
    }
}
