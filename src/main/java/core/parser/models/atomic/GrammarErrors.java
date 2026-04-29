package core.parser.models.atomic;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class GrammarErrors {

    @Getter private final List<String> leftRecursionDetails;

    @Getter private final List<String> commonPrefixDetails;

    /** Constructs an empty error container (no errors). */
    public GrammarErrors() {
        this.leftRecursionDetails = new ArrayList<>();
        this.commonPrefixDetails = new ArrayList<>();
    }

    public GrammarErrors(List<String> leftRecursionDetails, List<String> commonPrefixDetails) {
        this.leftRecursionDetails =
                leftRecursionDetails != null ? leftRecursionDetails : new ArrayList<>();
        this.commonPrefixDetails =
                commonPrefixDetails != null ? commonPrefixDetails : new ArrayList<>();
    }

    public boolean hasErrors() {
        return !leftRecursionDetails.isEmpty() || !commonPrefixDetails.isEmpty();
    }
}
