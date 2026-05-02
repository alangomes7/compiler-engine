package core.parser;

import core.lexer.models.atomic.Token;
import core.parser.models.Grammar;
import core.parser.models.ParseTable;
import core.parser.models.Production;
import core.parser.models.atomic.Symbol;
import core.parser.models.tree.Node;
import core.parser.models.tree.ParseTree;
import core.parser.utils.TokenFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import models.atomic.Constants;

public class LL1Parser {
    private final Grammar grammar;
    private final ParseTable parseTable;
    private final List<String> errors;

    public LL1Parser(Grammar grammar, ParseTable parseTable) {
        this.grammar = grammar;
        this.parseTable = parseTable;
        this.errors = new ArrayList<>();
    }

    public ParseTree parse(List<Token> rawTokens) {
        errors.clear();
        Stack<Node> stack = new Stack<>();

        TokenFilter tokenFilter = new TokenFilter();
        List<Token> tokens = tokenFilter.filter(rawTokens);

        Node root = new Node(grammar.getStartSymbol());
        stack.push(new Node(Symbol.EOF));
        stack.push(root);

        int lookaheadIndex = 0;

        while (!stack.isEmpty()) {
            Node currentNode = stack.pop();
            Symbol top = currentNode.getSymbol();

            if (top.equals(Symbol.EPSILON)) {
                continue;
            }

            Token currentToken =
                    (lookaheadIndex < tokens.size()) ? tokens.get(lookaheadIndex) : null;
            Symbol lookahead = resolveLookahead(currentToken);

            if (top.isTerminal() || top.equals(Symbol.EOF)) {
                if (top.equals(lookahead)) {
                    if (currentToken != null) {
                        currentNode.setLexeme(currentToken.getLexeme());
                    }
                    if (!top.equals(Symbol.EOF)) {
                        lookaheadIndex++;
                    }
                } else {
                    // ERROR RECOVERY: Record error, pretend missing terminal was matched (leave
                    // lookahead as is)
                    recordError("Expected '%s', but found '%s'", currentToken, top.getName());
                }
            } else {
                List<Production> productions = this.parseTable.getEntry(top, lookahead);

                if (productions == null || productions.isEmpty()) {
                    // ERROR RECOVERY: Record error, skip unexpected token, push non-terminal back
                    // to try again
                    recordError(
                            "No rule to derive '%s' with lookahead '%s'",
                            currentToken, top.getName());

                    if (currentToken != null && !isEofToken(currentToken)) {
                        lookaheadIndex++;
                        stack.push(
                                currentNode); // Try expanding this non-terminal again with the next
                        // token
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

        if (lookaheadIndex < tokens.size()) {
            Token remaining = tokens.get(lookaheadIndex);
            if (remaining != null && !isEofToken(remaining)) {
                errors.add(
                        "Syntax Error: Unexpected tokens after program end. Found: "
                                + remaining.getLexeme());
            }
        }

        return new ParseTree(root);
    }

    private Symbol resolveLookahead(Token token) {
        if (token == null) {
            return Symbol.EOF;
        }

        String lexeme = token.getLexeme();
        String tokenType = token.getType();

        String normalizedType = tokenType;
        if ("comment".equals(tokenType)) {
            normalizedType = "#";
        } else if ("NEWLINE_CH".equals(tokenType)) {
            normalizedType = "newline";
        } else if (tokenType != null && tokenType.endsWith("_NUM")) {
            normalizedType = "number";
        } else if ("DIGIT".equals(tokenType)) {
            normalizedType = "number";
        } else if ("LOWER".equals(tokenType) || "UPPER".equals(tokenType)) {
            normalizedType = "identifier";
        } else if ("INC_PRE".equals(tokenType)) {
            normalizedType = "++_pre";
        } else if ("DEC_PRE".equals(tokenType)) {
            normalizedType = "--_pre";
        } else if ("INC_POST".equals(tokenType)) {
            normalizedType = "++_post";
        } else if ("DEC_POST".equals(tokenType)) {
            normalizedType = "--_post";
        }

        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(normalizedType)) {
                return terminal;
            }
        }

        for (Symbol terminal : grammar.getTerminals()) {
            if (terminal.getName().equals(lexeme)) {
                return terminal;
            }
        }

        return new Symbol(tokenType != null ? tokenType : lexeme, true);
    }

    private void recordError(String messageTemplate, Token currentToken, String expectedOrDerived) {
        int line = (currentToken != null) ? currentToken.getLine() : 0;
        int col = (currentToken != null) ? currentToken.getCol() : 0;
        String found = (currentToken != null) ? currentToken.getLexeme() : "EOF";

        String detail = String.format(messageTemplate, expectedOrDerived, found);
        errors.add(String.format("Syntax Error at line %d:%d: %s", line, col, detail));
    }

    private boolean isEofToken(Token token) {
        if (token.getLexeme() == null) return false;
        return token.getLexeme().equals(Constants.EOF) || token.getType().equals("EOF");
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}
