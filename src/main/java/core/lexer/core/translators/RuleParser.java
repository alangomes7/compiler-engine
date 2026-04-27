package core.lexer.core.translators;

import core.lexer.core.conversors.ReToNFAE;
import core.lexer.models.atomic.Rule;
import core.lexer.models.automata.NFAE;

/**
 * Facade for parsing regular expression rules. Dispatches parsing to either the standard or
 * extended rule parser based on the rule's {@code extended} flag.
 *
 * @author Generated
 * @version 1.0
 */
public class RuleParser {

    private final RuleParserStandard ruleParserStandard;
    private final RuleParserExtended extendedRuleParser;

    /**
     * Constructs a RuleParser with the given NFA-ε generator.
     *
     * @param generator the converter used to build NFA-ε fragments
     */
    public RuleParser(ReToNFAE generator) {
        this.ruleParserStandard = new RuleParserStandard(generator);
        this.extendedRuleParser = new RuleParserExtended(generator);
    }

    /**
     * Parses a rule into an NFA-ε. If the rule is marked as extended, the extended parser is used;
     * otherwise, the standard parser is used.
     *
     * @param rule the rule to parse
     * @return the NFA-ε representing the rule's regular expression
     */
    public NFAE parse(Rule rule) {
        if (rule.isExtended()) {
            return extendedRuleParser.parse(rule);
        } else {
            return ruleParserStandard.parse(rule);
        }
    }
}
