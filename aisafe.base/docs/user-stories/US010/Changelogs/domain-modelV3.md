# Domain Model Changelog

## V3 — Sprint 2 (7 May 2026)

The following changes were made to the domain model during Sprint 2 implementation.

---

### 1. `Airport` aggregate redesigned — coordinates and codes promoted to Value Objects

**Before:**
```
package "Airport Aggregate" {
    class Airport {
        name
        town
        country
        altitude
        latitude
        longitude
    }
}
Airport "1" --> "1" IATACode : "identified by"
Airport "1" --> "1" ICAOCode : "identified by"
```

**After:**
```
package "Airport Aggregate" {
    class Airport {
        name
        town
        country
        altitude
    }
    class AirportIATACode <<identity, value object>> {
        iataCode
    }
    class AirportICAOCode <<value object>> {
        icaoCode
    }
    class GeoCoordinate <<value object>> {
        latitude
        longitude
    }
}
Airport *-- AirportIATACode : "identified by"
Airport *-- AirportICAOCode : "has"
Airport *-- GeoCoordinate : "located at"
```

**Reason — `AirportIATACode` and `AirportICAOCode`:**
Airport codes follow different formats from company codes. Airport IATA codes have exactly 3 uppercase letters (e.g. LIS, OPO) while company IATA codes have 2 letters (e.g. TP, BA). Airport ICAO codes have exactly 4 uppercase letters (e.g. LPPT, LPPR) while company ICAO codes have 2-3 letters. Therefore separate Value Objects `AirportIATACode` and `AirportICAOCode` were created, distinct from the existing `IATACode` and `ICAOCode` used by `AirTransportCompany`. This is consistent with section 3.4.2 of the project document which specifies airport codes as 3-letter (IATA) and 4-letter (ICAO).

**Reason — `GeoCoordinate`:**
The project document (section 3.2) specifies "Coordinates" as a single concept for an airport location. Storing `latitude` and `longitude` as plain `double` fields directly in `Airport` duplicates validation logic and misses an opportunity to express a meaningful domain concept. `GeoCoordinate` encapsulates latitude (between -90 and 90) and longitude (between -180 and 180) with centralised validation. This Value Object can also be reused by other domain entities that require a geographic point, such as `Node` (used in flight segments).

---

## Summary of changes

| Change | Type |
|--------|------|
| `latitude` and `longitude` extracted from `Airport` into `GeoCoordinate` value object | Redesign |
| `AirportIATACode` created as airport-specific identity value object (3 letters) | Enhancement |
| `AirportICAOCode` created as airport-specific value object (4 letters) | Enhancement |
| `Airport` no longer references global `IATACode` and `ICAOCode` | Correction |
