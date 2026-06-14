package aisafe.dsl.ast;

public record SourcePosition(int line, int column) {

    private static final SourcePosition UNKNOWN = new SourcePosition(0, 0);

    public static SourcePosition unknown() {
        return UNKNOWN;
    }
}
