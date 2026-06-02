# US080 — Create a Flight Plan

## 1. Context

This US is being developed in Sprint 3. It allows a Pilot to register a flight plan for an existing flight route, providing the aircraft, departure date/time and fuel quantity. The pilot assigned to the plan must belong to the route's company. The flight plan is created with status `DRAFT` and is the starting point of a multi-step validation process — semantic validation via the Flight DSL (US083) and flight testing in the simulator (US085).

### 1.1 List of issues

Analysis: Define the domain rules for `FlightPlan` form-based creation: the relation to `FlightRoute`, `Aircraft` and `Pilot`, the company-matching invariants, and the initial status.

Design: Define the architecture for flight plan registration (UI, controller, repository) and align with the pre-existing `FlightPlan` aggregate already partially shaped by US081 (creation from DSL file) and US083 (DSL specification).

Implement: Implement the form-based `FlightPlan` creation, the application controller, the console UI, and integrate with the Pilots' menu.

Test: Unit tests for `FlightPlan` domain invariants (above 90% coverage) and manual acceptance tests for the end-to-end flow.

---

## 2. Requirements

**US080** As a Pilot, I want to register a flight plan for a route.

> *(Enunciado, p. 19, lines 9–12):* "I must add the aircraft, departure date/time, fuel quantity, pilot. The pilot must be of the route's company. Flight plan status is set to 'draft' when created and must undergo a multi-step validation process."

**Acceptance Criteria:**

- **AC080.1** Only an authenticated user with the `PILOT` role may register a flight plan.
- **AC080.2** The flight plan must reference an existing `FlightRoute` already registered in the system.
- **AC080.3** The pilot assigned to the flight plan must belong to the company of the flight route — i.e. `pilot.companyIataCode()` must equal `flightRoute.companyIataCode()`. *(Literal rule from the enunciado.)*
- **AC080.4** The aircraft referenced in the flight plan must already exist in the system.
- **AC080.5** The fuel quantity must be strictly positive.
- **AC080.6** The departure date/time must be in the future (it is not allowed to plan a flight in the past).
- **AC080.7** The flight plan is created with status `DRAFT`.
- **AC080.8** The flight plan has a unique identifier (`FlightPlanDesignator`).
- **AC080.9** The aircraft assigned to the flight plan must belong to the company of the flight route. *(Derived rule — a route is operated by one company's fleet; not explicitly stated in the enunciado but follows from the domain.)*
- **AC080.10** The aircraft must be in `ACTIVE` operational status (not `DECOMMISSIONED`). *(Derived rule — by analogy with US071, a decommissioned aircraft cannot be assigned to new flight plans.)*

**Notes on interpretation:**

The enunciado does not provide an explicit "Acceptance Criteria" section for US080. The criteria above are derived from the user story statement and from the system specifications (sections 3.2 and 3.4 of the requirements document). Criteria literally stated in the enunciado are marked as *Literal*; criteria reasoned from the domain are marked as *Derived*.

The user story is read with the interpretation that the **authenticated pilot is the pilot assigned to the flight plan** — the "I" of "I must add … pilot" is the authenticated user. The UI does not ask the operator to select a pilot from a list; the pilot of the plan is implicitly the session user. This is the most direct reading of "As a Pilot, I want to register a flight plan" and keeps the use case simple. The remaining inputs the pilot must provide are therefore: **flight route, aircraft, departure date/time, fuel quantity**.

**Dependencies/References:**

- **US030** — Authentication and Authorization (the user must be authenticated with the `PILOT` role).
- **US055** — Create an Aircraft Model (the aircraft model must exist for the aircraft to exist).
- **US060** — Register an Air Transport Company (the route belongs to a company; the pilot also belongs to a company).
- **US070** — Add an Aircraft to Fleet (the aircraft must be registered).
- **US073** — Create a flight route (the route must exist before a flight plan can reference it).
- **US075** — Add a pilot (the pilot must be registered, with `companyIataCode` set).
- Acts as a prerequisite for:
  - **US082** — Insert weather data in a flight (operates on an existing flight plan).
  - **US083** — Flight DSL specification and validation (LPROG — semantic validation of the plan).
  - **US085** — Test/validate flight plan (LAPR4 — tests the plan in simulation).

**Out of scope for this user story:**

- Flight plan creation **from a DSL file** — covered by US081 (LPROG).
- The **multi-step validation process** itself (lexical/syntactic/semantic validation, fuel calculations, collision detection) — covered by US083 (LPROG) and US085 (LAPR4). US080 only puts the plan in `DRAFT`; the subsequent validation states are the responsibility of those user stories.
- **Creation of the flight route** — covered by US073.
- **Multi-leg flight plans** with intermediate stops and segment definitions — relevant only for the DSL-based flight plans (US081) and consumed by the simulation (US100). The form-based creation in US080 registers a single-leg plan covering the route from its declared origin to its destination.
- **Crew assignment and passenger/cargo load** — section 3.2 lists these as part of a flight's domain, but they are not in the explicit input list for US080 ("aircraft, departure date/time, fuel quantity, pilot") and are therefore not handled here.
