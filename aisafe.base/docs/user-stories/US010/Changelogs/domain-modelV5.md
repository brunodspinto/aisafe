## V5 — Sprint 2 (14 May 2026)

The following changes were made after identifying inconsistencies between the domain model and the implementation, and after feedback from the project supervisor.

---

### 1. `CabinConfiguration` multiplicity corrected: `0..*` → `0..1`

**Before:**
```
Aircraft "1" *-- "0..*" CabinConfiguration : "has"
```

**After:**
```
Aircraft "1" *-- "0..1" CabinConfiguration : "has"
```

**Reason:**
The multiplicity `0..*` was incorrect on two counts:

1. **Implies a collection** — the implementation uses a single `@Embedded` field, not a collection. An aircraft has at most one cabin configuration.
2. **Carries no semantics about CARGO** — the `AircraftType` enum already distinguishes `CARGO` from `PASSENGER`/`MIXED`. The correct cardinality is `0..1`:
   - `CARGO` aircraft: 0 (absent — null)
   - `PASSENGER` / `MIXED` aircraft: 1 (present — required)

The multiplicity `0..1` captures this optionality while ruling out the nonsensical "multiple configurations" that `0..*` would imply.

---

### 2. `Aircraft` constructor enforces cabin presence by type

| Aircraft type | `cabinConfiguration` |
|---------------|----------------------|
| `CARGO`       | must be `null`       |
| `PASSENGER`   | must be non-null     |
| `MIXED`       | must be non-null     |

The `cabinConfiguration()` getter always returns `null` for `CARGO` aircraft, abstracting the JPA persistence detail (Hibernate 6 stores the embedded columns as `0` for cargo and reconstructs the object on load; the getter hides this from callers).

`CabinConfiguration` retains the invariant "at least one seat" — this is enforced for `PASSENGER`/`MIXED` aircraft and is never invoked for `CARGO`.

---

### 3. `SecurityLevel` enum added

**Before:**
```
class SecurityClearance <<value object>> {
    level
    expirationDate
}
```

**After:**
```
class SecurityClearance <<value object>> {
    expirationDate
}

Enum SecurityLevel {
    LOW
    GUARDED
    ELEVATED
    HIGH
    CRITICAL
}

SecurityClearance "1" --> "1" SecurityLevel : "at level"
```

**Reason:**
The `level` attribute was a free value in `SecurityClearance`. The implementation already used a `SecurityLevel` enum with controlled values. The model was updated to reflect this — domain constraints should be explicit in the model.

---

### 4. `SimulationStatus` linked to `Simulation`

**Before:**
`SimulationStatus` existed as an enum with no relation to any class.

**After:**
```
Simulation "1" --> "1" SimulationStatus : "has"
```

**Reason:**
An enum that is not linked to any class is meaningless in the domain model. The `Simulation` aggregate has a status lifecycle (PENDING → RUNNING → COMPLETED/FAILED) that must be expressed explicitly.

---

### 5. `status` attribute removed from `Simulation`

**Before:**
```
class Simulation <<entity, aggregate root>> {
    ...
    status
}
```

**After:**
```
class Simulation <<entity, aggregate root>> {
    timeRangeStart
    timeRangeEnd
    timeStep
    safetyThreshold
}
```

**Reason:**
The `status` attribute was redundant — it is now expressed via the `SimulationStatus` enum through the explicit relation added in change 4.

---

### 6. `Collaborator` aggregate enriched

**Before:**
```
package "Collaborator Aggregate" <<Rectangle>> {
    class Collaborator <<entity, aggregate root>> {
    }
}
```

**After:**
```
package "Collaborator Aggregate" <<Rectangle>> {
    class Collaborator <<entity, aggregate root>> {
        status
    }

    Enum CollaboratorStatus {
        ACTIVE
        DISABLED
    }
}

Collaborator "1" --> "1" CollaboratorStatus : "has"
```

**Reason:**
US062 states "it should not list disabled collaborators" and US064 states "disable a customer's collaborator so that he may not use the system anymore." An aggregate root must have explicit identity and lifecycle attributes. The empty class was a design gap.

---

