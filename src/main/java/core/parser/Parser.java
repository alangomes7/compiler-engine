package core.parser;

import core.lexer.models.atomic.Token;
import core.parser.models.Grammar;
import core.parser.models.atomic.ParserError;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import core.parser.models.tree.ParseTree;
import core.parser.utils.TokenFilter;
import java.util.ArrayList;
import java.util.List;
import models.atomic.Constants;

public abstract class Parser {
    protected final Grammar grammar;
    protected final List<ParserError> errors;

    protected List<Token> tokens;
    protected int lookaheadIndex;

    protected Parser(Grammar grammar) {
        this.grammar = grammar;
        this.errors = new ArrayList<>();
    }

    public final ParseTree parse(List<Token> rawTokens) {
        initialize(rawTokens);

        Node root = parseCore();

        postParseCheck();

        return new ParseTree(root != null ? root : new Node(grammar.getStartSymbol()));
    }

    protected void initialize(List<Token> rawTokens) {
        this.errors.clear();
        this.lookaheadIndex = 0;
        TokenFilter tokenFilter = new TokenFilter();
        this.tokens = new ArrayList<>(tokenFilter.filter(rawTokens));
    }

    protected abstract Node parseCore();

    protected void postParseCheck() {
        if (lookaheadIndex < tokens.size()) {
            Token remaining = tokens.get(lookaheadIndex);
            if (remaining != null && !isEofToken(remaining)) {
                errors.add(
                        new ParserError(
                                remaining.getLine(),
                                remaining.getCol(),
                                String.format(
                                        "Unexpected tokens after program end. Found: '%s'",
                                        remaining.getLexeme())));
            }
        }
    }

    public List<ParserError> getErrors() {
        return new ArrayList<>(errors);
    }

    protected Symbol resolveLookahead(Token token) {
        if (token == null) return Symbol.EOF;

        String lexeme = token.getLexeme();
        String tokenType = token.getType();
        String normalizedType = tokenType;

        if ("comment".equals(tokenType)) normalizedType = "#";
        else if ("NEWLINE_CH".equals(tokenType)) normalizedType = "newline";
        else if (tokenType != null && tokenType.endsWith("_NUM")) normalizedType = "number";
        else if ("DIGIT".equals(tokenType)) normalizedType = "number";
        else if ("LOWER".equals(tokenType) || "UPPER".equals(tokenType))
            normalizedType = "identifier";
        else if ("INC_PRE".equals(tokenType)) normalizedType = "++_pre";
        else if ("DEC_PRE".equals(tokenType)) normalizedType = "--_pre";
        else if ("INC_POST".equals(tokenType)) normalizedType = "++_post";
        else if ("DEC_POST".equals(tokenType)) normalizedType = "--_post";

        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(normalizedType)) return terminal;
        }

        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(lexeme)) return terminal;
        }

        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(tokenType)) return terminal;
        }

        return new Symbol(normalizedType != null ? normalizedType : lexeme, true);
    }

    protected boolean isEofToken(Token token) {
        if (token.getLexeme() == null) return false;
        return token.getLexeme().equals(Constants.EOF) || token.getType().equals("EOF");
    }

    protected void recordError(
            String messageTemplate, Token currentToken, String expectedOrDerived) {
        int line = (currentToken != null) ? currentToken.getLine() : 0;
        int col = (currentToken != null) ? currentToken.getCol() : 0;
        String found = (currentToken != null) ? currentToken.getLexeme() : "EOF";

        String detail = String.format(messageTemplate, expectedOrDerived, found);

        errors.add(new ParserError(line, col, detail));
    }
}
