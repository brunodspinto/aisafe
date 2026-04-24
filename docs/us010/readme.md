# Domain Model — Justification

## Overview

The AISafe domain model was designed following Domain-Driven Design (DDD) principles, as required by US010. The model identifies aggregates, entities, value objects and their relationships based on the project requirements and clarifications provided by the client.

The system covers four main functional areas:

- **Backoffice configuration** — aircraft, engines, airports, companies
- **Flight management** — routes, flights and flight plans
- **Weather service** — weather data import and consultation
- **Flight simulation** — simulation and safety violation detection

---

## Entities, Value Objects and Key Decisions

### User Aggregate

`User` is an entity — it has a unique identity (email address) and its attributes change over time (status, skills assessment date). It is identified by an `Email` value object. The `Email` value object validates the address format and domain internally — a separate `EmailDomain` value object is unnecessary as the domain is already part of the address.

`SecurityClearance` is a value object — it describes the user's clearance level and expiration date. It has no identity independent of the user. The client confirmed that security clearance requirements apply to all users, including Administrators and Backoffice Operators.

The `Role` intermediary was removed. `User` now references `RoleType` directly with multiplicity `1..*`. This follows the requirement in section 3.1.1, which states that the number of roles is expected to grow and multiple roles per user may be supported in the future. The client confirmed that position = role.

The valid email domains for internal AISafe users are loaded via bootstrap. The client confirmed that collaborator emails outside the company domain are acceptable and cannot be verified.

---

### Maker, EngineModel and AircraftModel Aggregates

These are three independent aggregates. `Maker`, `EngineModel` and `AircraftModel` each have their own lifecycle and exist independently of one another.

The client confirmed that a single entity can be both an aircraft manufacturer and an engine manufacturer — there is no strict separation.

`EngineModel` and `AircraftModel` are separate aggregates because US057 states that engine models can be added to and removed from an aircraft model's certified list — which presupposes that both exist independently. US058 further confirms this: an engine model cannot be removed if aircraft are using it, meaning it persists independently of the aircraft model association.

The client confirmed that an aircraft model can be certified with engine models from different manufacturers.

---

### Aircraft Aggregate

`Aircraft` is an entity identified by its `registrationNumber`. It is an instance of an `AircraftModel` and is owned by an `AirTransportCompany`.

`CabinConfiguration` is a value object with multiplicity `0..*` — a cargo aircraft has no cabin configuration, while a passenger aircraft has one. The total number of seats cannot exceed the model's maximum capacity.

`registrationCountry` was added as an attribute of `Aircraft` (not `AircraftModel`) because US070 states that an aircraft is registered in a country that may differ from the company's home country.

---

### AirControlArea and Airport Aggregates

`AirControlArea` and `Airport` are independent aggregates. An airport belongs to exactly one air control area, but it has its own lifecycle.

The client confirmed that air control areas cannot overlap, and that the coordinates of the boundaries must not be the same (zero area). The `GeoBoundary` value object validates that coordinates are within the valid ranges (latitude [-90°, 90°], longitude [-180°, 180°]) and that the area is non-zero.

The client confirmed that the location of an airport must be inside the air control area it belongs to. If the airport coordinates don't belong to the intended air control area, the system must reject the creation.

`IATACode` and `ICAOCode` are now shared value objects outside any aggregate boundary. The client confirmed that the company name, IATA code and ICAO code must all be unique. These value objects encapsulate the format validation (IATA = 3 letters for airports / 2 letters for companies, ICAO = 4 letters for airports / 2-3 letters for companies).

---

### FlightRoute Aggregate

`FlightRoute` is an entity identified by its `name` (format: company initials + up to 4 digits, e.g. TP123). The client confirmed that a route is owned by an Air Transport Company and its ID includes the company ID.

`FlightRoute` references `IATACode` with multiplicity `2` — a route always connects exactly two airports. The multiplicity `2` with a single association replaces two separate arrows and is cleaner. The distinction between origin and destination is handled at the implementation level.

The client confirmed that each route is unique per company — different companies can operate routes between the same airports but they are distinct routes.

---

### Flight Aggregate

