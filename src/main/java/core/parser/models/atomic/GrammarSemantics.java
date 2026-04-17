package core.parser.models.atomic;

public enum GrammarSemantics {
    LL1_DETERMINISTIC("Unambiguous, Deterministic, Free of Left-Recursion"),
    CONTAINS_CONFLICTS("Contains Conflicts"),
    LEFT_RECURSIVE("Grammar contains Left-Recursion"),
    AMBIGUOUS("Grammar is inherently Ambiguous");

    private final String description;

    GrammarSemantics(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
