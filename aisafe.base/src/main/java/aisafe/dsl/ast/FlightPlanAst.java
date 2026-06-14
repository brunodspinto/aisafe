package aisafe.dsl.ast;

import java.util.List;

public record FlightPlanAst(String identifier, FlightType flightType, List<LegAst> legs, SourcePosition position) {

    public FlightPlanAst(final String identifier, final FlightType flightType, final List<LegAst> legs) {
        this(identifier, flightType, legs, SourcePosition.unknown());
    }
}