This is the most complex aggregate. `Flight` is an entity and aggregate root — it represents a specific instance of a `FlightRoute` on a given date and time, identified by its `flightDesignator`.

`FlightPlan` is now an **entity** inside this aggregate. This decision was revised after the Sprint 1 presentation. The client clarified that a flight plan has a lifecycle — it evolves through states (DRAFT → VALIDATED → APPROVED / REJECTED) — and a flight can have multiple plans over time. Each plan requires individual identity to be distinguished.

The client confirmed: *"For a Flight, one can have multiple Flight Plans, albeit just one approved/validated."* If a flight plan is rejected, the pilot must submit a new one.

The `FlightPlanStatus` belongs to the `FlightPlan` entity — each plan has its own independent status. The `FlightPlan` is also linked to the `User` who reviewed it (`reviewed by`, multiplicity `0..1`) — this is the `FLIGHT_CONTROL_OPERATOR` who approves or rejects the plan.

`FlightPlan` was enriched with additional attributes (`passengerCount`, `crewCount`, `totalWeight`, `estimatedDuration`) to better describe the complete operational state of the flight.

The client confirmed that weather conditions are not the same everywhere inside an air control area. Weather data is added to a flight plan via US082 after the plan is created. The client confirmed: *"US080 → Create a flight, including its flight plan. US082 → Add weather data to an existing flight plan of a flight."*

`Flight` references `IATACode` with multiplicity `2` for departure and arrival airports. The `FlightPlan` references one `IATACode` for the alternate airport.

`FlightSegment` is a value object. The client confirmed that nodes are not necessarily airports — they are navigation waypoints. Each `FlightSegment` has exactly `2` nodes — start and end — represented by a single association with multiplicity `2` instead of two separate arrows.

The client confirmed that a flight plan supports multiple legs (the DSL supports this), but for the prototype only direct flights are considered.

For regular flights, the schedule is represented as days of the week with times (e.g. Monday 12:00, Tuesday 12:30). For charter flights, there is a single specific date and time.

---

### WeatherData Aggregate

`WeatherData` is an entity that records meteorological conditions for a specific `AirControlArea` on a given date.

`WeatherSource` is a value object that describes the provider and format of the imported data. US042 states that weather data may come from multiple external providers.

The client confirmed that weather conditions are not the same everywhere inside an air control area — data may vary by location and altitude within the area.

---

### Simulation Aggregate

`Simulation` is an entity that represents an execution of all flights in a given area and time range.

The client confirmed that the simulation module is implemented in C (SCOMP), but the core Java system must send information to run the simulation and receive feedback and results from it. The `Simulation` aggregate models this interface.

`SimulationReport` is an entity — it has identity within the aggregate and aggregates simulation results including safety violations.

`SafetyViolation` is a **value object** — it is an immutable record of a safety event. Once recorded it never changes state and has no independent lifecycle. It includes a `flightDesignator` attribute to identify which flight was involved, and a `description` field for human-readable context. The `FlightId` value object was removed as redundant — the `flightDesignator` attribute provides sufficient identification for reporting purposes.

---

## Shared Value Objects

`IATACode` and `ICAOCode` are declared outside any aggregate boundary because they are used by multiple aggregates (`Airport`, `AirTransportCompany`, `FlightRoute`, `Flight`, `FlightPlan`). As immutable value objects with format validation, they can be freely shared.

---

## Key Client Clarifications Summary

| Topic | Client Answer |
|-------|--------------|
| Can a Maker be both aircraft and engine manufacturer? | Yes |
| Can an AircraftModel use engines from different manufacturers? | Yes |
| Are nodes always airports? | No — they are navigation waypoints |
| Is a route owned by one company? | Yes — route ID includes company ID |
| Can a flight have multiple flight plans? | Yes — but only one approved/validated |
| Does FlightPlan have weather data? | Yes — added via US082 after plan creation |
| Do security clearance requirements apply to all users? | Yes |
| Can air control areas overlap? | No |
| Must airport coordinates be inside its air control area? | Yes |
| Is simulation part of the Java domain model? | Yes — the system sends/receives simulation data |
| Can a pilot work for multiple companies? | No — one company at a time |
| What does "position" mean for collaborators? | Role |
