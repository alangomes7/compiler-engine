package core.parser.models.atomic;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class GrammarErrors {
    @Getter private List<String> leftRecursionDetails;
    @Getter private List<String> commonPrefixDetails;

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
