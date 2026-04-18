package core.lexer.core.translators;

import core.lexer.core.conversors.ReToAFNDE;
import core.lexer.models.atomic.Rule;
import core.lexer.models.automata.AFNDE;

public class RuleParser {

    private final RuleParserStandard ruleParserStandard;
    private final ExtendedRuleParser extendedRuleParser;

    public RuleParser(ReToAFNDE generator) {
        this.ruleParserStandard = new RuleParserStandard(generator);
        this.extendedRuleParser = new ExtendedRuleParser(generator);
    }

    public AFNDE parse(Rule rule) {
        if (rule.isExtended()) {
            return extendedRuleParser.parse(rule);
        } else {
            return ruleParserStandard.parse(rule);
        }
    }
}
