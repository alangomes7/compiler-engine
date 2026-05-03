package core.parser;

import core.lexer.models.atomic.Token;
import core.parser.models.Grammar;
import core.parser.models.Production;
import core.parser.models.atomic.ParserError;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import core.parser.models.tree.ParseTree;
import core.parser.utils.TokenFilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.atomic.Constants;

public class BacktrackingParser {
    private final Grammar grammar;
    private final List<ParserError> errors;

    private List<Token> tokens;
    private int lookaheadIndex;

    private int maxLookaheadIndex = 0;
    private final Set<String> expectedAtMax = new HashSet<>();

    public BacktrackingParser(Grammar grammar) {
        this.grammar = grammar;
        this.errors = new ArrayList<>();
    }

    public ParseTree parse(List<Token> rawTokens) {
        this.errors.clear();
        TokenFilter tokenFilter = new TokenFilter();
        
        List<Token> workingTokens = new ArrayList<>(tokenFilter.filter(rawTokens));
        Node bestRoot = null;

        while (true) {
            this.tokens = workingTokens;
            this.lookaheadIndex = 0;
            this.maxLookaheadIndex = 0;
            this.expectedAtMax.clear();

            Node root = parseSymbol(grammar.getStartSymbol());
            boolean parseSuccess = (root != null);

            if (parseSuccess && lookaheadIndex < workingTokens.size()) {
                Token t = workingTokens.get(lookaheadIndex);
                if (!isEofToken(t)) {
                    parseSuccess = false;
                    if (lookaheadIndex >= maxLookaheadIndex) {
                        maxLookaheadIndex = lookaheadIndex;
                        expectedAtMax.clear();
                        expectedAtMax.add("EOF");
                    }
                }
            }

            if (parseSuccess) {
                if (bestRoot == null) bestRoot = root;
                break;
            }

            Token failToken = (maxLookaheadIndex < workingTokens.size()) ? workingTokens.get(maxLookaheadIndex) : null;
            
            if (failToken == null || isEofToken(failToken)) {
                if (errors.isEmpty()) {
                    int line = (failToken != null) ? failToken.getLine() : 0;
                    int col = (failToken != null) ? failToken.getCol() : 0;
                    String expected = String.join(", ", expectedAtMax);
                    String msg = String.format("Syntax Error at [%d, %d]: Expected one of: [%s], but found EOF", line, col, expected);
                    
                    if (!isDuplicateError(line, col, msg)) {
                        errors.add(new ParserError(line, col, msg));
                    }
                }
                break;
            }

            int line = failToken.getLine();
            int col = failToken.getCol();
            String failLexeme = failToken.getLexeme();
            String failType = failToken.getType();
            String expected = String.join(", ", expectedAtMax);

            String message = String.format("Syntax Error at [%d, %d]: Expected one of: [%s], but found '%s' (Type: %s)", line, col, expected, failLexeme, failType);
            
            if (!isDuplicateError(line, col, message)) {
                errors.add(new ParserError(line, col, message));
            }

            workingTokens.remove(maxLookaheadIndex);

            if (workingTokens.isEmpty()) {
                break;
            }
        }

        return new ParseTree(bestRoot != null ? bestRoot : new Node(grammar.getStartSymbol()));
    }

    private boolean isDuplicateError(int line, int col, String message) {
        for (ParserError e : errors) {
            if (e.getLine() == line && e.getCol() == col && e.getMessage().equals(message)) {
                return true;
            }
        }
        return false;
    }

    private Node parseSymbol(Symbol symbol) {
        if (symbol.equals(Symbol.EPSILON)) {
            return new Node(Symbol.EPSILON);
        }

        if (symbol.isTerminal() || symbol.equals(Symbol.EOF)) {
            return matchTerminal(symbol);
        } else {
            return parseNonTerminal(symbol);
        }
    }

    private Node parseNonTerminal(Symbol nonTerminal) {
        List<Production> productions = grammar.getProductionsFor(nonTerminal);
        int savedIndex = this.lookaheadIndex;

        for (Production production : productions) {
            this.lookaheadIndex = savedIndex;

            Node currentNode = new Node(nonTerminal);
            boolean matchSuccess = true;

            for (Symbol rhsSymbol : production.getRhs()) {
                if (rhsSymbol.equals(Symbol.EPSILON)) continue;

                Node childNode = parseSymbol(rhsSymbol);

                if (childNode != null) {
                    currentNode.addChild(childNode);
                } else {
                    matchSuccess = false;
                    break;
                }
            }

            if (matchSuccess) {
                return currentNode;
            }
        }

        return null;
    }

    private Node matchTerminal(Symbol expectedTerminal) {
        Token currentToken = (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;

        Symbol lookahead = resolveLookahead(currentToken);

        if (expectedTerminal.equals(lookahead)) {
            Node node = new Node(expectedTerminal);

            if (currentToken != null) {
                node.setLexeme(currentToken.getLexeme());
            }

            if (!expectedTerminal.equals(Symbol.EOF)) {
                lookaheadIndex++;
            }

            return node;
        }

        if (lookaheadIndex > maxLookaheadIndex) {
            maxLookaheadIndex = lookaheadIndex;
            expectedAtMax.clear();
            expectedAtMax.add(expectedTerminal.getName());
        } else if (lookaheadIndex == maxLookaheadIndex) {
            expectedAtMax.add(expectedTerminal.getName());
        }

        return null;
    }

    private Symbol resolveLookahead(Token token) {
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

    private boolean isEofToken(Token token) {
        if (token.getLexeme() == null) return false;
        return token.getLexeme().equals(Constants.EOF) || token.getType().equals("EOF");
    }

    public List<ParserError> getErrors() {
        return new ArrayList<>(errors);
    }
}