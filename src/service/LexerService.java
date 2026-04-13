package service;

import java.util.List;

import models.TokenRule;
import scanner.Scanner;
import scanner.TokenReader;

public class LexerService {

    private Scanner lexer;

    public LexerService() {
        try {
            String path = "resources/ada-tokens.txt";
            List<TokenRule> rules = TokenReader.readTokens(path);
            lexer = new Scanner(rules);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String analyze(String sourceCode) {
        lexer.scan(sourceCode);

        String result = lexer.getSymbolTable().toString();
        lexer.getSymbolTable().clearTable();

        return result;
    }
}