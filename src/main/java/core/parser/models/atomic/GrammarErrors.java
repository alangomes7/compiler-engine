package core.parser.models.atomic;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * Container for grammatical errors discovered during grammar analysis. Currently tracks left
 * recursion and common prefix (non‑LL(1) ambiguity) details.
 *
 * @author Generated
 * @version 1.0
 */
public class GrammarErrors {

    /** List of error messages describing left recursion occurrences. */
    @Getter private final List<String> leftRecursionDetails;

    /**
     * List of error messages describing common prefix conflicts (e.g., FIRST/FIRST or
     * FIRST/FOLLOW).
     */
    @Getter private final List<String> commonPrefixDetails;

    /** Constructs an empty error container (no errors). */
    public GrammarErrors() {
        this.leftRecursionDetails = new ArrayList<>();
        this.commonPrefixDetails = new ArrayList<>();
    }

    /**
     * Constructs an error container with the given error lists.
     *
     * @param leftRecursionDetails list of left recursion error messages (may be null)
     * @param commonPrefixDetails list of common prefix error messages (may be null)
     */
    public GrammarErrors(List<String> leftRecursionDetails, List<String> commonPrefixDetails) {
        this.leftRecursionDetails =
                leftRecursionDetails != null ? leftRecursionDetails : new ArrayList<>();
        this.commonPrefixDetails =
                commonPrefixDetails != null ? commonPrefixDetails : new ArrayList<>();
    }

    /**
     * Returns {@code true} if there are any recorded errors.
     *
     * @return true if left recursion or common prefix details are non‑empty
     */
    public boolean hasErrors() {
        return !leftRecursionDetails.isEmpty() || !commonPrefixDetails.isEmpty();
    }
}
