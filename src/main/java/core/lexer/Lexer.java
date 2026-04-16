package core.lexer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.lexer.conversors.AFDMinimizer;
import core.lexer.conversors.AFNDEtoAFND;
import core.lexer.conversors.AFNDtoAFD;
import core.lexer.conversors.ReToAFNDE;
import core.lexer.models.SymbolTable;
import core.lexer.models.atomic.Rule;
import core.lexer.models.atomic.State;
import core.lexer.models.automata.AFD;
import core.lexer.models.automata.AFND;
import core.lexer.models.automata.AFNDE;
import core.lexer.translators.RuleParser;

public class Lexer {
    
    private static final Logger log = LoggerFactory.getLogger(Lexer.class);

    private AFD masterAutomaton;
    private final List<Rule> rules;
    private final SymbolTable symbolTable;
    
    private final Set<String> dynamicTokens;
    private final Set<String> skipTokens;

    public Lexer(List<Rule> rules) {
        this.rules = rules;
        log.info("--- Building Scanner For: {} Tokens ---", rules.size());
        
        this.symbolTable = new SymbolTable();
        this.dynamicTokens = new HashSet<>();
        this.skipTokens = new HashSet<>();
        
        categorizeRules();
        buildScanner();
    }

    private void categorizeRules() {
        String metacharacterPattern = ".*[\\\\\\[\\]\\(\\)\\*\\+\\?\\|].*";
        for (Rule rule : rules) {
            if (rule.isSkip()) {
                skipTokens.add(rule.getTokenType());
            }
            if (rule.getRegex().matches(metacharacterPattern)) {
                dynamicTokens.add(rule.getTokenType());
            }
        }
    }

    private void buildScanner() {
        log.info("--- Building Scanner Pipeline ---");
        long totalStart = System.nanoTime();
        long start, end;

        start = System.nanoTime();
        ReToAFNDE afndeGenerator = new ReToAFNDE();
        RuleParser parser = new RuleParser(afndeGenerator);
        List<AFNDE> afndeList = new ArrayList<>(rules.size());

        for (Rule rule : rules) {
            afndeList.add(parser.parse(rule));
        }
        end = System.nanoTime();
        log.debug("0. Rules parsed into AFNDEs. Time: {} ms", (end - start) / 1_000_000);

        start = System.nanoTime();
        AFNDE masterAFNDE = afndeGenerator.buildMasterScanner(afndeList);
        end = System.nanoTime();
        log.debug("1. Master AFNDE built successfully. Time: {} ms", (end - start) / 1_000_000);

        start = System.nanoTime();
        AFNDEtoAFND eConverter = new AFNDEtoAFND();
        AFND masterAFND = eConverter.convert(masterAFNDE);
        end = System.nanoTime();
        log.debug("2. AFNDE Converted to AFND. Time: {} ms", (end - start) / 1_000_000);

        start = System.nanoTime();
        AFNDtoAFD afdConverter = new AFNDtoAFD();
        AFD masterAFD = afdConverter.convert(masterAFND);
        end = System.nanoTime();
        log.debug("3. AFND converted to strictly deterministic AFD. Time: {} ms", (end - start) / 1_000_000);

        start = System.nanoTime();
        AFDMinimizer minimizer = new AFDMinimizer();
        this.masterAutomaton = minimizer.minimize(masterAFD);
        end = System.nanoTime();
        log.debug("4. AFD Minimized successfully. Time: {} ms", (end - start) / 1_000_000);

        log.info("Total pipeline time: {} ms\n", (System.nanoTime() - totalStart) / 1_000_000);
    }

    public AFD getMasterAutomaton() {
        return masterAutomaton;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public String scan(String input) {
        log.info("Scanning input...");
        
        char[] chars = input.toCharArray();
        int inputLength = chars.length;
        
        int currentIndex = 0;
        int currentLine = 1;
        int currentCol = 1;
        
        boolean hasErrors = false;
        List<PendingSymbol> pendingSymbols = new ArrayList<>();

        while (currentIndex < inputLength) {
            int lastAcceptingIndex = -1;
            String lastAcceptingToken = null;

            State currentState = masterAutomaton.getStartState();

            for (int i = currentIndex; i < inputLength; i++) {
                currentState = masterAutomaton.getNextState(currentState, chars[i]);

                if (currentState == null) {
                    break; 
                }

                if (currentState.isFinal()) {
                    lastAcceptingIndex = i;
                    lastAcceptingToken = currentState.getAcceptedToken();
                }
            }

            if (lastAcceptingIndex != -1) {
                int length = lastAcceptingIndex - currentIndex + 1;
                String lexeme = new String(chars, currentIndex, length);

                boolean isSkipToken = skipTokens.contains(lastAcceptingToken);

                if (log.isDebugEnabled()) {
                    String displayLexeme = lexeme.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
                    log.debug("[{}, {}]: Found Token: <{}, '{}'>", currentLine, currentCol, lastAcceptingToken, displayLexeme);
                }

                if (!isSkipToken && dynamicTokens.contains(lastAcceptingToken)) {
                    pendingSymbols.add(new PendingSymbol(lexeme, lastAcceptingToken, currentLine, currentCol));
                }

                for (int i = currentIndex; i <= lastAcceptingIndex; i++) {
                    if (chars[i] == '\n') {
                        currentLine++;
                        currentCol = 1;
                    } else {
                        currentCol++;
                    }
                }
                
                currentIndex = lastAcceptingIndex + 1;
                
            } else {
                hasErrors = true; 
                
                int errorStart = currentIndex;
                int errorLine = currentLine;
                int errorCol = currentCol;
                
                while (currentIndex < inputLength && !Character.isWhitespace(chars[currentIndex])) {
                    if (chars[currentIndex] == '\n') {
                        currentLine++;
                        currentCol = 1;
                    } else {
                        currentCol++;
                    }
                    currentIndex++;
                }
                
                String malformedChunk = new String(chars, errorStart, currentIndex - errorStart);
                log.error("[{}, {}]: Lexical Error: Invalid sequence '{}'", errorLine, errorCol, malformedChunk);
            }
        }

        if (hasErrors) {
            log.warn("Scanning finished with errors. Symbol table was NOT populated.");
            symbolTable.clearTable();
            return "Scanning failed with lexical errors.";
        } else {
            for (PendingSymbol ps : pendingSymbols) {
                symbolTable.insert(ps.tokenType, ps.lexeme, ps.line, ps.col);
            }
            log.info("Scanning complete. Symbol table populated successfully.");
            return "Scanning complete.";
        }
    }
}