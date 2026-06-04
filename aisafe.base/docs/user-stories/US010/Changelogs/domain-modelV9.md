# Domain Model Changelog

## V9 - Sprint 3 (4 June 2026)

The following changes were made after implementing US043 (Consult Weather Data) and comparing the weather model with the actual DDD implementation.

---

### 1. `AirControlAreaCode` value object added to `AirControlArea Aggregate`

**Before:**
```
package "AirControlArea Aggregate" {
    class AirControlArea <<entity, aggregate root>> {
        areaCode
        name
        minimumFuelRequired
    }
}
```

**After:**
```
package "AirControlArea Aggregate" {
    class AirControlArea <<entity, aggregate root>> {
        name
        minimumFuelRequired
    }

    class AirControlAreaCode <<value object>> {
        value
    }
}

AirControlArea "1" *-- "1" AirControlAreaCode : "identified by"
```

**Reason:** In the implementation, `AirControlArea` is identified by the `AirControlAreaCode` value object, not by a plain string attribute. Modelling it explicitly is more consistent with DDD tactical patterns and with the rest of the model, where business identities are represented as value objects.

---

### 2. `WeatherData` reference to `AirControlArea` corrected

**Before:**
```
WeatherData "1" --> "1" AirControlArea : "recorded for"
```

**After:**
```
WeatherData "1" --> "1" AirControlAreaCode : "recorded for"
WeatherData "1" ..> "1" AirControlArea : "area exists"
```

**Reason:** `WeatherData` stores an `AirControlAreaCode` external reference. It does not hold a direct reference to the `AirControlArea` aggregate. This keeps the `WeatherData` aggregate independent from the `AirControlArea` aggregate lifecycle and follows the existing low-coupling pattern used across the project. The application layer validates that the referenced area exists before registering, importing, or consulting weather data.

---

### 3. `WeatherData.areaCode` attribute added

**Before:**
```
class WeatherData <<entity, aggregate root>> {
    date
    windSpeed
    windDirection
    temperature
    pressure
    visibility
}
```

**After:**
```
class WeatherData <<entity, aggregate root>> {
    areaCode
    date
    windSpeed
    windDirection
    temperature
    pressure
    visibility
}
```

**Reason:** The area code is part of the weather record identity context. US043 queries weather data by day and air control area, so the model must make the area-code reference visible.

---

## Summary of changes

| Change | Type |
|--------|------|
| `AirControlAreaCode <<value object>>` added to `AirControlArea Aggregate` | Correction |
| `AirControlArea "1" *-- "1" AirControlAreaCode : "identified by"` added | Correction |
| `WeatherData.areaCode` added | Correction |
| `WeatherData --> AirControlArea` direct association replaced by `WeatherData --> AirControlAreaCode` | Correction |
| `WeatherData ..> AirControlArea : "area exists"` dependency added | Clarification |
