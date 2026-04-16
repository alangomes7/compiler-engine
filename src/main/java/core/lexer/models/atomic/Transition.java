package core.lexer.models.atomic;

public class Transition {
    private final State source;
    private final State target;
    private final Symbol symbol;

    public Transition(State source, State target, Symbol symbol) {
        this.source = source;
        this.target = target;
        this.symbol = symbol;
    }

    public State getSource() { return source; }
    public State getTarget() { return target; }
    public Symbol getSymbol() { return symbol; }
}