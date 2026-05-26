// Invalid (SEMANTIC): fuel quantity is negative
// Rule: fuel must be strictly positive (> 0)
// Syntax is correct; error is detected in semantic validation phase
// Expected: REJECTED with message "fuel quantity must be strictly positive"
FLIGHT TP123 TYPE REGULAR {
  LEG {
    DEPARTURE: 2026-06-01 10:00;
    ARRIVAL: 2026-06-01 10:45;
    ROUTE: OPO -> LIS;
    SEGMENT {
      START: (+41.15, -8.61);
      END: (+38.72, -9.14);
      ALTITUDE: 10000 M WIDTH: 2000 M;
      WIND: (180, 12 M/S);
    }
    FUEL: -5300 KG;
  }
}
