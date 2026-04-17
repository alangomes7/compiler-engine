package core.parser.core.grammar;

import core.parser.models.atomic.GrammarSemantics;
import core.parser.models.atomic.GrammarType;
import core.parser.models.atomic.ParserAlgorithm;

public class GrammarClassification {
    private ParserAlgorithm recommendedParser;
    private GrammarType type;
    private GrammarSemantics semantics;

    public GrammarClassification() {}

    public GrammarClassification(ParserAlgorithm recommendedParser, GrammarType type, GrammarSemantics semantics) {
        this.recommendedParser = recommendedParser;
        this.type = type;
        this.semantics = semantics;
    }

    public ParserAlgorithm getRecommendedParser() {
        return recommendedParser;
    }

    public void setRecommendedParser(ParserAlgorithm recommendedParser) {
        this.recommendedParser = recommendedParser;
    }

    public GrammarType getType() {
        return type;
    }

    public void setType(GrammarType type) {
        this.type = type;
    }

    public GrammarSemantics getSemantics() {
        return semantics;
    }

    public void setSemantics(GrammarSemantics semantics) {
        this.semantics = semantics;
    }

    @Override
    public String toString() {
        return """
                GrammarClassification {
                        type='%s',
                        recommendedParser='%s',
                        semantics='%s'
                }
                """.formatted(
                        type.getDescription(),
                        recommendedParser.getName(),
                        semantics.getDescription()
                    );
    }
}