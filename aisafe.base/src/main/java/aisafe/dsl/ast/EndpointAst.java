package aisafe.dsl.ast;

public record EndpointAst(String date, String time, SourcePosition position) {

    public EndpointAst(final String date, final String time) {
        this(date, time, SourcePosition.unknown());
    }
}
