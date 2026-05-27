// Invalid (SEMANTIC): segment start and end coordinates are identical
// Rule: start and end coordinates of a segment must be different
// Expected: REJECTED with message "start and end coordinates must be different"
FLIGHT TP123 TYPE REGULAR {
  LEG {
    DEPARTURE: 2026-06-01 10:00;
    ARRIVAL: 2026-06-01 10:45;
    ROUTE: OPO -> LIS;
    SEGMENT {
      START: (+41.15, -8.61);
      END: (+41.15, -8.61);
      ALTITUDE: 10000 M WIDTH: 2000 M;
      WIND: (180, 12 M/S);
    }
    FUEL: 5300 KG;
  }
}
