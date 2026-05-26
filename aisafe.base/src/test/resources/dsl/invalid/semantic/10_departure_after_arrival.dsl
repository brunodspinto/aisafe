// Invalid (SEMANTIC): departure time is after arrival time within the same leg
// Rule: departure datetime must precede arrival datetime within each leg
// Expected: REJECTED with message "departure ... must be before arrival"
FLIGHT TP123 TYPE REGULAR {
  LEG {
    DEPARTURE: 2026-06-01 11:00;
    ARRIVAL: 2026-06-01 10:00;
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
