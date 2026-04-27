package core.parser.models.atomic;

/**
 * Classification of a grammar's semantic properties with respect to LL(1) parsing. Indicates
 * whether the grammar is deterministic (LL(1)), contains conflicts, exhibits left recursion, or is
 * inherently ambiguous.
 *
 * @author Generated
 * @version 1.0
 */
public enum GrammarSemantics {

    /** Grammar is unambiguous, deterministic, and free of left recursion. */
    LL1_DETERMINISTIC("Unambiguous, Deterministic, Free of Left-Recursion"),

    /** Grammar contains conflicts (e.g., FIRST/FIRST or FIRST/FOLLOW) preventing LL(1) parsing. */
    CONTAINS_CONFLICTS("Contains Conflicts"),

    /** Grammar contains direct or indirect left recursion. */
    LEFT_RECURSIVE("Grammar contains Left-Recursion"),

    /** Grammar is inherently ambiguous (no unambiguous grammar exists for the language). */
    AMBIGUOUS("Grammar is inherently Ambiguous");

    private final String description;

    /**
     * Constructs a semantic classification with a human‑readable description.
     *
     * @param description textual explanation of the semantics
     */
    GrammarSemantics(String description) {
        this.description = description;
    }

    /**
     * Returns the description of this semantic classification.
     *
     * @return the description string
     */
    public String getDescription() {
        return description;
    }
}
