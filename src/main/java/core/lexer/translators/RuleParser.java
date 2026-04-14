package core.lexer.translators;

import core.lexer.conversors.ReToAFNDE;
import models.atomic.Token;
import models.automata.AFNDE;

public class RuleParser {

    private final StandardRuleParser standardParser;
    private final ExtendedRuleParser extendedParser;

    public RuleParser(ReToAFNDE generator) {
        this.standardParser = new StandardRuleParser(generator);
        this.extendedParser = new ExtendedRuleParser(generator);
    }

    /**
     * Master evaluation method. Routes the TokenRule to the appropriate 
     * underlying parser based on the extended file flag.
     */
    public AFNDE parse(Token rule) {
        if (rule.isExtended()) {
            return extendedParser.parse(rule);
        } else {
            return standardParser.parse(rule);
        }
    }
}