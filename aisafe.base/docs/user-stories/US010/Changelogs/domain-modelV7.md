# Domain Model Changelog

## V7 — Sprint 3 (29 May 2026)

The following changes were made to support the implementation of US073 (Create a Flight Route) in Sprint 3.

---

### 1. `RouteName` value object added to `FlightRoute Aggregate`

**Before:**
```
package "FlightRoute Aggregate" {
    class FlightRoute <<entity, aggregate root>> {
        name
        activeUntil
    }
}
```

**After:**
```
package "FlightRoute Aggregate" {
    class FlightRoute <<entity, aggregate root>> {
        activeUntil
    }
    class RouteName <<value object>> {
        name
    }
}
FlightRoute "1" *-- "1" RouteName : "identified by"
```

**Reason:** The route name is the natural business identity of the `FlightRoute` aggregate — it follows a defined format (`[A-Z]{2}[0-9]{1,4}`) and must be unique in the system. According to DDD tactical patterns, a business identity should be represented as a Value Object (CO3 criterion: "business identity as VOs"). The `RouteName` value object encapsulates the format validation and immutability, consistent with how other business identities are modelled in the domain (e.g. `AirportIATACode`, `MecanographicNumber`, `RegistrationNumber`). Using a plain `String` attribute would leave the format validation scattered across the system.

---

## Summary of changes

| Change | Type |
|--------|------|
| `RouteName <<value object>>` added to `FlightRoute Aggregate` | Enhancement |
| `FlightRoute "1" *-- "1" RouteName : "identified by"` added | Enhancement |
| `name` attribute removed from `FlightRoute` (moved to `RouteName`) | Correction |

---

