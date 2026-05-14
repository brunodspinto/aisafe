## 3. Aggregate Catalog
Justification of the main Aggregates identified for the *AISafe* domain:
---
### 3.1. Aggregate: Air Transport Company
* **Aggregate Root:** `AirTransportCompany`
* **Value Objects:** `IATACode`, `ICAOCode`
* **Scenario:** Register a new air transport company in the system (US060).
* **Invariant (Business Rule):** An air transport company must have a valid name, and its IATA code (2 letters) and ICAO code (2-3 letters) must be strictly formatted and globally unique.
* **Justification:** Acts as the root entity representing an airline, defined entirely by primitive attributes. It ensures loose coupling by holding only external references to the `User`, `Aircraft`, and `FlightRoute` aggregates, preventing heavy database memory loads. `IATACode` and `ICAOCode` are value objects owned by this aggregate and referenced by other aggregates (`FlightRoute`, `Flight`, `FlightPlan`).
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_1/aggregate-3_1.puml)*
  * Note: This diagram also serves as the standard "Register X" architectural flow for other simple catalog aggregates in the system (such as Maker, Aircraft Model, and Engine Model)
---
### 3.2. Aggregate: User
* **Aggregate Root:** `User`
* **Value Objects:** `Email`, `SecurityClearance`
* **Enums:** `RoleType`, `SecurityLevel`
* **Scenario:** Register a new user in the system (US031).
* **Invariant (Business Rule):** A user must have a valid email address, an active security clearance, and at least one assigned role. Roles are managed via the EAPLI `SystemUser` framework.
* **Justification:** Acts as the root entity managing user identity and access. It encapsulates the `Email` and `SecurityClearance` Value Objects, guaranteeing their internal validity and consistency upon creation. Role assignment is delegated to the EAPLI `SystemUser` to avoid reimplementing authentication infrastructure. `SecurityClearance` enforces a controlled security level via the `SecurityLevel` enum and an expiration date.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_2/aggregate-3_2.puml)*
---
### 3.3. Aggregate: Maker
* **Aggregate Root:** `Maker`
* **Scenario:** Register a new aircraft or engine manufacturer in the system catalog (Backoffice Configuration).
* **Invariant (Business Rule):** A manufacturer must have a valid and unique name to correctly identify the maker of aircraft and engine models.
* **Justification:** Acts as a standalone catalog root entity defined entirely by primitive attributes. It ensures internal consistency and serves strictly as an independent external reference for the `AircraftModel` and `EngineModel` aggregates, ensuring low coupling.
* **Sequence Diagram:**
  * This aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company). A repetitive diagram is omitted.
---
### 3.4. Aggregate: Engine Model
* **Aggregate Root:** `EngineModel`
* **Enums:** `EngineType`
* **Scenario:** Register a new aircraft engine model (US056).
* **Invariant (Business Rule):** The combination of the engine model's name and its manufacturer (`Maker`) must be unique.
* **Justification:** Acts as a standalone catalog root entity characterized by primitive data types and an `EngineType` enum. It guarantees internal consistency and ensures low coupling by relying strictly on an independent external reference to the `Maker` aggregate to enforce its uniqueness rule.
* **Sequence Diagram:**
  * This aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company). A repetitive diagram is omitted.
---
### 3.5. Aggregate: Aircraft Model
* **Aggregate Root:** `AircraftModel`
* **Enums:** `AircraftType`
* **Scenario:** Create a new aircraft model to be added to the catalog (US055).
* **Invariant (Business Rule):** An aircraft model must have a unique combination of model name and manufacturer, and it must have at least one certified engine model associated with it. All engine models certified for the same aircraft model must be of the same `EngineType`.
* **Justification:** Acts as a catalog root entity characterized by primitive attributes and an `AircraftType` enum. It maintains internal consistency by validating its aerodynamic data upon creation, and ensures low coupling by holding only external references to its `Maker` and the certified `EngineModel` aggregates.
* **Sequence Diagram:**
  * This aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company). A repetitive diagram is omitted.
---
### 3.6. Aggregate: Aircraft
* **Aggregate Root:** `Aircraft`
* **Value Objects:** `CabinConfiguration`
* **Enums:** `OperationalStatus`
* **Scenario:** Add an aircraft to the fleet (US070).
* **Invariant (Business Rule):** The total number of seats configured in the cabin must not exceed the maximum capacity defined by the associated `AircraftModel`. For `CARGO` aircraft, `CabinConfiguration` must be absent (`null`). For `PASSENGER` and `MIXED` aircraft, `CabinConfiguration` is mandatory and must have at least one seat.
* **Justification:** Acts as the root entity representing a physical aircraft. It encapsulates the `CabinConfiguration` Value Object and ensures internal consistency by validating its capacity against the aircraft type. It maintains low coupling by holding only an external reference to the `AircraftModel` aggregate.
* **Sequence Diagram:**
  * This aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company). A repetitive diagram is omitted.
---
### 3.7. Aggregate: Air Control Area
* **Aggregate Root:** `AirControlArea`
* **Value Objects:** `GeoBoundary`
* **Scenario:** Register an air control area (US050).
* **Invariant (Business Rule):** The geographic boundaries of the area must be valid.
* **Justification:** Acts as the root entity representing a designated airspace. It encapsulates the `GeoBoundary` Value Object and guarantees internal consistency by strictly validating that the coordinate boundaries form a logically correct geographical space upon creation.
* **Sequence Diagram:**
  * This aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company). A repetitive diagram is omitted.
