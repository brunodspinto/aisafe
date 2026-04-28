# US030 - Authentication and Authorization

## What This Does

Protects flight plan operations by checking user roles before allowing actions. Different users have different permissions:

- **ADMIN** - Full access (create, read, update, delete, approve)
- **BACKOFFICE_OPERATOR** - Can create/edit flight plans
- **ATCC** - Can approve flight plans
- **PILOT** - Can view flight plans (read-only)
- **FLIGHT_CONTROL_OPERATOR** - Can approve flight plans
- **WEATHER_PERSON** - Can view flight plans (read-only)

---

## How It Works

### 1. Set Authenticated User (After Login)
```java
AuthenticationContext.setCurrentUser(systemUser);
```

### 2. Use Flight Plan Service (Authorization Automatic)
```java
// This automatically checks if user can create
service.saveFlightPlanDsl(flightPlanDsl);

// If user doesn't have permission, throws:
// UnauthorizedException: "User does not have permission to create flight plans"
```

### 3. Clear User (On Logout)
```java
AuthenticationContext.clear();
```

---

## Files

### Core Classes (aisafe.auth package)
- **FlightPlanRoles.java** - Defines the 6 roles
- **AuthenticationContext.java** - Stores current logged-in user (thread-local)
- **AuthorizationService.java** - Checks permissions for flight operations
- **UnauthorizedException.java** - Thrown when access denied

### Integration
- **FlightPlanPersistenceService.java** - Calls authorization checks automatically

---

## Permission Matrix

| Role | Create | Read | Update | Delete | Approve |
|------|--------|------|--------|--------|---------|
| ADMIN | ✓ | ✓ | ✓ | ✓ | ✓ |
| BACKOFFICE_OPERATOR | ✓ | ✓ | ✓ | - | - |
| ATCC | - | ✓ | - | - | ✓ |
| PILOT | - | ✓ | - | - | - |
| FLIGHT_CONTROL_OPERATOR | - | ✓ | - | - | ✓ |
| WEATHER_PERSON | - | ✓ | - | - | - |

---

## Authorization Checks (in AuthorizationService)

```
requireCanCreateFlightPlan()    → Needs ADMIN or BACKOFFICE_OPERATOR
requireCanReadFlightPlan()      → Needs any authenticated user
requireCanUpdateFlightPlan()    → Needs ADMIN or BACKOFFICE_OPERATOR
requireCanDeleteFlightPlan()    → Needs ADMIN only
requireCanApproveFlightPlan()   → Needs ADMIN, ATCC, or FLIGHT_CONTROL_OPERATOR
```

---

## How Roles Come From eapli.base

Roles are defined centrally in eapli.base:
- Location: `eapli.base/exemplo.core/usermanagement/domain/AiSafeRoles.java`
- FlightPlanRoles.java simply re-exports them from there
- All AISafe modules use the same 6 roles

---

## Example

```java
// User logs in
SystemUser user = loginService.authenticate("john", "password");
AuthenticationContext.setCurrentUser(user);

// User tries to create flight plan
try {
    FlightPlanSnapshot saved = persistenceService.saveFlightPlanDsl(dslString);
} catch (UnauthorizedException e) {
    // "User does not have permission to create flight plans. 
    //  Required roles: ADMIN, BACKOFFICE_OPERATOR"
}

// User logs out
AuthenticationContext.clear();
```

---

## Is It Production Ready?

**For console/testing apps:** Yes! Use the AuthenticationContext to store user sessions.

**For web apps (Spring):** Usually handled by Spring Security. Our AuthorizationService still works automatically—just don't use AuthenticationContext directly.

**For REST APIs:** Same as web apps, but add `@RequiredRole` annotations or call AuthorizationService checks.

---

## Dependencies

Uses:
- `eapli.framework.core` - SystemUser, Role abstractions
- `eapli.framework.infrastructure.authz` - Authentication/authorization framework
