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