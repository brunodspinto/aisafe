
## 3. Aggregate Catalog

Justification of the main Aggregates identified for the *AISafe* domain:

---

### 3.1. Aggregate: Air Transport Company

* **Aggregate Root:** `AirTransportCompany`
* **Scenario:** Register a new air transport company in the system (US060).
* **Invariant (Business Rule):** An air transport company must have a valid name, and its IATA code (2 letters) and ICAO code (2-3 letters) must be strictly formatted and globally unique.
* **Justification:** Acts as the root entity representing an airline, defined entirely by primitive attributes. Following DDD best practices, it ensures loose coupling by holding only external references to the `User`, `Aircraft`, and `FlightRoute` aggregates, preventing heavy database memory loads.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_1/aggregate-3_1.puml)*
  * Note: As per Section 7.2 of the Application Engineering Process guidelines, this diagram also serves as the standard "Register X" architectural flow for other simple catalog aggregates in the system (such as Maker, Aircraft Model, and Engine Model)


---

### 3.2. Aggregate: User

* **Aggregate Root:** `User`
* **Value Objects:** `Email`, `EmailDomain`, `SecurityClearance`, `Role`
* **Enums:** `RoleType`
* **Scenario:** Register a new user in the system (US031).
* **Invariant (Business Rule):** A user must have a valid email domain, an assigned role, and an associated security clearance.
* **Justification:** Acts as the root entity managing user identity and access. It encapsulates highly cohesive Value Objects (Email, Role, SecurityClearance), guaranteeing their internal validity and consistency upon creation.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_2/aggregate-3_2.puml)*

---

### 3.3. Aggregate: Maker

* **Aggregate Root:** `Maker`
* **Scenario:** Register a new aircraft or engine manufacturer in the system catalog (Backoffice Configuration).
* **Invariant (Business Rule):** A manufacturer must have a valid and unique name to correctly identify the maker of aircraft and engine models.
* **Justification:** Acts as a standalone catalog root entity defined entirely by primitive attributes. It ensures internal consistency and serves strictly as an independent external reference for the AircraftModel and EngineModel aggregates, ensuring low coupling.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_3/aggregate-3_3.puml)*
  * Following Section 7.2 of the Application Engineering Process guidelines , this aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company).  A repetitive diagram is omitted.

---

### 3.4. Aggregate: Engine Model

* **Aggregate Root:** `EngineModel`
* **Enums:** `EngineType`
* **Scenario:** Register a new aircraft engine model (US056).
* **Invariant (Business Rule):** The combination of the engine model's name and its manufacturer (`Maker`) must be unique.
* **Justification:** Acts as a standalone catalog root entity defined entirely by primitive data types and an `EngineType` enum. It guarantees internal consistency and ensures low coupling by relying strictly on an independent external reference to the `Maker` aggregate to enforce its uniqueness rule.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_4/aggregate-3_4.puml)*
  * Following Section 7.2 of the Application Engineering Process guidelines , this aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company).  A repetitive diagram is omitted.
---

### 3.5. Aggregate: Aircraft Model

* **Aggregate Root:** `AircraftModel`
* **Enums:** `AircraftType`
* **Scenario:** Create a new aircraft model to be added to the catalog (US055).
* **Invariant (Business Rule):** An aircraft model must have a unique combination of model name and manufacturer, and it must have at least one certified engine model associated with it.
* **Justification:** Acts as a catalog root entity characterized by primitive attributes and an `AircraftType` enum. It maintains internal consistency by validating its aerodynamic data upon creation, and ensures low coupling by holding only external references to its `Maker` and the certified `EngineModel` aggregates.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_5/aggregate-3_5.puml)*
  * Following Section 7.2 of the Application Engineering Process guidelines , this aggregate adopts the standard "Register X" flow illustrated in Section 3.1 (Air Transport Company). A repetitive diagram is omitted. 
* 
---

### 3.6. Aggregate: Aircraft

* **Aggregate Root:** `Aircraft`
* **Value Objects:** `CabinConfiguration`
* **Enums:** `OperationalStatus`
* **Scenario:** Add an aircraft to the fleet (US070).
* **Invariant (Business Rule):** The total number of seats configured in the cabin must not exceed the maximum capacity defined by the associated `AircraftModel`.
* **Justification:** Acts as the root entity representing a physical aircraft. It encapsulates the `CabinConfiguration` Value Object and ensures internal consistency by validating its capacity. It maintains low coupling by holding only an external reference to the `AircraftModel` aggregate to enforce this limit.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_6/aggregate-3_6.puml)*

