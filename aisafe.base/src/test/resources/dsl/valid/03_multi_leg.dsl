// Valid: 2 legs with correct sequence (OPO -> LIS -> FAO)
// Leg 1 arrives LIS, Leg 2 departs LIS: sequence coherent
// Leg 1 arrives 09:50, Leg 2 departs 11:00: time coherent
// Expected: ACCEPTED
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
    DEPARTURE: LIS 2026-08-01 11:00;
    ARRIVAL: FAO 2026-08-01 12:00;
    ROUTE: LIS -> FAO;
    SEGMENT {
      START: (+38.72, -9.14);
      END: (+37.01, -7.96);
      ALTITUDE: 7000 M WIDTH: 1600 M;
      WIND: (135, 10 M/S);
    }
    FUEL: 2500 KG;
  }
}
