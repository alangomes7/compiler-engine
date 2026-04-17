package core.parser.models.atomic;

import java.util.ArrayList;
import java.util.List;

public class GrammarErrors {
    private List<String> leftRecursionDetails;
    private List<String> commonPrefixDetails;

    public GrammarErrors() {
        this.leftRecursionDetails = new ArrayList<>();
        this.commonPrefixDetails = new ArrayList<>();
    }

    public GrammarErrors(List<String> leftRecursionDetails, List<String> commonPrefixDetails) {
        this.leftRecursionDetails = leftRecursionDetails != null ? leftRecursionDetails : new ArrayList<>();
        this.commonPrefixDetails = commonPrefixDetails != null ? commonPrefixDetails : new ArrayList<>();
    }

    public List<String> getLeftRecursionDetails() {
        return leftRecursionDetails;
    }

    public void setLeftRecursionDetails(List<String> leftRecursionDetails) {
        this.leftRecursionDetails = leftRecursionDetails;
    }

    public List<String> getCommonPrefixDetails() {
        return commonPrefixDetails;
    }

    public void setCommonPrefixDetails(List<String> commonPrefixDetails) {
        this.commonPrefixDetails = commonPrefixDetails;
    }

    public boolean hasErrors() {
        return !leftRecursionDetails.isEmpty() || !commonPrefixDetails.isEmpty();
    }
}