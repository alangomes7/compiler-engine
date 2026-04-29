package ui.core.state;

import core.lexer.models.automata.DFA;
import core.parser.models.FirstFollowTable;
import core.parser.models.ParseTable;
import lombok.Data;
import ui.core.services.ParserService.ParseResult;

@Data
public class AnalysisState {

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

    private boolean lexerNeedsRebuild = true;

    private String tokenFilePath;

    private String grammarFilePath;

    private FirstFollowTable currentFirstFollowTable;

    private ParseTable currentParseTable;

    private ParseResult currentParseResult;

    private DFA currentAutomaton;

    public void resetAnalysisData() {
        // Reset computed results
        currentFirstFollowTable = null;
        currentParseTable = null;
        currentParseResult = null;
        currentAutomaton = null;

        lexerRunSuccess = false;
        parseRunSuccess = false;
        hasSymbolTableData = false;
        hasFirstFollowData = false;
        hasParseTableData = false;
        hasGrammarTree = false;
        hasInputTree = false;
        hasValidationData = false;

        lexerNeedsRebuild = true;
    }

    public void resetAll() {
        resetAnalysisData();

        tokenLoaded = false;
        grammarLoaded = false;
        tokenFilePath = null;
        grammarFilePath = null;

        isProgrammaticChange = false;
    }

    public boolean isReadyForGrammarAnalysis() {
        return grammarLoaded && tokenLoaded;
    }

    public boolean isReadyForParsing() {
        return lexerRunSuccess && grammarLoaded;
    }

    public boolean isReadyForTreeGeneration() {
        return grammarLoaded && currentParseResult != null;
    }
}