---
### 3.7. Aggregate: Air Control Area

* **Aggregate Root:** `AirControlArea`
* **Value Objects:** `GeoBoundary`
* **Scenario:** Register an air control area.
* **Invariant (Business Rule):** The geographic boundaries of the area must be valid.
* **Justification:** Acts as the root entity representing a designated airspace. It encapsulates the `GeoBoundary` Value Object and guarantees internal consistency by strictly validating that the coordinate boundaries form a logically correct geographical space upon creation.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_7/aggregate-3_7.puml)*

---
### 3.8. Aggregate: Airport

* **Aggregate Root:** `Airport`
* **Scenario:** Register an airport in a given air control area (US052).
* **Invariant (Business Rule):** An airport must have globally unique IATA and ICAO codes. Its geographic coordinates (latitude and longitude) must fall strictly within the rectangular boundaries of its associated `AirControlArea`.
* **Justification:** Acts as an independent root entity representing a physical location, defined entirely by primitive attributes. It guarantees internal consistency upon creation and ensures low coupling by holding only an external reference to the `AirControlArea` aggregate to validate its geographic boundaries.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_8/aggregate-3_8.puml)*
---
### 3.9. Aggregate: WeatherData

* **Aggregate Root:** `WeatherData`
* **Value Objects:** `WeatherSource`
* **Scenario:** Register weather data or import bulk weather data for a specific air control area (US041 / US042).
* **Invariant (Business Rule):** Weather data must be recorded for a valid and existing air control area, and its origin must be tracked by a known weather source.
* **Justification:** Acts as the root entity encapsulating environmental readings and the `WeatherSource` Value Object. It guarantees internal consistency and ensures low coupling by relying strictly on an external reference to the `AirControlArea` aggregate.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_9/aggregate-3_9.puml)*


---

### 3.10. Aggregate: Simulation

* **Aggregate Root:** `Simulation`
* **Local Entities:** `SimulationReport`, `SafetyViolation`
* **Value Objects:** `FlightId`
* **Enums:** `SimulationStatus`
* **Scenario:** Simulate flights in a given area and generate a validation report (US109/US111).
* **Invariant (Business Rule):** A simulation must successfully produce a report that accurately records any safety violations detected between the included flights.
* **Justification:** Acts as the root entity managing simulation execution. By team design decision, it directly references the included `FlightPlan` Value Objects to access operational data, while simultaneously encapsulating the `SimulationReport`. Internal `SafetyViolation` entities maintain low coupling by using `FlightId` strictly as an external reference to identify involved flights.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_10/aggregate-3_10.puml)*
---

### 3.11. Aggregate: Flight Route

* **Aggregate Root:** `FlightRoute`
* **Value Objects:** `AirportCode`
* **Enums:** `FlightRouteStatus`
* **Scenario:** Create a flight route for a company (US073).
* **Invariant (Business Rule):** The route must originate and end at different airports, and its name must follow the required format (e.g., TP123) and be unique.
* **Justification:** Acts as an independent root entity defined by simple attributes. It guarantees internal consistency and ensures strict low coupling by using `AirportCode` Value Objects solely as external references for its origin and destination, entirely avoiding heavy dependencies on the `Airport` aggregate.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_11/aggregate-3_11.puml)*
---

### 3.12. Aggregate: Flight

* **Aggregate Root:** `Flight`
* **Value Objects:** `FlightPlan`, `FlightSegment`, `Node`, `AirportCode`
* **Enums:** `FlightType`, `FlightPlanStatus`
* **Scenario:** Instantiate/schedule a flight for a specific flight route and register/validate its flight plans (US080).
* **Invariant (Business Rule):** A flight must belong to a valid flight route, be properly typed (Regular or Charter), and encapsulate its flight plans. The flight plan itself is responsible for gathering all operational data, including the assigned aircraft, the pilot, weather data, and its specific validation status.
* **Justification:** Acts as the root entity representing an operational flight. It encapsulates the highly cohesive `FlightPlan` Value Object (which delegates all complex external references like Pilot and Aircraft). It guarantees internal consistency and ensures low coupling by holding only an external reference to the `FlightRoute` and using `AirportCode`s for geographic locations.
* **Sequence Diagram:**
  *[Sequence Diagram](aggregate-3_12/aggregate-3_12.puml)*
---
