package models;

public class FirstFollowRow {
    private final String nonTerminal;
    private final String firstSet;
    private final String followSet;

    public FirstFollowRow(String nt, String first, String follow) {
        this.nonTerminal = nt;
        this.firstSet = first;
        this.followSet = follow;
    }

    public String getNonTerminal() { return nonTerminal; }
    public String getFirstSet() { return firstSet; }
    public String getFollowSet() { return followSet; }
}