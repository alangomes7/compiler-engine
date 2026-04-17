package core.parser.models.atomic;

public enum GrammarType {
    TYPE_0_UNRESTRICTED("Type-0 Unrestricted Grammar (Recursively Enumerable)"),
    TYPE_1_CONTEXT_SENSITIVE("Type-1 Context-Sensitive Grammar (CSG)"),
    TYPE_2_CONTEXT_FREE("Type-2 Context-Free Grammar (CFG)"),
    TYPE_3_REGULAR("Type-3 Regular Grammar");

    private final String description;

    GrammarType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}