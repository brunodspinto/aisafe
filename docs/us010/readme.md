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

`User` is an entity — it has a unique identity (email address) and its attributes change over time (status, skills assessment date). It is identified by an `Email`, which is a value object composed of an `EmailDomain`. Both are value objects because they are defined entirely by their attributes and have no identity of their own.

`SecurityClearance` is a value object — it describes the user's clearance level and expiration date. It has no identity independent of the user.

`Role` is a value object typed by the `RoleType` enum. The decision to use a single `User` entity with a `Role` instead of subclasses (`Pilot`, `Collaborator`) follows the requirement in section 3.1.1, which states that the number of roles is expected to grow and multiple roles per user may be supported in the future. A role-based approach is more flexible than inheritance.

---

### Maker, EngineModel and AircraftModel Aggregates

These are three independent aggregates. `Maker`, `EngineModel` and `AircraftModel` each have their own lifecycle and exist independently of one another.

`Maker` manufactures both aircraft models and engine models — a single manufacturer can produce both, as confirmed by the client.

`EngineModel` and `AircraftModel` are separate aggregates because US057 states that engine models can be added to and removed from an aircraft model's certified list — which presupposes that both exist independently. US058 further confirms this: an engine model cannot be removed if aircraft are using it, meaning it persists independently of the aircraft model association.

---

### Aircraft Aggregate

`Aircraft` is an entity identified by its `registrationNumber`. It is an instance of an `AircraftModel` and is owned by an `AirTransportCompany`.

`CabinConfiguration` is a value object — it describes the seat distribution across classes and is defined entirely by its attributes. It has no identity independent of the aircraft.

`registrationCountry` was added as an attribute of `Aircraft` (not `AircraftModel`) because US070 states that an aircraft is registered in a country that may differ from the company's home country. Registration is a property of the specific aircraft instance, not the model.

---

### AirControlArea and Airport Aggregates

`AirControlArea` and `Airport` are independent aggregates. An airport belongs to exactly one air control area, but it has its own lifecycle — it is created independently (US052) and its existence does not depend on the area.

`GeoBoundary` is a value object inside `AirControlArea` — it describes the geographic limits of the area and is defined entirely by its four coordinate attributes.

---

### FlightRoute Aggregate

`FlightRoute` is an entity identified by its `name` (format: company initials + up to 4 digits, e.g. TP123). It has an `activeUntil` attribute and a `FlightRouteStatus` because US074 states that a route can be deactivated from a given date, after which no new flights can be created for it.

`FlightRoute` references `AirportCode` (a shared value object) for its origin and destination. `AirportCode` is declared outside any aggregate boundary because it is used by both `FlightRoute` and `Flight` — value objects can be freely shared across aggregates.

---

### Flight Aggregate

This is the most complex aggregate. `Flight` is an entity and aggregate root — it represents a specific instance of a `FlightRoute` on a given date and time, identified by its `flightDesignator`.

`FlightPlan` is a value object inside this aggregate. This decision follows the client's clarification: *"A flight plan is a description written using a DSL. It's final. So, likely it is a value object."* A flight plan is immutable after submission — if rejected, the pilot must submit a new one. A `Flight` may therefore have more than one `FlightPlan`, as confirmed by the client: *"For a Flight, one can have multiple Flight Plans, albeit just one approved/validated."*

Because `FlightPlan` is a value object, it cannot hold references to other aggregates or have mutable state. These responsibilities belong to `Flight`, which references `Aircraft`, `User`, `WeatherData` and holds the current `FlightPlanStatus`.

`FlightSegment` is a value object — it is part of the immutable DSL description of a flight plan. It is defined by its altitude slots, width and wind data, and has no identity independent of the plan.

`Node` is a value object — it represents a geographic point defined by latitude, longitude and altitude. As confirmed by the client, nodes are not necessarily airports — they are navigation waypoints that connect segments.

---

### WeatherData Aggregate

`WeatherData` is an entity that records meteorological conditions for a specific `AirControlArea` on a given date. It is associated with a `Flight` when the pilot adds weather data to the flight plan (US082).

`WeatherSource` is a value object that describes the provider and format of the imported data. US042 states that weather data may come from multiple external providers.

---

### Simulation Aggregate

`Simulation` is an entity that represents an execution of all flights in a given area and time range.

`SimulationReport` is an entity — it has identity within the aggregate and contains `SafetyViolation` entities. Because it contains entities, it cannot be a value object.

`SafetyViolation` is an entity — it records a specific safety event with its own identity (timestamp, position, speed, heading). It references `FlightId`, a shared value object, to identify the flight involved without crossing aggregate boundaries directly.

---

## Shared Value Objects

Two value objects are declared outside any aggregate boundary:

`AirportCode` — used by both `FlightRoute` (origin and destination of the route) and `Flight` (departure and arrival airports of the specific instance). As an immutable value object, it can be freely shared.

`FlightId` — used by `SafetyViolation` to reference a `Flight` from within the `Simulation Aggregate`. A direct object reference to an aggregate root of another aggregate is not permitted by DDD rules. Using a value object with the flight's identifier respects the aggregate boundary.

---
