package ui.core.services;

import core.lexer.Lexer;
import core.lexer.core.translators.RuleReader;
import core.lexer.models.atomic.Rule;
import core.lexer.models.atomic.Token;
import core.lexer.models.automata.DFA;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service for building the lexer from rule files and scanning input. Encapsulates the lexer
 * construction pipeline and token stream access.
 *
 * @author Generated
 * @version 1.0
 */
public class LexerService {

    private Lexer lexer;

    /**
     * Builds the lexer from a rule definition file.
     *
     * @param filePath the path to the token rules file
     * @param log a consumer for status messages (e.g., UI log area)
     * @return the minimized DFA used by the lexer
     * @throws Exception if rule reading or lexer construction fails
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
     * @param input the source code to scan
     * @return a status message ("Scanning complete." or error description)
     * @throws IllegalStateException if the lexer has not been initialised
     */
    public String scan(String input) {
        if (lexer == null) throw new IllegalStateException("Lexer not initialized");
        lexer.getSymbolTable().clearTable();
        return lexer.scan(input);
    }

    /**
     * Returns the full token stream (including duplicates) after scanning.
     *
     * @return list of tokens, or empty list if no scan has been performed
     */
    public List<Token> getSymbolTable() {
        return lexer.getSymbolTable().getTable();
    }

    /**
     * Checks whether the lexer has been successfully built.
     *
     * @return true if the lexer is ready for scanning
     */
    public boolean isInitialized() {
        return lexer != null;
    }
}
