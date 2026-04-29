package core.lexer.models.atomic;

import lombok.Getter;

@Getter
public class Transition {
    private final State source;
    private final State target;
    private final Symbol symbol;

    public Transition(State source, State target, Symbol symbol) {
        this.source = source;
        this.target = target;
        this.symbol = symbol;
    }
}
