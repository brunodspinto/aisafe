package aisafe.dsl.ast;

public record FuelAst(double amount, String unit, SourcePosition position) {

    public FuelAst(final double amount, final String unit) {
        this(amount, unit, SourcePosition.unknown());
    }
}
