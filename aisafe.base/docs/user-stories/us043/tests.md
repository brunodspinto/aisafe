# US043 - Acceptance Tests

## Unit Tests

| Test | Scope | Expected Result |
|------|-------|-----------------|
| `activeAirControlAreasReturnsRepositoryAreas` | Controller unit | The controller returns the injected repository's air control areas and invokes the authorization guard once. |
| `consultWeatherDataReturnsOnlySelectedDateAndArea` | Controller unit | The controller validates the selected area and returns only weather records for the selected day and area. |
| `consultWeatherDataRejectsNullDate` | Controller unit | A null date is rejected before querying weather data. |
| `consultWeatherDataRejectsUnknownAreaCode` | Controller unit | An unknown air control area code is rejected before querying weather data. |
| `consultWeatherDataStopsWhenAuthorizationFails` | Controller unit | A failing authorization guard stops the use case before validation/query execution. |

## Acceptance/Integration Tests

| Test | Acceptance Criteria | Expected Result |
|------|---------------------|-----------------|
| `ensureWeatherDataCanBeQueriedByDateAndArea` | AC043.1 | Only records matching the selected day and air control area are returned. |
| `ensureEmptyResultIsReturnedWhenNoWeatherDataExists` | AC043.1b | The controller returns an empty result for date/area combinations without records. |
| `ensureReturnedWeatherDataIncludesMeteorologicalInformation` | AC043.2 | Returned records expose source, timestamp, temperature, wind, pressure, and visibility. |
| `ensureFlightControlOperatorCanConsultWeatherData` | AC043.3 | Flight Control Operator users can access the consultation flow. |
| `ensureUnauthorizedUserCannotConsultWeatherData` | AC043.3 | Users without the required roles receive an authorization exception. |
| `ensureUnknownAreaCodeIsRejected` | AC043.1 | Unknown area codes are rejected before querying weather data. |

## Implementation Tests

| Test | Flow Covered | Expected Result |
|------|--------------|-----------------|
| `ensureWeatherPersonCanRunTheWholeConsultWeatherDataFlow` | Authenticated Weather Person lists areas, selects date/area, consults weather data, and receives full meteorological fields. | The selected area is available, only matching date/area records are returned, and all relevant fields are present. |
| `ensurePilotCanRunTheConsultWeatherDataFlow` | Authenticated Pilot runs the same consult flow. | Pilot role can list areas and consult records for the selected day and area. |
| `ensureUnauthorizedUserCannotRunTheConsultWeatherDataFlow` | Backoffice user attempts the consult flow. | The flow is stopped by authorization before area listing or data consultation. |

## Command

```bash
mvn test -Dtest=ConsultWeatherDataControllerUnitTest
mvn test -Dtest=ConsultWeatherDataControllerTest
mvn test -Dtest=ConsultWeatherDataImplementationTest
```

## Execution Notes

- `mvn -q -DskipTests compile` succeeds, confirming the US043 main implementation compiles.
- `mvn test` succeeds with 659 tests run, 0 failures, 0 errors, and 0 skipped.

## Manual Scenario

1. Login as a Weather Person, Pilot, or Flight Control Operator.
2. Open **Weather > Consult Weather Data**.
3. Select an existing air control area code.
4. Enter a day in `yyyy-MM-dd` format.
5. Confirm that matching weather data is shown in a table with source, date/time, temperature, wind speed, wind direction, pressure, and visibility.
6. Repeat with a date that has no records and confirm that the UI reports no weather data found.
7. Login as a user without the allowed roles and confirm that the Weather consultation option is not exposed, or that direct controller access is rejected.
