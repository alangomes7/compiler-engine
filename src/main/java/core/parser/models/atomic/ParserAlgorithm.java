package core.parser.models.atomic;

/**
 * Enumeration of common parsing algorithms and their characteristics. Used to recommend an
 * appropriate parser based on grammar properties.
 *
 * @author Generated
 * @version 1.0
 */
public enum ParserAlgorithm {

    /** LL(1): top‑down, recursive descent with one symbol lookahead. */
    LL1("LL(1)", "Top-Down, 1 symbol lookahead"),

    /** LR(0): bottom‑up, no lookahead (very restrictive). */
    LR0("LR(0)", "Bottom-Up, 0 symbol lookahead"),

    /** SLR(1): Simple LR, bottom‑up with one symbol lookahead using FOLLOW sets. */
    SLR1("SLR(1)", "Simple LR, Bottom-Up, 1 symbol lookahead"),

    /** LALR(1): Look‑Ahead LR, bottom‑up, more powerful than SLR(1) but less than LR(1). */
    LALR1("LALR(1)", "Look-Ahead LR, Bottom-Up, 1 symbol lookahead"),

    /** LR(1): Canonical LR, bottom‑up, most powerful (largest parser tables). */
    LR1("LR(1)", "Canonical LR, Bottom-Up, 1 symbol lookahead"),

    /** LALR(1) or LR(1): either algorithm can handle certain conflict‑prone grammars. */
    LALR1_OR_LR1("LALR(1) or LR(1)", "Bottom-up parsers capable of handling conflicts");

    private final String name;
    private final String description;

    /**
     * Constructs a parser algorithm entry.
     *
     * @param name short name of the algorithm (e.g., "LL(1)")
     * @param description longer description of the algorithm's characteristics
     */
    ParserAlgorithm(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Returns the short name of the algorithm.
     *
     * @return the algorithm name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of the algorithm.
     *
     * @return the description string
     */
    public String getDescription() {
        return description;
    }
}
