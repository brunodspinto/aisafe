# US077 — Remove a Pilot

## 1. Context

This US is being implemented for the first time in Sprint 3. It allows an **Air Transport Company Collaborator (ATCC)** to make a pilot inactive in their company's roster. A pilot that is made inactive is not deleted from the system — the record is preserved for historical traceability, but the pilot can no longer be assigned to new flight plans.

`Pilot` is an existing aggregate (US075). This US adds deactivation behaviour to that aggregate.

### 1.1 List of issues

- **Requirements:** Define the rules for pilot deactivation, including the blocking constraint on active flight plans.
- **Analysis:** Identify what "flight plans assigned" means and which domain objects are involved.
- **Design:** Sequence diagram and class diagram for the deactivation flow.
- **Implement:** Add `deactivate()` to the domain, controller, UI and wire into the menu.
- **Test:** Unit tests for domain deactivation + integration tests for the controller.

---

## 2. Requirements

**US077** As an Air Transport Company Collaborator, I want to make a pilot inactive in my company's roster.

**Acceptance Criteria:**

- **AC077.1** Only an authenticated Air Transport Company Collaborator (`ATCC` role) may perform this action.
- **AC077.2** The pilot is made **inactive** — the record is not deleted. An inactive pilot does not appear on the active roster.
- **AC077.3** A pilot that has flight plans assigned **cannot** be deactivated.
- **AC077.4** The collaborator can only deactivate pilots of their own company — the company is derived from the authenticated user's session.

**Dependencies/References:**

- **US030** — Authentication and authorization must be in place (role `ATCC`).
- **US075** — Add a Pilot. A pilot must exist and be active before it can be deactivated.


---
