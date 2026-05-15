// Valid: all keywords in lowercase (grammar is case-insensitive)
// Expected: ACCEPTED - keywords like 'flight', 'leg', 'departure', etc. are all valid
flight TP501 type regular {
  leg {
    departure: OPO 2026-11-01 06:00;
    arrival: LIS 2026-11-01 06:45;
    route: OPO -> LIS;
    segment {
      start: (+41.15, -8.61);
      end: (+38.72, -9.14);
      altitude: 8000 m width: 1500 m;
      wind: (90, 10 m/s);
    }
    fuel: 3500 kg;
  }
}
