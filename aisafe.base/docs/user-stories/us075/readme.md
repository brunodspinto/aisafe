# US075

## 1. Context

This US is being developed for the first time in Sprint 3. It allows an Air Transport Company Collaborator (ATCC) to add a pilot to their company. A pilot is a system user with the PILOT role, certified to operate one or more aircraft models.

### 1.1 List of issues

Analysis: Define the domain rules for `Pilot`, including certification requirements and company association.

Design: Define the architecture for pilot registration, domain model and persistence.

Implement: Implement the `Pilot` aggregate, repository, controller and UI.

Test: Unit tests for `Pilot` (all above 90% coverage).

---

## 2. Requirements

**US075** As an Air Transport Company Collaborator, I want to add a pilot to my company. A pilot is a system's user. A pilot is certified to pilot one or more aircraft models.

**Acceptance Criteria:**

- **AC075.1** A pilot must be a system user with the PILOT role.
- **AC075.2** A pilot must belong to exactly one air transport company — the company of the authenticated collaborator.
- **AC075.3** A pilot must be certified for at least one aircraft model.
- **AC075.4** All aircraft models referenced in the certifications must already exist in the system.
- **AC075.5** A newly added pilot is active by default.

**Dependencies/References:**

- **US055** — Create an Aircraft Model. The aircraft models referenced in the pilot's certifications must already be registered.
- **US060** — Register an Air Transport Company. The company must exist before a pilot can be added.
- **US061** — Add a Customer's Collaborator. The authenticated user must be a company collaborator (ATCC).

---

## 3. Analysis

A pilot is a system user with the PILOT role that belongs to an air transport company and is certified to operate one or more aircraft models.

The main design decisions made were:

**Pilot as separate aggregate** — The `Pilot` aggregate was created separately from `Collaborator` because a pilot has distinct domain behaviour — certifications for aircraft models and an active/inactive status. This keeps each aggregate focused on a single responsibility.

**Company reference by identity** — The `Pilot` references the company via `IATACode` (identity) rather than a full `AirTransportCompany` object reference. This avoids cross-aggregate object references — a DDD principle of low coupling between aggregates.

**Aircraft model reference by identity** — The pilot's certifications are stored as a `Set<Long>` of aircraft model ids rather than full `AircraftModel` references. This keeps the coupling between `Pilot` and `AircraftModel` aggregates low.

**Authentication-based company resolution** — The controller resolves the authenticated collaborator's company automatically from the session — the ATCC does not manually select a company. This enforces AC075.2 — a pilot can only be added to the authenticated collaborator's own company.

**Certification invariant** — A pilot must always have at least one certification. This is enforced in the constructor and the set is exposed as unmodifiable.

The main classes identified are:

| Class | Type | Responsibility |
|-------|------|----------------|
| `Pilot` | Entity / Aggregate Root | Holds pilot data and certifications |
| `PilotRepository` | Repository Interface | Persistence contract |
| `AddPilotController` | Controller | Use case orchestrator |
| `AddPilotUI` | UI | Console UI for pilot registration |

The following diagram shows the domain model excerpt relevant to this US:

![Domain Model](svg/US075-domain-model.svg)

---
