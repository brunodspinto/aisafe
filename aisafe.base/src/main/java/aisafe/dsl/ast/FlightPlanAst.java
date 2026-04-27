package aisafe.dsl.ast;

import java.util.List;

public record FlightPlanAst(String identifier, FlightType flightType, List<LegAst> legs) {
}
