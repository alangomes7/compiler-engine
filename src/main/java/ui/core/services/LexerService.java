package ui.core.services;

import core.lexer.Lexer;
import core.lexer.core.translators.RuleReader;
import core.lexer.models.atomic.Rule;
import core.lexer.models.atomic.Token;
import core.lexer.models.automata.AFD;
import java.util.List;
import java.util.function.Consumer;

public class LexerService {

    private Lexer lexer;

    public AFD buildLexer(String filePath, Consumer<String> log) throws Exception {
        log.accept("📥 Reading rules...");
        List<Rule> rules = RuleReader.readRules(filePath);

        log.accept("Creating lexer...");
        this.lexer = new Lexer(rules);

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
