# Domain Model Changelog

## V8 — Sprint 3 (2 June 2026)

The following changes were made to support the implementation of US075 (Add a Pilot) and US086 (Pilot Remote Access) in Sprint 3.

---

### 1. `Pilot Aggregate` added

**Before:**
```
(no Pilot aggregate — pilots were implicitly represented as Users with the PILOT role)
```

**After:**
```
package "Pilot Aggregate" {
    class Pilot <<entity, aggregate root>> {
        active
    }
}

Pilot "1" --> "1" User : "is a"
Pilot "0..*" --> "1" AirTransportCompany : "belongs to"
Pilot "1" --> "1..*" AircraftModel : "certified for"
```

**Reason:** The V7 model represented pilots implicitly as `User` instances with the `PILOT` role, using a generic `User --> AircraftModel : "certified for"` relationship. This was a simplification that mixed two distinct business concepts. A Pilot is not just a User with a role — it has its own lifecycle (`active`/inactive), belongs to a specific air transport company, and holds certifications for one or more aircraft models. These are domain rules that only apply to Pilots and not to any other User type (e.g., a Backoffice Operator also has the `User` aggregate but has no certifications or company assignment). The `Pilot` aggregate encapsulates these rules and makes the domain model more accurate and expressive, consistent with the DDD principle of modelling distinct business concepts as distinct aggregates (CO3 criterion). The `User --> AircraftModel : "certified for"` generic relationship is removed, as it is now owned exclusively by the `Pilot` aggregate.

---

### 2. `User --> AircraftModel : "certified for"` relationship removed

**Before:**
```
User "0..*" --> "0..*" AircraftModel : "certified for"
```

**After:**
```
(relationship removed — moved to Pilot aggregate)
```

**Reason:** This relationship was a simplification from early modelling. Aircraft model certifications are a business rule that only applies to Pilots, not to all Users. With the introduction of the `Pilot` aggregate, the certification responsibility is correctly owned by `Pilot`, removing the incorrect coupling between the generic `User` aggregate and `AircraftModel`.

---

---

### 3. `PILOT` removed from `RoleType`

**Before:**
```
Enum RoleType {
    ADMIN
    BACKOFFICE_OPERATOR
    ATCC
    PILOT
    FLIGHT_CONTROL_OPERATOR
    WEATHER_PERSON
}
```

**After:**
```
Enum RoleType {
    ADMIN
    BACKOFFICE_OPERATOR
    ATCC
    FLIGHT_CONTROL_OPERATOR
    WEATHER_PERSON
}
```

**Reason:** With the introduction of the `Pilot` aggregate as a first-class domain concept, representing `PILOT` as a value in `RoleType` becomes redundant and confusing. `RoleType` captures authorization roles, which is an infrastructure concern of the EAPLI framework. The `Pilot` aggregate already expresses "being a Pilot" at the domain level — with its own lifecycle (`active`), company assignment, and certifications. Keeping `PILOT` in `RoleType` would mix two distinct concerns: domain identity (who a Pilot *is*) and authorization infrastructure (what access a Pilot *has*). In the domain model, the concept is owned by the aggregate; the EAPLI role (`AiSafeRoles.PILOT`) remains in code as an authorization mechanism but does not belong in the domain model.

---

## Summary of changes

| Change | Type |
|--------|------|
| `Pilot <<entity, aggregate root>>` added to new `Pilot Aggregate` | Addition |
| `Pilot "1" --> "1" User : "is a"` added | Addition |
| `Pilot "0..*" --> "1" AirTransportCompany : "belongs to"` added | Addition |
| `Pilot "1" --> "1..*" AircraftModel : "certified for"` added | Addition |
| `User "0..*" --> "0..*" AircraftModel : "certified for"` removed | Correction |
| `PILOT` removed from `RoleType` | Correction |
