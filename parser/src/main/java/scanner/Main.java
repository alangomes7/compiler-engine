package scanner;

import java.util.ArrayList;
import java.util.List;
import models.TokenRule;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing Lexical Analyzer System...\n");

        List<TokenRule> lexicalRules = new ArrayList<>();
        
        lexicalRules.add(new TokenRule("NUMBER", "[ - ]? [ [ 0-9 ]+ [ . [ 0-9 ]* ]? | [ . [ 0-9 ]+ ] ] [ [ e / E ] [ - ]? [ 0-9 ]+ ]?"));
        lexicalRules.add(new TokenRule("STRING", "\" [ [ ^ \" \\ ] | [ \\ [ \" / \\ / n / r / t ] ] ]* \""));
        lexicalRules.add(new TokenRule("IDENTIFIER", "[ a-zA-Z!$%&*\\/:<=>?^_~ ] [ a-zA-Z0-9!$%&*\\/:<=>?^_~+\\-.@ ]*"));
        lexicalRules.add(new TokenRule("BOOLEAN", "# [ t / T / f / F ]"));

        Scanner lexer = new Scanner(lexicalRules);

        System.out.println("=== Final Minimized Master Automaton ===");
        System.out.println(lexer.getMasterAutomaton().toString());

    }
}