# US082 — Insert Weather Data in a Flight

## 1. Context

This US is being developed in Sprint 3. It allows a Pilot to associate weather data with one of their own flight plans. The weather data has previously been registered in the system by a Weather Person (US041/US042) for a specific air control area. If the flight plan had already been tested, attaching new weather data voids that test, because the simulation/test result depended on the previous weather conditions.

### 1.1 List of issues

Analysis: Define how a `FlightPlan` references the `WeatherData` it uses, the "plan of mine" ownership rule, and the status transition that voids a previous test.

Design: Define the architecture for the use case (UI, controller), the new association on the `FlightPlan` aggregate, and the status-voiding behaviour.

Implement: Extend the `FlightPlan` aggregate to hold its weather data reference(s) and to void a previous test; implement the application controller and the console UI; integrate with the Pilots' menu.

Test: Unit tests for the `FlightPlan` weather-data association and the test-voiding invariant (above 90% coverage), plus manual acceptance tests for the end-to-end flow.

---

## 2. Requirements

**US082** As a Pilot, I want to add weather data to a flight plan of mine.

> *(Enunciado, p. 19):* "If the flight plan has been previously tested, the test is deemed void because of the new weather data."

**Acceptance Criteria:**

- **AC082.1** Only an authenticated user with the `PILOT` role may add weather data to a flight plan.
- **AC082.2** The flight plan must belong to the authenticated pilot — i.e. its assigned pilot is the session user ("of mine").
- **AC082.3** The flight plan must reference an existing flight plan in the system (identified by its designator).
- **AC082.4** The weather data added must already exist in the system (it was registered by a Weather Person — US041/US042).
- **AC082.5** The weather data becomes associated with the flight plan (the plan keeps a reference to it by identity).
- **AC082.6** If the flight plan was previously in `TESTED` status, attaching new weather data voids the test: the status reverts to `VALIDATED`. The previous DSL/semantic validation is not affected — only the flight test is voided.
- **AC082.7** Attaching weather data to a flight plan that is not `TESTED` (i.e. `DRAFT` or `VALIDATED`) does not change its status.

**Notes on interpretation:**

The enunciado does not provide an explicit "Acceptance Criteria" section for US082. The criteria above are derived from the user story statement and from the system specifications (sections 3.2 and 3.4 of the requirements document).

Three points are open for confirmation with the Product Owner / team and will be settled in the Analysis/Design phase:

1. **Void target status (AC082.6).** "The test is deemed void" is read as voiding only the flight test (US085), not the DSL/semantic validation (US083), which does not depend on weather. The status therefore reverts `TESTED → VALIDATED`. An alternative reading would revert all the way to `DRAFT` (re-run the whole multi-step process); this is considered more conservative but less faithful to "the *test* is void".

2. **One or many weather records.** A flight may span more than one air control area, and weather data is recorded per area and date. The plan is therefore modelled to hold a set of weather data references (the pilot may attach more than one over successive operations), rather than a single record. This is the most flexible reading; a single-record interpretation is also defensible.

3. **Area/date matching.** The enunciado does not require the attached weather data to match the flight's air control area(s) or departure date. For US082, any existing weather data record may be attached; constraining it to the route's area(s) and date is treated as a possible future enhancement, not a requirement here.

**Dependencies/References:**

- **US030** — Authentication and Authorization (the user must be authenticated with the `PILOT` role).
- **US041 / US042** — Register / Import weather data (the weather data must already exist in the system).
- **US080** — Create a flight plan (the flight plan must exist, and carries the `assignedPilotId` used for the ownership rule and the `status` that may be voided).
- Relates to:
  - **US085** — Test/validate flight plan (LAPR4 — produces the `TESTED` status that US082 may void).

**Out of scope for this user story:**

- **Weather impact computation** on the flight path or fuel — that belongs to the simulation (US110) and the flight test (US085), and is explicitly out of scope of the back-office (section 3.4.6).
- **Re-validation and re-testing** of the flight plan after the test is voided — performed by US083 (LPROG) and US085 (LAPR4); US082 only voids the previous test by reverting the status.
- **Registering or importing weather data** — covered by US041 and US042; US082 only consumes already-registered weather data.
