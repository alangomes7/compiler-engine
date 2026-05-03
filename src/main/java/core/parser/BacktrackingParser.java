package core.parser;

import core.lexer.models.atomic.Token;
import core.parser.models.Grammar;
import core.parser.models.Production;
import core.parser.models.atomic.ParserError;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BacktrackingParser extends Parser {
    private int maxLookaheadIndex = 0;
    private final Set<String> expectedAtMax = new HashSet<>();

    public BacktrackingParser(Grammar grammar) {
        super(grammar);
    }

    @Override
    protected void postParseCheck() {
        // Overridden to be empty: Backtracking handles error recovery
        // and EOF checking inside its own parsing loop.
    }

    @Override
    protected Node parseCore() {
        Node bestRoot = null;

        while (true) {
            this.lookaheadIndex = 0;
            this.maxLookaheadIndex = 0;
            this.expectedAtMax.clear();

            Node root = parseSymbol(grammar.getStartSymbol());
            boolean parseSuccess = (root != null);

            if (parseSuccess && lookaheadIndex < tokens.size()) {
                Token t = tokens.get(lookaheadIndex);
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

            Token failToken =
                    (maxLookaheadIndex < tokens.size()) ? tokens.get(maxLookaheadIndex) : null;

            if (failToken == null || isEofToken(failToken)) {
                if (errors.isEmpty()) {
                    int line = (failToken != null) ? failToken.getLine() : 0;
                    int col = (failToken != null) ? failToken.getCol() : 0;
                    String expected = String.join(", ", expectedAtMax);
                    String msg = String.format("Expected one of: [%s], but found EOF", expected);

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

            String message =
                    String.format(
                            "Expected one of: [%s], but found '%s' (Type: %s)",
                            expected, failLexeme, failType);

            if (!isDuplicateError(line, col, message)) {
                errors.add(new ParserError(line, col, message));
            }

            tokens.remove(maxLookaheadIndex);

            if (tokens.isEmpty()) break;
        }

        return bestRoot;
    }

    private boolean isDuplicateError(int line, int col, String message) {
        for (ParserError e : errors) {
            if (e.getLine() == line && e.getCol() == col && e.getMessage().equals(message))
                return true;
        }
        return false;
    }

    private Node parseSymbol(Symbol symbol) {
        if (symbol.equals(Symbol.EPSILON)) return new Node(Symbol.EPSILON);
        if (symbol.isTerminal() || symbol.equals(Symbol.EOF)) return matchTerminal(symbol);
        return parseNonTerminal(symbol);
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

            if (matchSuccess) return currentNode;
        }

        return null;
    }

    private Node matchTerminal(Symbol expectedTerminal) {
        Token currentToken = (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;
        Symbol lookahead = resolveLookahead(currentToken);

        if (expectedTerminal.equals(lookahead)) {
            Node node = new Node(expectedTerminal);
            if (currentToken != null) node.setLexeme(currentToken.getLexeme());
            if (!expectedTerminal.equals(Symbol.EOF)) lookaheadIndex++;
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
}
