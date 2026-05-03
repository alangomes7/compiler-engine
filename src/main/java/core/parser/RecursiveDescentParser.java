package core.parser;

import core.lexer.models.atomic.Token;
import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import java.util.List;

public class RecursiveDescentParser extends Parser {
    private final ParseTable parseTable;

    public RecursiveDescentParser(Grammar grammar, ParseTable parseTable) {
        super(grammar);
        this.parseTable = parseTable;
    }

    @Override
    protected Node parseCore() {
        Node root = parseNonTerminal(grammar.getStartSymbol());
        matchTerminal(Symbol.EOF);
        return root;
    }

    private Node parseSymbol(Symbol symbol) {
        if (symbol.equals(Symbol.EPSILON)) return null;

        if (symbol.isTerminal() || symbol.equals(Symbol.EOF)) {
            return matchTerminal(symbol);
        } else {
            return parseNonTerminal(symbol);
        }
    }

    private Node parseNonTerminal(Symbol nonTerminal) {
        Node currentNode = new Node(nonTerminal);
        Token currentToken = (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;
        Symbol lookahead = resolveLookahead(currentToken);

        List<Production> productions = this.parseTable.getEntry(nonTerminal, lookahead);

        if (productions == null || productions.isEmpty()) {
            recordError(
                    "No rule to derive '%s' with lookahead '%s'",
                    currentToken, nonTerminal.getName());

            if (currentToken != null && !isEofToken(currentToken)) lookaheadIndex++;
            return currentNode;
        }

        Production production = productions.get(0);

        for (Symbol rhsSymbol : production.getRhs()) {
            if (rhsSymbol.equals(Symbol.EPSILON)) continue;

            Node childNode = parseSymbol(rhsSymbol);
            if (childNode != null) currentNode.addChild(childNode);
        }

        return currentNode;
    }

    private Node matchTerminal(Symbol expectedTerminal) {
        Node node = new Node(expectedTerminal);
        Token currentToken = (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;
        Symbol lookahead = resolveLookahead(currentToken);

        if (expectedTerminal.equals(lookahead)) {
            if (currentToken != null) node.setLexeme(currentToken.getLexeme());
            if (!expectedTerminal.equals(Symbol.EOF)) lookaheadIndex++;
        } else {
            recordError("Expected '%s', but found '%s'", currentToken, expectedTerminal.getName());
        }

        return node;
    }
}
