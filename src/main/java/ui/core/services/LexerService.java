package ui.core.services;

import core.lexer.Lexer;
import core.lexer.core.translators.RuleReader;
import core.lexer.models.atomic.Rule;
import core.lexer.models.atomic.Token;
import core.lexer.models.automata.DFA;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LexerService {

    private static final Logger log = LoggerFactory.getLogger(LexerService.class);

    private Lexer lexer;

    public DFA buildLexer(String filePath, Consumer<String> logCallback) throws Exception {
        log.info("📥 Reading rules...");
        logCallback.accept("📥 Reading rules...");
        List<Rule> rules = RuleReader.readRules(filePath);

        log.info("Creating lexer...");
        logCallback.accept("Creating lexer...");
        this.lexer = new Lexer(rules);

        log.info("Loading context-sensitive rules...");
        logCallback.accept("Loading context-sensitive rules...");
        RuleReader.loadContextRulesIntoLexer(this.lexer, filePath);

        return this.lexer.getMasterAutomaton();
    }

    public String scan(String input) {
        if (lexer == null) throw new IllegalStateException("Lexer not initialized");
        lexer.getSymbolTable().clearTable();
        return lexer.scan(input);
    }

    public List<Token> getSymbolTable() {
        return lexer.getSymbolTable().getTable();
    }

    public boolean isInitialized() {
        return lexer != null;
    }
}