### 7. `Collaborator "1" --> "1" User : "is a"` label corrected

**Before:**
```
Collaborator "1" --> "1" User : "is"
```

**After:**
```
Collaborator "1" --> "1" User : "is a"
```

**Reason:**
`"is a"` is the correct UML terminology for modelling identity delegation between aggregates.

---

### 8. `yearOfManufacture` added to `Aircraft`

**Reason:**
US072d requires "list my company's aircraft of a given age." Without this attribute it is impossible to calculate the age of an aircraft. Its absence was a functional regression.

---

### 9. `maxRange` added to `AircraftModel`

**Reason:**
The project specification (US055) states: "An aircraft model will have a type, maximum range and other flight characteristics." The attribute was missing from the model.

---

### 10. `registrationCountry` corrected to `registeredCountry`

**Reason:**
The implementation uses `registeredCountry`. The model had an inconsistent name.

---

### 11. `IATACode` and `ICAOCode` moved into `AirTransportCompany Aggregate`

**Before:**
```
class IATACode <<value object>> { code }
class ICAOCode <<value object>> { code }
' declared outside any aggregate
```

**After:**
```
package "AirTransportCompany Aggregate" <<Rectangle>> {
    class AirTransportCompany <<entity, aggregate root>> { name }
    class IATACode <<value object>> { code }
    class ICAOCode <<value object>> { code }
}
```

**Reason:**
Following feedback from the project supervisor, value objects do not need to be declared outside aggregates — they can be referenced by other aggregates even when owned by one. `AirTransportCompany` is the primary owner of these codes. `FlightRoute`, `Flight` and `FlightPlan` reference `IATACode` across aggregate boundaries, which is valid in DDD for immutable value objects.

---

### 12. `AirportIATACode` stereotype corrected: `<<identity, value object>>` → `<<value object>>`

**Reason:**
A class cannot simultaneously be `identity` and `value object` — these are contradictory stereotypes. `AirportIATACode` is a value object that serves as the identity of `Airport`, but the correct stereotype is `<<value object>>`. The `"identified by"` association label already conveys the identity role.

---

### 13. EAPLI note added to `User`

**Reason:**
The model shows `User "1" --> "1..*" RoleType : "has"` but in reality roles are managed by the EAPLI `SystemUser`. A note was added to make this integration explicit for readers of the model.

---

### 14. `Flight --> WeatherData` relation removed

**Before:**
```
Flight "0..1" --> "1" WeatherData : "uses"
```

**After:**
Relation removed.

**Reason:**
US082 states "add to **flight plan** of mine the weather data" — the relation belongs to `FlightPlan`, not `Flight`. Furthermore, `WeatherData` is already associated with `AirControlArea` (`WeatherData "1" --> "1" AirControlArea : "recorded for"`), and flight plans operate within an area. The direct `Flight --> WeatherData` relation was semantically incorrect and redundant.

---

## Summary of changes

| Change | Type |
|--------|------|
| `Aircraft *-- CabinConfiguration` multiplicity changed from `0..*` to `0..1` | Correction |
| `Aircraft` constructor validates cabin presence against `AircraftType` | Enhancement |
| `cabinConfiguration()` getter returns `null` for `CARGO` aircraft | Bug fix |
| `SecurityLevel` enum added; `SecurityClearance` linked via `"at level"` | Enhancement |
| `SimulationStatus` linked to `Simulation` via `"has"` relation | Correction |
| `status` attribute removed from `Simulation` (replaced by enum relation) | Correction |
| `Collaborator` enriched with `status` attribute and `CollaboratorStatus` enum | Enhancement |
| `Collaborator --> User` label corrected from `"is"` to `"is a"` | Correction |
| `yearOfManufacture` added to `Aircraft` | Enhancement |
| `maxRange` added to `AircraftModel` | Enhancement |
| `registrationCountry` corrected to `registeredCountry` | Correction |
| `IATACode` and `ICAOCode` moved into `AirTransportCompany Aggregate` | Correction |
| `AirportIATACode` stereotype corrected to `<<value object>>` | Correction |
| EAPLI note added to `User` aggregate | Enhancement |
| `Flight --> WeatherData` relation removed | Correction |
