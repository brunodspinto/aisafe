// Invalid (SEMANTIC): end coordinate of segment 1 does not match start coordinate of segment 2
// Rule: consecutive segments in a leg must be spatially connected
// Expected: REJECTED with message "end of segment 1 ... does not match start of segment 2"
FLIGHT TP123 TYPE REGULAR {
  LEG {
    DEPARTURE: 2026-06-01 10:00;
    ARRIVAL: 2026-06-01 11:00;
    ROUTE: OPO -> LIS;
    SEGMENT {
      START: (+41.15, -8.61);
      END: (+40.00, -8.00);
      ALTITUDE: 10000 M WIDTH: 2000 M;
      WIND: (180, 12 M/S);
    }
    SEGMENT {
      START: (+38.72, -9.14);
      END: (+38.00, -9.50);
      ALTITUDE: 10000 M WIDTH: 2000 M;
      WIND: (180, 12 M/S);
    }
    FUEL: 5300 KG;
  }
}
