package models.atomic;

public class Transition {
    private final String symbol;
    private final State target;

    public Transition(String symbol, State target) {
        this.symbol = symbol;
        this.target = target;
    }

    public String getSymbol() { return symbol; }
    public State getTarget() { return target; }
}