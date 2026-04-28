# Domain Model Changelog

## V2 — Sprint 1 Review (April 2026)

The following changes were made to the domain model based on feedback received during the Sprint 1 presentation.

---

### 1. Removed `Role` as intermediate value object

**Before:** `User --> Role --> RoleType`

**After:** `User --> RoleType` directly with multiplicity `1..*`

**Reason:** The `Role` value object was unnecessary as an intermediate layer. Linking `User` directly to `RoleType` simplifies the model. The multiplicity `1..*` also supports the future requirement stated in the specification that a user may have multiple roles.

---

### 2. Removed `EmailDomain` value object

**Before:** `User *-- Email *-- EmailDomain`

**After:** `User *-- Email`

**Reason:** The `Email` value object already validates the domain internally as part of its address. A separate `EmailDomain` value object adds unnecessary complexity without additional value.

---

### 3. `CabinConfiguration` multiplicity changed to `0..*`

**Before:** `Aircraft "1" *-- "1" CabinConfiguration`

**After:** `Aircraft "1" *-- "0..*" CabinConfiguration`

**Reason:** An aircraft can be of type CARGO, PASSENGER or MIXED. A cargo aircraft has no cabin configuration. The `0..*` multiplicity correctly models this — a passenger aircraft has one configuration, a cargo aircraft has none.

---

### 4. `SafetyViolation` changed from entity to value object

**Before:** `SafetyViolation <<entity>>`

**After:** `SafetyViolation <<value object>>`

**Reason:** A safety violation is an immutable record of an event. Once recorded, it never changes state. It has no independent lifecycle — it exists only within the `SimulationReport`. These characteristics define a value object.

---

### 5. `description` and `flightDesignator` added to `SafetyViolation`

**Reason:** The `description` field allows the system to provide a human-readable explanation of the violation type. The `flightDesignator` field records which flight was involved in the violation, providing essential context for the simulation report without requiring a direct reference to the `Flight` aggregate.

---

### 6. `FlightId` removed

**Before:** `SafetyViolation --> FlightId`

**After:** `flightDesignator` added as a simple attribute in `SafetyViolation`

**Reason:** Since `SafetyViolation` is now a value object and the `Simulation` already references the involved flights directly, a separate `FlightId` value object is redundant. The `flightDesignator` attribute provides sufficient identification for reporting purposes.

---

### 7. `FlightPlan` changed from value object to entity

**Before:** `FlightPlan <<value object>>`

**After:** `FlightPlan <<entity>>`

**Reason:** A `FlightPlan` has a lifecycle — it evolves through states (DRAFT → VALIDATED → APPROVED / REJECTED). A flight can have multiple plans over time, each requiring individual identity. These characteristics define an entity, not a value object.

---

### 8. `FlightPlan` enriched with additional attributes

**Added attributes:** `passengerCount`, `crewCount`, `totalWeight`, `estimatedDuration`

**Reason:** A flight plan must describe the complete operational state of the flight, not just fuel and cargo. These attributes are necessary for accurate flight simulation and safety validation.

---

### 9. `FlightPlanStatus` moved from `Flight` to `FlightPlan`

**Before:** `Flight --> FlightPlanStatus`

**After:** `FlightPlan --> FlightPlanStatus`

**Reason:** The status belongs to the plan, not the flight. Each `FlightPlan` has its own independent status. Moving it to `FlightPlan` correctly reflects the ownership of this lifecycle state.

---

### 10. `FlightPlan` linked to `User` via `reviewed by`

**Added:** `FlightPlan "1" --> "0..1" User : "reviewed by"`

**Reason:** The `FlightPlanStatus` is changed by a `Flight Control Operator` (a `User` with role `FLIGHT_CONTROL_OPERATOR`). This association makes explicit who is responsible for approving or rejecting a flight plan. The `0..1` multiplicity reflects that a plan in DRAFT state has not yet been reviewed.

---

### 11. `IATACode` and `ICAOCode` created as shared value objects

**Before:** `IATACode` and `ICAOCode` were simple string attributes inside `Airport` and `AirTransportCompany`

**After:** Declared as independent value objects outside any aggregate boundary

**Reason:** IATA and ICAO codes have format validation rules (IATA = 3 letters, ICAO = 2-4 letters). Promoting them to value objects encapsulates this validation logic. As immutable value objects they can be freely shared across aggregates.

---

### 12. `AirportCode` removed and replaced by `IATACode`

**Before:** `AirportCode <<value object>>` used in `FlightRoute` and `Flight`

**After:** `IATACode` used directly in `FlightRoute`, `Flight` and `FlightPlan`

**Reason:** Now that `IATACode` exists as a proper value object, the generic `AirportCode` is redundant. Using `IATACode` directly is more explicit and consistent with the rest of the model.

---

### 13. `FlightRoute` and `Flight` connections to `IATACode` simplified

**Before:**
```
FlightRoute "1" --> "1" IATACode : "originates at"
FlightRoute "1" --> "1" IATACode : "ends at"
Flight "1" --> "1" IATACode : "departs from"
Flight "1" --> "1" IATACode : "arrives at"
```

**After:**
```
FlightRoute "1" --> "2" IATACode : "connects"
Flight "1" --> "2" IATACode : "connects"
```

**Reason:** A route and a flight always connect exactly two airports. Using multiplicity `2` with a single association is cleaner and avoids redundant arrows. The distinction between origin and destination is handled at the implementation level.

---

### 14. `FlightSegment` node connections simplified

**Before:**
```
FlightSegment "1" *-- "1" Node : "starts at"
FlightSegment "1" *-- "1" Node : "ends at"
```

**After:**
```
FlightSegment "1" *-- "2" Node : "has"
```

**Reason:** A segment always has exactly two nodes — a start and an end. Using a single association with multiplicity `2` is cleaner and avoids two redundant arrows. The order of nodes determines which is the start and which is the end.

---

## Summary of changes

| Change | Type |
|--------|------|
| Removed `Role` intermediary | Simplification |
| Removed `EmailDomain` | Simplification |
| `CabinConfiguration` to `0..*` | Correction |
| `SafetyViolation` to value object | Redesign |
| Added `description` and `flightDesignator` to `SafetyViolation` | Enhancement |
| Removed `FlightId` | Simplification |
| `FlightPlan` to entity | Redesign |
| `FlightPlan` new attributes | Enhancement |
| `FlightPlanStatus` moved to `FlightPlan` | Correction |
| `FlightPlan reviewed by User` | Enhancement |
| `IATACode` and `ICAOCode` as value objects | Enhancement |
| `AirportCode` removed | Simplification |
| `FlightRoute` and `Flight` connections simplified | Simplification |
| `FlightSegment` nodes simplified | Simplification |