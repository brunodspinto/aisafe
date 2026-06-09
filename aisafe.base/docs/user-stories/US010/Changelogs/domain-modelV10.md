# Domain Model Changelog

## V10 - Sprint 3 (9 June 2026)

The following changes were made after implementing US075 (Add a pilot) and US082 (Insert weather data in a flight), to align the domain model with these implemented user stories.

---

### 1. `PILOT` value added to the `RoleType` enumeration

**Before:**
```
Enum RoleType {
    ADMIN
    BACKOFFICE_OPERATOR
    ATCC
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
    PILOT
    FLIGHT_CONTROL_OPERATOR
    WEATHER_PERSON
}
```

**Reason:** US075 (Add a pilot) creates a pilot as a system user with the `PILOT` role. The role is one of the six actors identified in section 3.1 of the requirements document and is defined in the implementation (`AiSafeRoles.PILOT`), but it was missing from the model's `RoleType` enumeration. Adding it makes the `Pilot --> User` ("is a") relationship coherent — a pilot is a `User` holding the `PILOT` role.

---

### 2. `FlightPlan --> WeatherData` association added

**Before:**
```
(no association between FlightPlan and WeatherData)
```

**After:**
```
FlightPlan "1" --> "0..*" WeatherData : "uses"
```

**Reason:** US082 (Insert weather data in a flight) lets a pilot attach weather data to a flight plan. In the implementation, `FlightPlan` holds a `Set<Long>` of `WeatherData` identities (`@ElementCollection`), so a flight plan uses zero or more weather records. The reference is kept by identity (low coupling), consistent with the other cross-aggregate references in the model. The model previously had no link between the two, so US082 was not represented.

---

## Summary of changes

| Change | Type |
|--------|------|
| `PILOT` added to `RoleType` enum | Correction |
| `FlightPlan "1" --> "0..*" WeatherData : "uses"` association added | Enhancement |
