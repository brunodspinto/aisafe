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
