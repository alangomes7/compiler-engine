package ui.core.state;

import core.lexer.models.automata.AFD;
import core.parser.models.FirstFollowTable;
import core.parser.models.ParseTable;
import lombok.Data;
import ui.core.services.ParserService.ParseResult;

@Data
public class AnalysisState {
    // State flags
    private boolean tokenLoaded = false;
    private boolean grammarLoaded = false;
    private boolean lexerRunSuccess = false;
    private boolean parseRunSuccess = false;

    private boolean hasSymbolTableData = false;
    private boolean hasFirstFollowData = false;
    private boolean hasParseTableData = false;
    private boolean hasGrammarTree = false;
    private boolean hasInputTree = false;
    private boolean hasValidationData = false;
    private boolean isProgrammaticChange = false;

    // Data holders
    private FirstFollowTable currentFirstFollowTable;
    private ParseTable currentParseTable;
    private ParseResult currentParseResult;
    private AFD currentAutomaton;
}
