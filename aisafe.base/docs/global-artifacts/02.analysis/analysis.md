# Analysis

This document captures the domain analysis derived from the specification (chapter 3) and the user stories (chapter 4). The team adopted Domain-Driven Design as required by US010.

## 1. Domain summary

AISafe is a back-office platform for flight control. It is operated by three distinct user populations:

1. **Backoffice staff** — Admin and Backoffice Operator — who maintain the reference data the system depends on (airports, air control areas, makers, aircraft and engine models, air transport companies and their collaborators).
2. **Air transport companies** — represented by ATCC collaborators and pilots — who manage their own fleet, routes and flight plans.
3. **Flight control entities** — represented by FCOs — who test flight plans and simulate flights over an air control area to detect safety violations.

A separate Weather Person role feeds environmental data into the system for use during flight tests and simulations.

## 2. Bounded contexts

The analysis groups the domain in five bounded contexts. Each context owns its aggregates and is autonomous in terms of persistence and consistency rules. Inter-context references are by identity (registration number, IATA/ICAO codes, username).

| Bounded context | Aggregates | Owns the rules for |
|---|---|---|
| **User Management** | `User`, `SystemUser` (EAPLI) | Authentication, role assignment, security clearance, skills assessment, enable/disable |
| **Backoffice Configuration** | `Airport`, `AirControlArea`, `Maker`, `AircraftModel`, `EngineModel` | Uniqueness of codes and names, certified-engine compatibility, removal restrictions |
| **Air Transport Companies** | `AirTransportCompany`, `Collaborator` | Company identity (IATA/ICAO), collaborator activation, contact-info edits |
| **Fleet & Flights** | `Aircraft` | Registration uniqueness, cabin vs cargo rules, decommission lifecycle |
| **Weather** | `WeatherData`, `WeatherSource` | Air-control-area scoping of measurements, provenance tracking |

## 3. Key aggregates and invariants

The justification of each aggregate boundary lives in [US011](../../user-stories/US011/readme.md). The most load-bearing invariants are:

- **`Aircraft`** — registration number is the identity; `cabinConfiguration` is `null` if and only if the model is CARGO; total seats may not exceed `AircraftModel.maxCapacity()` when the model declares one; an aircraft starts in `ACTIVE` and may only transition to `DECOMMISSIONED` (terminal).
- **`AircraftModel`** — `(modelName, maker)` must be globally unique; at least one certified engine model must be associated; engines on the same model must share the engine type.
- **`AirTransportCompany`** — `IATA` (2 letters) and `ICAO` (2–3 letters) are globally unique; the company owns its fleet as a set of registration strings (low coupling between aircraft and company).
- **`Airport`** — both IATA and ICAO codes are globally unique; an airport is associated with exactly one `AirControlArea`.
- **`SecurityClearance`** — level + expiration date; expiration must be today or in the future.

## 4. Concepts added in Sprint 2 / deferred to later sprints

The following concepts were introduced in Sprint 2 based on the Flight DSL (US081, US083) and simulation (US100–US103):

- **Flight Aggregate** — `Flight`, `FlightPlan`, `FlightSegment`, `Node` — modelled and present in the domain model V6. `FlightPlan` is an entity with a lifecycle (DRAFT → VALIDATED → APPROVED / REJECTED); a flight may have multiple plans but only one approved. `FlightSegment` and `Node` are value objects.
- **Simulation Aggregate** — `Simulation`, `SimulationReport`, `SafetyViolation`, `FlightExecutionStatus` — modelled in V6 to capture simulation state and per-flight outcomes (US109).

The following concepts remain out of scope for Java modelling:

- **Aircraft physics** (lift/drag, thrust, fuel) — used only by the C simulation engine; no Java aggregate is needed.
- **Route, Pilot roster** — `FlightRoute` aggregate is modelled; full pilot roster aggregates (US075–US077) are in progress.

## 5. Cross-cutting concerns

- **Authentication and authorization** are framework concerns delegated to EAPLI (`SystemUser`, `Role`, `AuthorizationService`). Each use case enforces the required role via `authz.ensureAuthenticatedUserHasAnyOf(...)`.
- **Persistence** is selectable at startup (`application.properties`) between an in-memory repository factory and a JPA factory backed by H2 in TCP mode. See [persistence.md](../../persistence.md).
- **Bootstrap** seeds a minimum data set so the application is usable on the first run. It is idempotent: re-running it skips entities that already exist.

## 6. Glossary and supplementary specification

- Domain vocabulary: [glossary.md](../01.requirements-engineering/glossary.md)
- Non-functional requirements: [supplementary-specification.md](../01.requirements-engineering/supplementary-specification.md)
- Use cases and actors: [use-case-diagram.md](../01.requirements-engineering/use-case-diagram.md)
