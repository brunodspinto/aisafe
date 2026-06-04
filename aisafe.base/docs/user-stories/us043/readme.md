# US043 — Consult Weather Data

## 1. Context

This user story allows authorized users (Weather Person, Pilot, Flight Control Operator) to query and view weather data for a specific date and air control area. Access to accurate meteorological information is critical for making safe and efficient decisions regarding flight planning and operations.

---

## 2. Requirements

**US043** As a Weather Person, a Pilot, or a Flight Control Operator, I want to consult weather data in the system for a given day and a specific air control area so that I can make informed decisions about flight operations.

**Acceptance Criteria:**

- **AC043.1** Weather data can be queried by a specific date and a selected air control area.
- **AC043.2** The query results are displayed clearly and include all relevant meteorological information (e.g., temperature, wind speed, precipitation).
- **AC043.3** Access to this feature is restricted to users with the roles: `WEATHER_PERSON`, `PILOT`, or `FLIGHT_CONTROL_OPERATOR`.

**Dependencies/References:**

- This US depends on **US030** for user authentication and role-based authorization.
- It assumes the existence of `WeatherData` and `AirControlArea` entities in the system.

---
## 3. Analysis

This feature is a query-based capability that allows authorized operational users to retrieve weather records already stored in the system. It reuses the `WeatherData` aggregate created in US041 and the bulk import capability from US042 as upstream data sources. No new domain aggregate is required for US043.

Following the existing DDD model, `WeatherData` remains the aggregate root for meteorological readings and stores an `AirControlAreaCode` as an external reference instead of holding an `AirControlArea` entity. This keeps the weather data aggregate independent from the air control area aggregate while still allowing queries by area code. The application layer is responsible for validating that the requested area exists before executing the query.

The query is day-based, while `WeatherData` stores a `LocalDateTime`. Therefore, the repository contract must retrieve all records whose timestamp falls within the selected day, from `00:00` inclusive to the following day at `00:00` exclusive.

Authorization is handled in the application controller using the existing `AuthorizationService`. Access is granted only to `WEATHER_PERSON`, `PILOT`, and `FLIGHT_CONTROL_OPERATOR`, as required by AC043.3.

The main classes involved are:

| Class | Type | Responsibility |
|-------|------|----------------|
| `WeatherData` | Entity / Aggregate Root | Existing - represents meteorological readings for one air control area at a specific timestamp. |
| `AirControlAreaCode` | Value Object | Existing - identifies the air control area referenced by the weather record. |
| `AirControlArea` | Entity / Aggregate Root | Existing - provides the selectable list of valid air control areas. |
| `AirControlAreaRepository` | Repository Interface | Existing - validates area existence and lists available areas. |
| `WeatherDataRepository` | Repository Interface | Extended with `findByDateAndAirControlArea(date, areaCode)`. |
| `ConsultWeatherDataController` | Application Controller | New - enforces role access, validates area existence, and delegates the weather query. |
| `ConsultWeatherDataUI` | UI | New - prompts for date/area and displays all relevant meteorological information. |

---

## 4. Design

### 4.1. Realization

1.  The `ConsultWeatherDataUI` will first call the controller to get a list of all available `AirControlArea`s to present to the user.
2.  The user selects an area and provides a date.
3.  The controller validates that the authenticated user has one of the required roles (`WEATHER_PERSON`, `PILOT`, `FLIGHT_CONTROL_OPERATOR`).
4.  The controller then calls a new method in the `WeatherDataRepository` to fetch the data.
5.  The UI renders the results in a formatted table.

The following sequence diagram illustrates the flow:

```mermaid
sequenceDiagram
    actor User
    participant UI
    participant ConsultWeatherDataController
    participant WeatherDataRepository

    User->>UI: Selects "Consult Weather Data"
    UI->>ConsultWeatherDataController: getAirControlAreas()
    ConsultWeatherDataController->>WeatherDataRepository: findAllAirControlAreas()
    WeatherDataRepository-->>ConsultWeatherDataController: returns List<AirControlArea>
    ConsultWeatherDataController-->>UI: returns List<AirControlArea>
    UI->>User: Shows list of areas and asks for date
    User->>UI: Selects area and provides date
    UI->>ConsultWeatherDataController: getWeatherData(date, area)
    ConsultWeatherDataController->>ConsultWeatherDataController: Authorize user role
    ConsultWeatherDataController->>WeatherDataRepository: findByDateAndAirControlArea(date, area)
    WeatherDataRepository-->>ConsultWeatherDataController: Returns List<WeatherData>
    ConsultWeatherDataController-->>UI: Displays weather data
    UI-->>User: Shows formatted weather information
```

### 4.2. Acceptance Tests

| Test ID | Description | Expected Result |
|---------|-------------|-----------------|
| AC043.1 | Query with a valid date and area with existing data. | Weather data for that day and area is displayed. |
| AC043.1b| Query for a date/area with no data. | "No weather data found" message is shown. |
| AC043.3 | A user without the required role attempts to access. | Access is denied with an authorization error. |

---

## 5. Implementation

*(To be detailed in a future phase)*

---

## 6. Integration/Demonstration

*(To be detailed in a future phase)*

---

## 7. Observations

*(To be detailed in a future phase)*
