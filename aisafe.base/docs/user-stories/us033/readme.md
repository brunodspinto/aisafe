# US033 — List Backoffice Users

## 1. Context

This US was implemented in Sprint 2 and allows the Administrator to list all registered backoffice users in the AISafe system. It depends on US030 (Authentication and Authorization) and US031 (Register Users), which must be in place so that only an authenticated Admin can invoke this feature and users exist to be listed.

No new domain entities were introduced. The listing reuses the existing `User` aggregate and `UserRepository`.

---

## 2. Requirements

**US033** As Administrator, I want to be able to list all users of the backoffice.

**Acceptance Criteria:**

- **AC033.1** The system must display all registered users, including both active and inactive.
- **AC033.2** Each user entry must show: username, status (ACTIVE / INACTIVE), first name, last name, email, and position.
- **AC033.3** Only an authenticated Administrator may access this feature.

**Dependencies/References:**

- US030 — Authentication and Authorization must be implemented first.
- US031 — Register Users must be implemented first (users must exist to be listed).

---

## 3. Analysis

No new aggregates were created. The `User` aggregate already holds all the fields needed for display. The `UserRepository.findAll()` method returns all users regardless of their active/inactive state, satisfying AC033.1.

The main classes involved are:

| Class | Type | Responsibility |
|-------|------|----------------|
| `User` | Entity / Aggregate Root | AISafe-specific user data; references `SystemUser` |
| `SystemUser` | EAPLI entity | Owns the `active` flag; exposes `isActive()` |
| `UserRepository` | Repository interface | Provides `findAll()` |
| `ListUsersController` | Application controller | Orchestrates the use case; enforces ADMIN role |
| `ListUsersUI` | UI | Retrieves and displays the user list in a formatted table |

The following sequence diagram illustrates the flow:

![Sequence Diagram](svg/US033-SD.svg)

The following class diagram shows the classes involved:

![Class Diagram](svg/US033-class-diagram.svg)

---

## 4. Design

### 4.1. Realization

1. The UI calls `controller.allUsers()`.
2. The controller verifies the authenticated user has the ADMIN role via `authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN)`.
3. The controller calls `userRepo.findAll()` and returns the result.
4. The UI iterates over the returned users and prints a formatted table showing username, status, first name, last name, email, and position.

`findAll()` is used instead of `findAllActive()` so that disabled users are also shown and can be identified (AC033.1).

### 4.2. Acceptance Tests

US033 is covered by a focused unit test and the manual acceptance scenarios documented in [tests.md](tests.md).

- `src/test/java/aisafe/usermanagement/application/ListUsersControllerTest.java` verifies that an authenticated Administrator can list every backoffice user, including inactive accounts, and that non-admin users are rejected.
- `src/test/java/aisafe/usermanagement/domain/UserTest.java` covers the `User` aggregate fields displayed by this use case.
- `src/test/java/aisafe/usermanagement/domain/DisableEnableUserTest.java` supports the active/inactive status checks used by the listing.

---

## 5. Implementation

The implementation is distributed across the following packages in `aisafe.base`:

| Package | Class | Role |
|---------|-------|------|
| `aisafe.usermanagement.application` | `ListUsersController` | Use case orchestrator; enforces ADMIN role |
| `aisafe.app.console.presentation.authz` | `ListUsersUI` | Console UI; displays the formatted user table |
| `aisafe.app.console.presentation` | `MainMenu` | Option 2 "List Users" under the Users submenu |

No changes were required to the domain layer or repositories.

---

## 6. Integration/Demonstration

**Prerequisites:** Run from the `aisafe.base` directory with Maven 3.9+ and Java 21.

**To list all users:**

1. Login with admin credentials (`admin` / `Password1`).
2. Select **2 — Users >** from the main menu.
3. Select **2 — List Users**.
4. The system displays a table with all registered users and their current status.

Expected output format:

```
Username             Status   First Name      Last Name       Email                          Position
----------------------------------------------------------------------------------------------------
admin                ACTIVE   Admin           User            admin@aisafe.com               Administrator
operator             ACTIVE   Op              User            operator@aisafe.com            Operator
pilot1               DISABLED Pilot           One             pilot1@aisafe.com              Pilot
```

---

## 7. Observations

- `userRepo.findAll()` is used (not `findAllActive()`) so that disabled users are also visible in the list and can be identified for re-enabling via US032.
- The status label is derived at the UI layer from `user.systemUser().isActive()`: `true` → `ACTIVE`, `false` → `INACTIVE`.
- The `Users >` submenu, including this option, is only rendered when the authenticated user has the ADMIN role (enforced in `MainMenu.buildMainMenu()`).
