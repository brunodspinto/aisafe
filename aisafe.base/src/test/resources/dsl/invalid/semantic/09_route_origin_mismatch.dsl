// Invalid (SEMANTIC): route mismatch between consecutive legs
// Leg 1 route ends at LIS, but leg 2 route starts at FAO (should be LIS)
// Rule: route destination of leg N must match route origin of leg N+1
// Expected: REJECTED with message "Leg 1 arrival airport (LIS) must match leg 2 departure airport (FAO)"
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
    FUEL: 5300 KG;
  }
  LEG {
    DEPARTURE: 2026-06-01 12:00;
    ARRIVAL: 2026-06-01 13:00;
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
