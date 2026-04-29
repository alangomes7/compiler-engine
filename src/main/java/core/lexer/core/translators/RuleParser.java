package core.lexer.core.translators;

import core.lexer.core.conversors.ReToNFAE;
import core.lexer.models.atomic.Rule;
import core.lexer.models.automata.NFAE;

public class RuleParser {

    private final RuleParserStandard ruleParserStandard;
    private final RuleParserExtended extendedRuleParser;

    public RuleParser(ReToNFAE generator) {
        this.ruleParserStandard = new RuleParserStandard(generator);
        this.extendedRuleParser = new RuleParserExtended(generator);
    }

    public NFAE parse(Rule rule) {
        if (rule.isExtended()) {
            return extendedRuleParser.parse(rule);
        } else {
            return ruleParserStandard.parse(rule);
        }
    }
}
