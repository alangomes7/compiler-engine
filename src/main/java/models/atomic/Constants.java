package models.atomic;

/**
 * Central repository of constant values used across the lexer and parser. Contains special symbols
 * such as epsilon (ε) and end‑of‑file ($).
 *
 * <p>This class is not intended to be instantiated; all members are static and final.
 */
public final class Constants {

    /** Private constructor to prevent instantiation. */
    private Constants() {}

    /**
     * The epsilon symbol ({@value}) representing an empty string or null transition. Used in ε‑NFAs
     * and in grammar productions (ε‑productions).
     */
    public static final String EPSILON = "ε";

    /**
     * The end‑of‑file marker ({@value}) used to indicate the end of input. Commonly employed in
     * FIRST/FOLLOW set computation and parsing tables.
     */
    public static final String EOF = "$";

    /**
     * The skip marker used to indicate a lexeme that should be ignored. Tokens with this value will
     * not be passed to the parser.
     */
    public static final String SKIP_TOKEN = "SKIP";

    /**
     * The name of the output folder where generated files (e.g., parser, lexer, or analysis
     * results) will be placed.
     */
    public static final String OUTPUT_FOLDER = "output/";
}
