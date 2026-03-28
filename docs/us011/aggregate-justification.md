

## 3. Aggregate Catalog

Justification of the main Aggregates identified for the *AISafe* domain:

---

### 3.1. Aggregate: Flight Plan

* **Aggregate Root:** `FlightPlan`
* **Local Entities:** `FlightSegment`
* **Value Objects:** `Node`
* **Scenario:** Create a flight plan for a route (US080) or import a flight plan from a DSL file (US081).
* **Invariant (Business Rule):** A flight plan must contain at least one segment. The start and end coordinates of each segment must be different, and the end node of a segment must match the start node of the next segment, ensuring route continuity. Additionally, its initial status must be set to "draft" until validated.
* **Justification:** The domain model defines that a `FlightPlan` is the root composed of multiple `FlightSegment` local entities, which in turn start and end at geographic points defined by `Node` value objects. The Aggregate Root (`FlightPlan`) ensures consistency across this internal structure, enforcing semantic validation rules (such as coordinate distinctness and route continuity). Furthermore, it manages the assignment of external references like `Aircraft` and `User` (Pilot), preventing invalid or disconnected flight plans from being persisted in the database.
* **Sequence Diagram:**
  *(To be added)*

---

### 3.2. Aggregate: User

* **Aggregate Root:** `User`
* **Value Objects:** `Email`, `EmailDomain`, `SecurityClearance`, `Role`
* **Scenario:** Register a new user in the system.
* **Invariant (Business Rule):** A user must have a valid email domain, an assigned role, and an associated security clearance.
* **Justification:** A `User` is identified by an `Email`, which belongs to an `EmailDomain`. The user also holds a `SecurityClearance` and is assigned a `Role` (typed by a specific `RoleType` enum). These Value Objects describe the identity and access permissions of a user. The Aggregate Root ensures that all these components are valid and consistent when creating or updating a user.
* **Sequence Diagram:**
  *(To be added)*

---

### 3.3. Aggregate: Maker

* **Aggregate Root:** `Maker`
* **Scenario:** Register a new aircraft or engine manufacturer in the system catalog (Backoffice Configuration).
* **Invariant (Business Rule):** A manufacturer must have a valid and unique name to correctly identify the maker of aircraft and engine models.
* **Justification:** The `Maker` aggregate functions as a standalone catalog entity within the backoffice domain. It does not encapsulate any local entities or custom value objects, as its state is defined by simple primitive attributes (such as name and country). As an Aggregate Root, it manages its own internal consistency upon creation and serves as an independent external reference for both the `AircraftModel` and `EngineModel` aggregates. This design ensures that manufacturers can be managed and persisted independently from the models they produce.
* **Sequence Diagram:**
  *(To be added)*

---

### 3.4. Aggregate: Engine Model

* **Aggregate Root:** `EngineModel`
* **Scenario:** Register a new aircraft engine model (US056).
* **Invariant (Business Rule):** The combination of the engine model's name and its manufacturer (`Maker`) must be unique.
* **Justification:** Just like the `AircraftModel`, the `EngineModel` acts as a standalone catalog entity within its own aggregate. It does not encapsulate other local entities or custom value objects, as its state is represented by primitive data types (such as thrust and TSFC) and classified by an `EngineType` enum. The Aggregate Root ensures its own internal consistency upon creation and enforces the business rule that its name, combined with the associated external `Maker` reference, must be unique, preventing duplicate catalog entries from being persisted in the database.
* **Sequence Diagram:**
  *(To be added)*

---


### 3.5. Aggregate: AircraftModel

* **Aggregate Root:** `AircraftModel`
* **Scenario:** Create a new aircraft model to be added to the catalog (US055).
* **Invariant (Business Rule):** An aircraft model must have a unique combination of model name and manufacturer, and it must have at least one certified engine model associated with it.
* **Justification:** The `AircraftModel` acts as a standalone catalog entity. In the domain model, it does not encapsulate other local entities or custom value objects (its state is characterized by primitive attributes and the `AircraftType` enum). It maintains its own internal consistency by validating its aerodynamic and capacity attributes. Furthermore, as an Aggregate Root, it enforces the business rule that it cannot be created or exist without at least one valid reference to a certified `EngineModel` (which belongs to a separate external aggregate), ensuring the catalog's integrity.
* **Sequence Diagram:**
  *(To be added)*
---

### 3.6. Aggregate: Aircraft

* **Aggregate Root:** `Aircraft`
* **Value Objects:** `CabinConfiguration`
* **Scenario:** Add an aircraft to the fleet (US070).
* **Invariant (Business Rule):** The total number of seats configured in the cabin must not exceed the maximum capacity defined by the associated `AircraftModel`.
* **Justification:** The `Aircraft` aggregate includes the `CabinConfiguration` Value Object to manage seat distribution, and its current state is simply characterized by an `OperationalStatus` enum. Although it is defined by an `AircraftModel` (which belongs to an external aggregate), the `Aircraft` root entity is responsible for enforcing its internal capacity constraint by validating the configuration against the model's limits, preventing invalid aircraft from being registered.
* **Sequence Diagram:**
  *(To be added)*

---
### 3.7. Aggregate: Air Control Area

