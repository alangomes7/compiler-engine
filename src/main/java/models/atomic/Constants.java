package models.atomic;

/**
 * Central repository of constant values used across the lexer and parser. Contains special symbols
 * such as epsilon (ε) and end‑of‑file ($).
 *
 * <p>This class is not intended to be instantiated; all members are static and final.
 *
 * @author Generated
 * @version 1.0
 */
public final class Constants {

    /** Private constructor to prevent instantiation. */
    private Constants() {
        // Prevent instantiation
    }

    /**
     * The epsilon symbol (ε) representing an empty string or null transition. Used in ε‑NFAs and in
     * grammar productions (ε‑productions).
     */
    public static final String EPSILON = "ε";

    /**
     * The end‑of‑file marker ($) used to indicate the end of input. Commonly employed in
     * FIRST/FOLLOW set computation and parsing tables.
     */
    public static final String EOF = "$";

    /**
     * The skip marker ($) used to indicate the skipped lexeme. Those tokens will not be passed to
     * the parser.
     */
    public static final String SKIP_TOKEN = "SKIP";
}
