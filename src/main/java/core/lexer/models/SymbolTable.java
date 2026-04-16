package core.lexer.models;

import java.util.ArrayList;
import java.util.List;

import core.lexer.models.atomic.Token;

public class SymbolTable {
    private final List<Token> table;

    public SymbolTable() {
        this.table = new ArrayList<>();
    }

    public Token insert(String tokenType, String lexeme, int line, int col) {
        Token token = new Token(tokenType, lexeme, line, col);
        table.add(token);
        return token;
    }

    public Token lookup(String lexeme) {
        for (Token tok : table) {
            if (tok.getLexeme().equals(lexeme)) {
                return tok;
            }
        }
        return null;
    }

    public void clearTable() {
        this.table.clear();
        System.out.println("\n=== Symbol Table: cleaned ===");
        printTable();
    }

    public void printTable() {
        System.out.println("\n=== Symbol Table ===");
        if (table.isEmpty()) {
            System.out.println(" (Empty) ");
        } else {
            for (Token tok : table) {
                System.out.println(" " + tok);
            }
        }
        System.out.println("====================\n");
    }
    
    public List<Token> getTable() {
        return table;
    }
}