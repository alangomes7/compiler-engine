package core.parser.models.atomic;

public enum ParserAlgorithm {
    LL1("LL(1)", "Top-Down, 1 symbol lookahead"),
    LR0("LR(0)", "Bottom-Up, 0 symbol lookahead"),
    SLR1("SLR(1)", "Simple LR, Bottom-Up, 1 symbol lookahead"),
    LALR1("LALR(1)", "Look-Ahead LR, Bottom-Up, 1 symbol lookahead"),
    LR1("LR(1)", "Canonical LR, Bottom-Up, 1 symbol lookahead"),
    LALR1_OR_LR1("LALR(1) or LR(1)", "Bottom-up parsers capable of handling conflicts");

    private final String name;
    private final String description;

    ParserAlgorithm(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}