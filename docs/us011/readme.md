# US011 - Aggregate Justification

## 1. Objective

This User Story aims to demonstrate and justify the responsibility of each Aggregate designed in the Domain Model (US010). For each aggregate, a representative scenario is illustrated through a Sequence Diagram (SD) and a brief explanation of the business rule (*invariant*) that the Aggregate Root (an Entity) is responsible for enforcing.

## 2. Tools and Rules

In accordance with **NFR02 (Technical Documentation)**:

* The sequence diagrams has to be created using **PlantUML**.


---

## 3. Aggregate Catalog

Justification of the Aggregates identified for the *AISafe* domain:

---

### 3.1. Aggregate: Flight Plan

* **Aggregate Root:** `FlightPlan`
* **Value Objects:** `FlightPlanStatus`, `FlightType`
* **Scenario:** Register/import a flight plan
* **Invariant (Business Rule):** The arrival node of a segment must match the departure node of the next segment, ensuring route continuity.
* **Justification:** The domain model defines that a `FlightPlan` is composed of multiple elements such as segments and nodes, but only certain attributes are modeled as Value Objects. The Aggregate Root ensures consistency across the structure, enforcing that all segments are properly connected and preventing invalid or disconnected routes from being persisted.
* **Sequence Diagram:**
  (to be added)

---

### 3.2. Aggregate: User

* **Aggregate Root:** `User`
* **Value Objects:** `Email`, `EmailDomain`, `SecurityClearance`, `RoleType`
* **Scenario:** Register a new user in the system.
* **Invariant (Business Rule):** A user must have a valid email, a defined role type, and an associated security clearance.
* **Justification:** A `User` is identified by an `Email`, which belongs to an `EmailDomain`. The user also holds a `SecurityClearance` and is assigned a role type. These Value Objects describe the identity and access permissions of a user. The Aggregate Root ensures that all these components are valid and consistent when creating or updating a user.
* **Sequence Diagram:**
  *(To be added)*

---

### 3.3. Aggregate: Aircraft

* **Aggregate Root:** `Aircraft`
* **Value Objects:** `CabinConfiguration`, `OperationalStatus`
* **Scenario:** Add an aircraft to the fleet.
* **Invariant (Business Rule):** The total number of seats must not exceed the capacity defined by the associated `AircraftModel`.
* **Justification:** The `Aircraft` aggregate includes Value Objects such as `CabinConfiguration` and `OperationalStatus`. Although it is associated with an `AircraftModel` (external aggregate), the root entity ensures that internal configurations respect the model constraints, preventing invalid aircraft configurations.
* **Sequence Diagram:**
  *(To be added)*

---
