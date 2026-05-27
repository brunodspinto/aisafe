// Valid: altitude in FT, width in KM, wind speed in KNOT
// Expected: ACCEPTED
FLIGHT TP800 TYPE REGULAR {
  LEG {
    DEPARTURE: 2026-07-04 08:00;
    ARRIVAL: 2026-07-04 09:30;
    ROUTE: OPO -> MAD;
    SEGMENT {
      START: (+41.15, -8.61);
      END: (+40.49, -3.56);
      ALTITUDE: 35000 FT WIDTH: 5 KM;
      WIND: (270, 30 KNOT);
    }
    FUEL: 6000 KG;
  }
}