* **Aggregate Root:** `AirControlArea`
* **Value Objects:** `GeoBoundary`
* **Scenario:** Register an air control area.
* **Invariant (Business Rule):** The geographic boundaries of the area must be valid.
* **Justification:** The `AirControlArea` is bounded by a `GeoBoundary`. The Aggregate Root is responsible for ensuring that these coordinate boundaries form a valid geographical space before the area is persisted.
* **Sequence Diagram:**
  *(To be added)*

---

### 3.8. Aggregate: Airport

* **Aggregate Root:** `AirControlArea`
* **Value Objects:** `GeoBoundary`
* **Scenario:** Register an air control area.
* **Invariant (Business Rule):** The geographic boundaries of the area must be valid.
* **Justification:** The `AirControlArea` is bounded by a `GeoBoundary`. The Aggregate Root is responsible for ensuring that these coordinate boundaries form a valid geographical space before the area is persisted.
* **Sequence Diagram:**
  *(To be added)*

---
### 3.9. Aggregate: WeatherData

* **Aggregate Root:** `WeatherData`
* **Value Objects:** `WeatherSource`
* **Scenario:** Register weather data or import bulk weather data for a specific air control area (US041 / US042).
* **Invariant (Business Rule):** Weather data must be recorded for a valid and existing air control area, and its origin must be tracked by a known weather source.
* **Justification:** The `WeatherData` aggregate acts as the root that encapsulates environmental readings (such as wind speed, direction, and temperature) and the `WeatherSource` Value Object, which identifies the data provider and format. The Aggregate Root guarantees its internal consistency and enforces the business rule that all meteorological data must be correctly bound to an external `AirControlArea` reference. This ensures that the simulations (which cover these areas) have reliable and traceable environmental conditions to operate on.
* **Sequence Diagram:**
  *(To be added)*


---

### 3.10. Aggregate: Simulation

* **Aggregate Root:** `Simulation`
* **Local Entities:** `SimulationReport`, `SafetyViolation`
* **Scenario:** Simulate flights in a given area and generate a report.
* **Invariant (Business Rule):** A simulation must successfully produce a report that accurately records any safety violations detected between the included flight plans.
* **Justification:** The `Simulation` aggregate controls the execution of flight plans over an `AirControlArea`. It acts as the root that generates and encapsulates the `SimulationReport`, ensuring that all internal `SafetyViolation` instances (which reference `FlightPlanId`) are properly recorded and bound to that specific simulation run.
* **Sequence Diagram:**
  *(To be added)*


---

### 3.11. Aggregate: Flight Route

* **Aggregate Root:** `FlightRoute`
* **Scenario:** Create a flight route for a company (US073).
* **Invariant (Business Rule):** The route must originate and end at different airports, and its name must follow the required format (e.g., TP123) and be unique.
* **Justification:** The `FlightRoute` acts as an independent aggregate. Its internal state is defined by simple attributes and the `FlightRouteStatus` enum. Following DDD best practices, it does not encapsulate direct object references to the `Airport` aggregate, but rather uses `AirportCode` value objects strictly as external references (identifiers) for the origin and destination. The Aggregate Root is responsible for enforcing the business rule that a route's origin and destination cannot be the same, and it validates the specific formatting and uniqueness of the route's name.
* **Sequence Diagram:**
  *(To be added)*
---

### 3.12. Aggregate: Flight

* **Aggregate Root:** `Flight`
* **Local Entities:** None
* **Value Objects:** None
* **Scenario:** Instantiate/schedule a flight for a specific flight route.
* **Invariant (Business Rule):** A flight must be uniquely identified by a valid flight designator (e.g., concatenation of a 2-letter airline designator, up to 4 digits, and an optional operational suffix) and must be properly typed as Regular or Charter.
* **Justification:** The `Flight` aggregate acts as the operational bridge between a static `FlightRoute` and the executable `FlightPlan`. In the domain model, it is an independent aggregate that maintains its own state (such as departure date/time) and is characterized by a `FlightType` enum. The `Flight` Aggregate Root ensures its internal consistency by validating the strict formatting of its unique flight designator. Furthermore, adhering to DDD principles, it manages its relationships by holding only external references (identifiers) to the associated `FlightRoute` and its respective `FlightPlan` instances, avoiding tight coupling between large aggregates.
* **Sequence Diagram:**
  *(To be added)*

---

### 3.13. Aggregate: Air Transport Company

* **Aggregate Root:** `AirTransportCompany`
* **Scenario:** Register a new air transport company in the system (US060).
* **Invariant (Business Rule):** An air transport company must have a valid name, and its IATA code (2 letters) and ICAO code (2-3 letters) must be strictly formatted and globally unique.
* **Justification:** The `AirTransportCompany` aggregate acts as the root entity representing an airline. In the domain model, it does not encapsulate any local entities or custom value objects, as its internal state is defined by simple primitive attributes (name, IATA, and ICAO codes). The Aggregate Root is responsible for enforcing its internal consistency by validating the strict formatting and uniqueness of these codes upon creation. Furthermore, adhering to DDD best practices, it manages its relationships by holding only external references to the `User` (collaborators/pilots), `Aircraft` (fleet), and `FlightRoute` aggregates, ensuring loose coupling and preventing the system from loading massive amounts of data into memory when a company is fetched.
* **Sequence Diagram:**
  *(To be added)*


---
