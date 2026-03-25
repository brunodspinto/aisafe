# US011 - Aggregate Justification

## 1. Objective

This User Story aims to demonstrate and justify the responsibility of each Aggregate designed in the Domain Model (US010). For each aggregate, a representative scenario is illustrated through a Sequence Diagram (SD) and a brief explanation of the business rule (*invariant*) that the Aggregate Root (an Entity) is responsible for enforcing.

## 2. Tools and Rules

In accordance with **NFR02 (Technical Documentation)**:

* The sequence diagrams were created using **PlantUML**.
* The source code of the diagrams (`.puml`) and their corresponding vector images (`.png`) are stored in this directory (`docs`).

---

## 3. Aggregate Catalog

Justification of the main Aggregates identified for the *AISafe* domain:

---

### 3.1. Aggregate: Flight Plan

* **Aggregate Root:** `FlightPlan`
* **Local Entities:** `FlightSegment`, `Node`
* **Value Objects:** `FlightPlanStatus`
* **Scenario:** Register/import a flight plan.
* **Invariant (Business Rule):** The end node of a segment must match the start node of the next segment, ensuring route continuity.
* **Justification:** The domain model defines that a `FlightPlan` is composed of multiple `FlightSegment` entities, which in turn start and end at a `Node`. The Aggregate Root (`FlightPlan`) ensures consistency across this internal structure, enforcing that all segments are properly connected and preventing invalid or disconnected routes from being persisted in the database.
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

### 3.3. Aggregate: Aircraft

* **Aggregate Root:** `Aircraft`
* **Value Objects:** `CabinConfiguration`, `OperationalStatus`
* **Scenario:** Add an aircraft to the fleet.
* **Invariant (Business Rule):** The total number of seats configured in the cabin must not exceed the maximum capacity defined by the associated `AircraftModel`.
* **Justification:** The `Aircraft` aggregate includes Value Objects such as `CabinConfiguration` and `OperationalStatus`. Although it is defined by an `AircraftModel` (which belongs to a different aggregate), the `Aircraft` root entity is responsible for enforcing its internal capacity constraint, preventing invalid aircraft configurations from being registered.
* **Sequence Diagram:**
  *(To be added)*

---

### 3.4. Aggregate: Flight Route

* **Aggregate Root:** `FlightRoute`
* **Value Objects:** `FlightRouteStatus`, `AirportCode` (Reference)
* **Scenario:** Create a flight route for a company.
* **Invariant (Business Rule):** The route must originate and end at different airports, and its name must follow the required format (e.g., TP123).
* **Justification:** The `FlightRoute` defines a connection between two `AirportCode` value objects. The Aggregate Root validates the uniqueness and format of its name, and ensures that the origin and destination are not the same, maintaining the integrity of the company's route network.
* **Sequence Diagram:**
  *(To be added)*

---

### 3.5. Aggregate: Simulation

* **Aggregate Root:** `Simulation`
* **Local Entities:** `SimulationReport`, `SafetyViolation`
* **Scenario:** Simulate flights in a given area and generate a report.
* **Invariant (Business Rule):** A simulation must successfully produce a report that accurately records any safety violations detected between the included flight plans.
* **Justification:** The `Simulation` aggregate controls the execution of flight plans over an `AirControlArea`. It acts as the root that generates and encapsulates the `SimulationReport`, ensuring that all internal `SafetyViolation` instances (which reference `FlightPlanId`) are properly recorded and bound to that specific simulation run.
* **Sequence Diagram:**
  *(To be added)*

---

### 3.6. Aggregate: Air Control Area

* **Aggregate Root:** `AirControlArea`
* **Value Objects:** `GeoBoundary`
* **Scenario:** Register an air control area.
* **Invariant (Business Rule):** The geographic boundaries of the area must be valid.
* **Justification:** The `AirControlArea` is bounded by a `GeoBoundary`. The Aggregate Root is responsible for ensuring that these coordinate boundaries form a valid geographical space before the area is persisted.
* **Sequence Diagram:**
  *(To be added)*

---
