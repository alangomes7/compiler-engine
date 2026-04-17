package core.parser.core.grammar;

import core.parser.models.atomic.GrammarErrors;
import core.parser.models.atomic.GrammarSemantics;
import core.parser.models.atomic.GrammarType;
import core.parser.models.atomic.ParserAlgorithm;

public class GrammarClassification {
    private ParserAlgorithm recommendedParser;
    private GrammarType type;
    private GrammarSemantics semantics;
    private GrammarErrors errors;

    public GrammarClassification() {}

    public GrammarClassification(ParserAlgorithm recommendedParser, GrammarType type, GrammarSemantics semantics) {
        this.recommendedParser = recommendedParser;
        this.type = type;
        this.semantics = semantics;
        this.errors = new GrammarErrors(); // Initialize empty to avoid nulls
    }

    public ParserAlgorithm getRecommendedParser() { return recommendedParser; }
    public void setRecommendedParser(ParserAlgorithm recommendedParser) { this.recommendedParser = recommendedParser; }

    public GrammarType getType() { return type; }
    public void setType(GrammarType type) { this.type = type; }

    public GrammarSemantics getSemantics() { return semantics; }
    public void setSemantics(GrammarSemantics semantics) { this.semantics = semantics; }

    public GrammarErrors getErrors() { return errors; }
    public void setErrors(GrammarErrors errors) { this.errors = errors; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
                GrammarClassification
                {
                    type='%s',
                    recommendedParser='%s',
                    semantics='%s'
                """, 
                type.getDescription(), 
                recommendedParser.getName(), 
                semantics.getDescription()));

        if (errors != null && errors.hasErrors()) {
            sb.append("\n    Conflicts:\n");
            for (String lrError : errors.getLeftRecursionDetails()) {
                sb.append("      - ❌ ").append(lrError).append("\n");
            }
            for (String cpError : errors.getCommonPrefixDetails()) {
                sb.append("      - ❌ ").append(cpError).append("\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }
}