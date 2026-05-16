// Invalid (syntactic): missing semicolon after DEPARTURE
// Expected: REJECTED at line 4 - parser expects ';' but finds 'ARRIVAL'
FLIGHT TP123 TYPE REGULAR {
  LEG {
    DEPARTURE: OPO 2026-06-01 10:00
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
