package core.parser.models.atomic;

/**
 * Classification of a grammar according to the Chomsky hierarchy. Defines the expressive power from
 * Type‑0 (unrestricted) down to Type‑3 (regular).
 *
 * @author Generated
 * @version 1.0
 */
public enum GrammarType {

    /** Type‑0: Unrestricted grammars, generate recursively enumerable languages. */
    TYPE_0_UNRESTRICTED("Type-0 Unrestricted Grammar (Recursively Enumerable)"),

    /** Type‑1: Context‑sensitive grammars, productions have non‑decreasing length. */
    TYPE_1_CONTEXT_SENSITIVE("Type-1 Context-Sensitive Grammar (CSG)"),

    /** Type‑2: Context‑free grammars, left‑hand side is a single nonterminal. */
    TYPE_2_CONTEXT_FREE("Type-2 Context-Free Grammar (CFG)"),

    /** Type‑3: Regular grammars, rules are either right‑linear or left‑linear. */
    TYPE_3_REGULAR("Type-3 Regular Grammar");

    private final String description;

    /**
     * Constructs a grammar type with a description.
     *
     * @param description textual explanation of the grammar class
     */
    GrammarType(String description) {
        this.description = description;
    }

    /**
     * Returns the description of this grammar type.
     *
     * @return the description string
     */
    public String getDescription() {
        return description;
    }
}
