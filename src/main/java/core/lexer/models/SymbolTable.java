package core.lexer.models;

import core.lexer.models.atomic.Token;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    private final List<Token> tokenStream;

    private final Map<String, Token> fastLookupTable;

    public SymbolTable() {
        this.tokenStream = new ArrayList<>();
        this.fastLookupTable = new LinkedHashMap<>();
    }

    public Token insert(String tokenType, String lexeme, int line, int col) {
        Token token = new Token(tokenType, lexeme, line, col);

        tokenStream.add(token);

        fastLookupTable.putIfAbsent(lexeme, token);

        return token;
    }

    public Token lookup(String lexeme) {
        return fastLookupTable.get(lexeme);
    }

    public void clearTable() {
        this.tokenStream.clear();
        this.fastLookupTable.clear();
        System.out.println("\n=== Symbol Table: cleaned ===");
        printTable();
    }

    public void printTable() {
        System.out.println("\n=== Token Stream ===");
        if (tokenStream.isEmpty()) {
            System.out.println(" (Empty) ");
        } else {
            for (Token tok : tokenStream) {
                System.out.println(" " + tok);
            }
        }
        System.out.println("====================\n");
    }

    public List<Token> getTable() {
        return tokenStream;
    }

    public List<Token> getUniqueSymbols() {
        return new ArrayList<>(fastLookupTable.values());
    }
}
