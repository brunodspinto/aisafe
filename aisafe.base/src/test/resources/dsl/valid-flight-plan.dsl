// Valid: single leg, REGULAR type, IATA codes
// Expected: ACCEPTED by lexer, parser and semantic validator
FLIGHT TP123 TYPE REGULAR {
  LEG {
    DEPARTURE: OPO 2026-06-01 10:00;
    ARRIVAL: LIS 2026-06-01 10:45;
    ROUTE: OPO -> LIS;
    SEGMENT {
      START: (+41.15, -8.61);
      END: (+38.72, -9.14);
      ALTITUDE: 10000 M WIDTH: 2000 M;
      WIND: (180, 12 M/S);
    }
    FUEL: 5300 KG;
  }
}
