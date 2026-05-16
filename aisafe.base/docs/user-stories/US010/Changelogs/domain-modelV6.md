## V6 — Sprint 2 (16 May 2026)

The following changes were made after comparing the domain model against the project requirements document (Project_Requirements_V2a.pdf) and identifying semantic inconsistencies.

---

### 1. `FlightRoute --> IATACode` corrected to `FlightRoute --> AirportIATACode`

**Before:**
```
FlightRoute "1" --> "2" IATACode : "connects"
```

**After:**
```
FlightRoute "1" --> "2" AirportIATACode : "connects"
```

**Reason:**
A flight route connects two airports, not two companies. `IATACode` (2-letter code) identifies an `AirTransportCompany`. `AirportIATACode` (3-letter code) identifies an `Airport`. Using `IATACode` to reference airports is semantically wrong — a route between LIS and OPO must reference `AirportIATACode`, not the 2-letter company designator.

---

### 2. `Flight --> IATACode` corrected to `Flight --> AirportIATACode`

**Before:**
```
Flight "1" --> "2" IATACode : "connects"
```

**After:**
```
Flight "1" --> "2" AirportIATACode : "connects"
```

**Reason:**
Same reasoning as change 1. A flight connects two airports identified by their 3-letter IATA airport codes. The reference to `IATACode` (company code) was inconsistent with the definitions in section 3.2 of the requirements, which states that airports are identified by their IATA airport code.

---

### 3. `FlightPlan --> IATACode : "alternate airport"` corrected to `FlightPlan --> AirportIATACode`

**Before:**
```
FlightPlan "1" --> "1" IATACode : "alternate airport"
```

**After:**
```
FlightPlan "1" --> "1" AirportIATACode : "alternate airport"
```

**Reason:**
An alternate airport is an airport, identified by a 3-letter IATA airport code. Referencing `IATACode` (2-letter company code) was incorrect. The alternate airport is selected from registered airports in the system, all of which are identified by `AirportIATACode`.

---

### 4. `FlightExecutionStatus` value object added to `Simulation Aggregate`

**Before:**
`SimulationReport` had no way to record individual flight outcomes.

**After:**
```
class FlightExecutionStatus <<value object>> {
    flightDesignator
    status
}

SimulationReport "1" *-- "0..*" FlightExecutionStatus : "details"
```

**Reason:**
US109 requires the simulation report to include "individual execution statuses" per flight, in addition to the aggregate counters (`totalFlights`, `passed`). `SimulationReport` previously only stored global results, with no record of what happened to each individual flight. `FlightExecutionStatus` is an immutable record that captures the outcome of a single flight (e.g. COMPLETED, TERMINATED, FAILED) identified by its `flightDesignator`. It has no identity outside the report, so a value object is the correct choice.

---

## Summary of changes

| Change | Type |
|--------|------|
| `FlightRoute "1" --> "2" IATACode` corrected to `AirportIATACode` | Correction |
| `Flight "1" --> "2" IATACode` corrected to `AirportIATACode` | Correction |
| `FlightPlan "1" --> "1" IATACode : "alternate airport"` corrected to `AirportIATACode` | Correction |
| `FlightExecutionStatus <<value object>>` added to `Simulation Aggregate` | Enhancement |
| `SimulationReport "1" *-- "0..*" FlightExecutionStatus : "details"` added | Enhancement |
