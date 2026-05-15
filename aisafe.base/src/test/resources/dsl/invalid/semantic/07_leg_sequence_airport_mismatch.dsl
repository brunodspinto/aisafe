// Invalid (SEMANTIC): arrival airport of leg 1 does not match departure airport of leg 2
// Leg 1 arrives at LIS, but leg 2 departs from FAO (should be LIS)
// Rule: arrival airport of leg N must match departure airport of leg N+1
// Expected: REJECTED with message "Leg 1 arrival airport (LIS) must match leg 2 departure airport (FAO)"
FLIGHT TP200 TYPE REGULAR {
  LEG {
    DEPARTURE: OPO 2026-08-01 09:00;
    ARRIVAL: LIS 2026-08-01 09:50;
    ROUTE: OPO -> LIS;
    SEGMENT {
      START: (+41.15, -8.61);
      END: (+38.72, -9.14);
      ALTITUDE: 8000 M WIDTH: 1800 M;
      WIND: (90, 15 M/S);
    }
    FUEL: 3000 KG;
  }
  LEG {
    DEPARTURE: FAO 2026-08-01 11:00;
    ARRIVAL: MAD 2026-08-01 12:30;
    ROUTE: FAO -> MAD;
    SEGMENT {
      START: (+37.01, -7.96);
      END: (+40.49, -3.56);
      ALTITUDE: 9000 M WIDTH: 2000 M;
      WIND: (270, 20 M/S);
    }
    FUEL: 4000 KG;
  }
}
