# US073 — Create a Flight Route

## 1. Context

This US is being implemented for the first time in Sprint 3. It allows an **Air Transport Company Collaborator (ATCC)** to create a new flight route for their company, defining the origin and destination airports and assigning a unique route name that follows the required naming convention.

`FlightRoute` is a new aggregate in the domain. It depends on `Airport` (US052) and `AirTransportCompany` (US060) being already registered in the system. Flight routes are a foundational prerequisite for US080 (Create a Flight Plan), since every flight plan is instantiated from a route.

### 1.1 List of issues

- **Analysis:** Define the domain rules for `FlightRoute`, including route name format and uniqueness constraints.
- **Design:** Define the architecture for flight route creation — domain model, persistence, and layers.
- **Implement:** Implement the `FlightRoute` aggregate, `RouteName` value object, repository, controller, and UI.
- **Test:** Unit tests for `FlightRoute` and `RouteName` (domain package coverage above 90%).

---

## 2. Requirements

**US073** As an Air Transport Company Collaborator, I want to add a flight route for my company, so that pilots can use it as the basis for creating flight plans.

**Acceptance Criteria:**

- **AC073.1** A route must be defined between exactly two airports (origin and destination), both already registered in the system.
- **AC073.2** Origin and destination airports must be different.
- **AC073.3** The route name must follow the format: 2 uppercase letters (company initials) followed by 1 to 4 digits (e.g. `TP123`). The format is validated by the `RouteName` value object.
- **AC073.4** The route name must be unique within the system.
- **AC073.5** Only an authenticated Air Transport Company Collaborator (`ATCC` role) may perform this action.
- **AC073.6** The collaborator can only create routes for their own company — the company is derived from the authenticated user's session, not provided as input.

**Dependencies/References:**

- **US030** — Authentication and authorization must be in place (role `ATCC`).
- **US052** — Create an Airport. Both airports referenced by the route must already be registered.
- **US060** — Register an Air Transport Company. The company must exist before a route can be created.

---

## 3. Analysis

A flight route represents a named connection between two airports operated by a specific air transport company.

The main design decisions taken were:

**`RouteName` as a Value Object** — The project document specifies that a route name consists of the company's 2-letter initials followed by up to 4 numeric digits (e.g. `TP123`). A `RouteName` Value Object was created to encapsulate and enforce this format via regex validation (`[A-Z]{2}[0-9]{1,4}`). The `RouteName` is the natural business identity of the `FlightRoute` aggregate — consistent with how other business identities are modelled in the domain (e.g. `AirportIATACode`, `MecanographicNumber`, `RegistrationNumber`). This satisfies the DDD principle "business identity as Value Objects" (CO3).

**Route status** — A route can be `ACTIVE` or `INACTIVE`. A route is always created as `ACTIVE`. Deactivation is handled by US074. The `FlightRouteStatus` enum enables soft deactivation without deleting the aggregate — past references from flight plans remain valid.

**Airport references by identity** — The route references airports via `AirportIATACode` value objects rather than full `Airport` object references. This keeps the coupling between `FlightRoute` and `Airport` aggregates low — a DDD Low Coupling principle.

**Uniqueness** — Route name uniqueness is enforced at the controller level via a pre-check through the repository, and also at the database level with a unique constraint, to prevent race conditions.

**Company binding** — The company is not an input: it is resolved from the currently authenticated user's `ATCC` session. This guarantees AC073.6 structurally.

The main classes identified are:

| Class | Type | Responsibility |
|-------|------|----------------|
| `FlightRoute` | Entity / Aggregate Root | Holds route data and enforces invariants |
| `RouteName` | Value Object / Identity | Route name format validation (`[A-Z]{2}[0-9]{1,4}`) |
| `FlightRouteStatus` | Enum | `ACTIVE` / `INACTIVE` |
| `FlightRouteRepository` | Repository Interface | Persistence contract |
| `CreateFlightRouteController` | Application Controller | Orchestrates the use case; enforces `ATCC` role |
| `CreateFlightRouteUI` | UI | Collects route name, origin and destination from the user |

The following diagram shows the domain model excerpt relevant to this US:

![Domain Model](svg/US073-domain-model.svg)

---

