package core.parser;

import core.lexer.models.atomic.Token;
import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class LL1Parser extends Parser {
    private final ParseTable parseTable;

    public LL1Parser(Grammar grammar, ParseTable parseTable) {
        super(grammar);
        this.parseTable = parseTable;
    }

    @Override
    protected Node parseCore() {
        Stack<Node> stack = new Stack<>();
        Node root = new Node(grammar.getStartSymbol());
        stack.push(new Node(Symbol.EOF));
        stack.push(root);

        while (!stack.isEmpty()) {
            Node currentNode = stack.pop();
            Symbol top = currentNode.getSymbol();

            if (top.equals(Symbol.EPSILON)) continue;

            Token currentToken =
                    (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;
            Symbol lookahead = resolveLookahead(currentToken);

            if (top.isTerminal() || top.equals(Symbol.EOF)) {
                if (top.equals(lookahead)) {
                    if (currentToken != null) currentNode.setLexeme(currentToken.getLexeme());
                    if (!top.equals(Symbol.EOF)) lookaheadIndex++;
                } else {
                    recordError("Expected '%s', but found '%s'", currentToken, top.getName());
                }
            } else {
                List<Production> productions = this.parseTable.getEntry(top, lookahead);

                if (productions == null || productions.isEmpty()) {
                    recordError(
                            "No rule to derive '%s' with lookahead '%s'",
                            currentToken, top.getName());

                    if (currentToken != null && !isEofToken(currentToken)) {
                        lookaheadIndex++;
                        stack.push(currentNode);
                    }
                    continue;
                }

                Production production = productions.get(0);
                List<Symbol> rhs = production.getRhs();
                List<Node> children = new ArrayList<>();

                for (Symbol s : rhs) {
                    Node child = new Node(s);
                    currentNode.addChild(child);
                    children.add(child);
                }

                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(children.get(i));
                }
            }
        }
        return root;
    }
}