---
### 3.8. Aggregate: Airport
* **Aggregate Root:** `Airport`
* **Value Objects:** `AirportIATACode`, `AirportICAOCode`, `GeoCoordinate`
* **Scenario:** Register an airport in a given air control area (US052).
* **Invariant (Business Rule):** An airport must have globally unique IATA (3 letters) and ICAO (4 letters) codes. Its geographic coordinates must be valid. Airport codes follow a different format from company codes — `AirportIATACode` and `AirportICAOCode` are distinct value objects from `IATACode` and `ICAOCode`.
* **Justification:** Acts as an independent root entity representing a physical location. It encapsulates `AirportIATACode`, `AirportICAOCode` and `GeoCoordinate` as Value Objects. It ensures low coupling by holding only an external reference to the `AirControlArea` aggregate.
* **Sequence Diagram:**
  * This aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company). A repetitive diagram is omitted.
---
### 3.9. Aggregate: WeatherData
* **Aggregate Root:** `WeatherData`
* **Value Objects:** `WeatherSource`
* **Scenario:** Register weather data or import bulk weather data for a specific air control area (US041 / US042).
* **Invariant (Business Rule):** Weather data must be recorded for a valid and existing air control area, and its origin must be tracked by a known weather source.
* **Justification:** Acts as the root entity encapsulating environmental readings and the `WeatherSource` Value Object. It guarantees internal consistency and ensures low coupling by relying strictly on an external reference to the `AirControlArea` aggregate.
* **Sequence Diagram:**
  * This aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company). A repetitive diagram is omitted.
---
### 3.10. Aggregate: Simulation
* **Aggregate Root:** `Simulation`
* **Local Entities:** `SimulationReport`
* **Value Objects:** `SafetyViolation`
* **Enums:** `SimulationStatus`
* **Scenario:** Simulate flights in a given area and generate a validation report (US109/US111).
* **Invariant (Business Rule):** A simulation must successfully produce a report that accurately records any safety violations detected between the included flights.
* **Justification:** Acts as the root entity managing simulation execution. It ensures internal consistency by encapsulating the `SimulationReport` entity and `SafetyViolation` value objects. `SafetyViolation` is a value object — it is an immutable record of an event with no independent lifecycle. It maintains low coupling by holding external references strictly to the `Flight` aggregate and the `AirControlArea`.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_10/aggregate-3_10.puml)*
---
### 3.11. Aggregate: Flight Route
* **Aggregate Root:** `FlightRoute`
* **Enums:** `FlightRouteStatus`
* **Scenario:** Create a flight route for a company (US073).
* **Invariant (Business Rule):** The route must originate and end at different airports, and its name must follow the required format (e.g., TP123) and be unique. The route connects exactly two airports identified by `IATACode`.
* **Justification:** Acts as an independent root entity defined by simple attributes. It guarantees internal consistency and ensures strict low coupling by referencing airports via `IATACode` value objects (owned by `AirTransportCompany` aggregate), entirely avoiding heavy dependencies on the `Airport` aggregate.
* **Sequence Diagram:**
  * This aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company). A repetitive diagram is omitted.
---
### 3.12. Aggregate: Flight
* **Aggregate Root:** `Flight`
* **Local Entities:** `FlightPlan`
* **Value Objects:** `FlightSegment`, `Node`
* **Enums:** `FlightType`, `FlightPlanStatus`
* **Scenario:** Instantiate/schedule a flight for a specific flight route and register/validate its flight plans (US080).
* **Invariant (Business Rule):** A flight must belong to a valid flight route, be properly typed (Regular or Charter), and maintain valid operational resources (Aircraft, Pilot) and its geographical flight plan. `FlightPlan` is a local entity — it has its own lifecycle (DRAFT → VALIDATED → APPROVED/REJECTED) and can be reviewed by a `User`.
* **Justification:** Acts as the root entity representing an operational flight. It encapsulates `FlightPlan` as a local entity, and `FlightSegment` and `Node` as value objects. It guarantees internal consistency and ensures low coupling by holding external references to `Aircraft`, `User`, and `FlightRoute`, and using `IATACode` for geographic locations.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_12/aggregate-3_12.puml)*
---
### 3.13. Aggregate: Collaborator
* **Aggregate Root:** `Collaborator`
* **Enums:** `CollaboratorStatus`
* **Scenario:** Register a customer's collaborator (US061).
* **Invariant (Business Rule):** A collaborator must be associated with exactly one customer — either an `AirTransportCompany` or an `AirControlArea` — and must reference a valid system `User`. A collaborator has a status (ACTIVE or DISABLED) that controls system access (US062, US064).
* **Justification:** Acts as the root entity representing the business relationship between a system user and a customer. It maintains low coupling by holding external references to `User`, `AirTransportCompany` and `AirControlArea`. The `CollaboratorStatus` enum enforces the lifecycle invariant required by US062 and US064.
* **Sequence Diagram:**
  * This aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company). A repetitive diagram is omitted.
---
