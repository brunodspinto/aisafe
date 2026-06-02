# US073 — Tests and Coverage

## Scope

US073 covers creating a flight route with a unique route name, two different registered airports, and automatic association to the authenticated collaborator's company.

## Automated Tests

### `FlightRouteTest`

Location: `src/test/java/aisafe/flightroute/domain/FlightRouteTest.java`

**Creation and validation:**
- `ensureValidFlightRouteCanBeCreated`
- `ensureRouteNameCannotBeNull`
- `ensureOriginAirportCannotBeNull`
- `ensureDestinationAirportCannotBeNull`
- `ensureCompanyCannotBeNull`
- `ensureOriginAndDestinationCannotBeTheSame`

**Status:**
- `ensureStatusStartsAsActive`
- `ensureIsActiveReturnsTrueForNewRoute`

**Identity and equality:**
- `ensureIdentityReturnsRouteName`
- `ensureTwoRoutesWithSameNameAreEqual`
- `ensureTwoRoutesWithDifferentNamesAreNotEqual`
- `ensureEqualsReturnsTrueForSameInstance`
- `ensureEqualsReturnsFalseForNull`
- `ensureHashCodeIsConsistentWithEquals`
- `ensureSameAsReturnsTrueForEqualRoutes`
- `ensureToStringContainsRouteName`

### `RouteNameTest`

Location: `src/test/java/aisafe/flightroute/domain/RouteNameTest.java`

**Format validation:**
- `ensureValidRouteNameIsAccepted`
- `ensureMinimumValidRouteNameIsAccepted`
- `ensureMaximumValidRouteNameIsAccepted`
- `ensureRouteNameCannotBeNull`
- `ensureRouteNameCannotBeBlank`
- `ensureRouteNameWithOnlyOneLetterIsRejected`
- `ensureRouteNameWithMoreThanTwoLettersIsRejected`
- `ensureRouteNameWithNoDigitsIsRejected`
- `ensureRouteNameWithMoreThanFourDigitsIsRejected`
- `ensureRouteNameWithLowercaseLettersIsRejected`
- `ensureRouteNameWithSpecialCharactersIsRejected`
- `ensureRouteNameWithOnlyLettersIsRejected`

**Identity and equality:**
- `ensureTwoRouteNamesWithSameValueAreEqual`
- `ensureTwoRouteNamesWithDifferentValuesAreNotEqual`
- `ensureEqualsReturnsTrueForSameInstance`
- `ensureEqualsReturnsFalseForNull`
- `ensureEqualsReturnsFalseForDifferentType`
- `ensureHashCodeIsConsistentWithEquals`
- `ensureToStringReturnsName`

## Coverage by Acceptance Criterion

- **AC073.1** (route references two registered airports): `ensureOriginAirportCannotBeNull`, `ensureDestinationAirportCannotBeNull`
- **AC073.2** (origin and destination must be different): `ensureOriginAndDestinationCannotBeTheSame`
- **AC073.3** (route name format `[A-Z]{2}[0-9]{1,4}`): `ensureValidRouteNameIsAccepted`, `ensureMinimumValidRouteNameIsAccepted`, `ensureMaximumValidRouteNameIsAccepted`, `ensureRouteNameWithOnlyOneLetterIsRejected`, `ensureRouteNameWithMoreThanTwoLettersIsRejected`, `ensureRouteNameWithNoDigitsIsRejected`, `ensureRouteNameWithMoreThanFourDigitsIsRejected`, `ensureRouteNameWithLowercaseLettersIsRejected`, `ensureRouteNameWithSpecialCharactersIsRejected`
- **AC073.4** (route name must be unique): `ensureCreateFlightRouteThrowsForDuplicateRouteName` (controller integration test); also enforced at DB level by `@UniqueConstraint` on `T_FLIGHT_ROUTE.route_name`
- **AC073.5** (only ATCC may perform this action): `ensureAllAirportsThrowsWhenWrongRole`, `ensureCreateFlightRouteThrowsWhenWrongRole`, `ensureAllAirportsThrowsWhenNotAuthenticated`, `ensureCreateFlightRouteThrowsWhenNotAuthenticated` (controller integration tests)
- **AC073.6** (company derived from session): `ensureRouteIsAssociatedWithAuthenticatedCollaboratorsCompany` (controller integration test)
- **Status starts as ACTIVE**: `ensureStatusStartsAsActive`, `ensureIsActiveReturnsTrueForNewRoute`
- **Identity and value semantics**: `ensureIdentityReturnsRouteName`, `ensureTwoRoutesWithSameNameAreEqual`, `ensureTwoRoutesWithDifferentNamesAreNotEqual`, `ensureEqualsReturnsTrueForSameInstance`, `ensureEqualsReturnsFalseForNull`, `ensureHashCodeIsConsistentWithEquals`, `ensureSameAsReturnsTrueForEqualRoutes`, `ensureToStringContainsRouteName`

### `CreateFlightRouteControllerTest`

Location: `src/test/java/aisafe/flightroute/application/CreateFlightRouteControllerTest.java`

**Authorization — no session:**
- `ensureAllAirportsThrowsWhenNotAuthenticated`
- `ensureCreateFlightRouteThrowsWhenNotAuthenticated`

**Authorization — wrong role:**
- `ensureAllAirportsThrowsWhenWrongRole`
- `ensureCreateFlightRouteThrowsWhenWrongRole`

**allAirports():**
- `ensureAllAirportsReturnsAtLeastTheSeededAirports`

**createFlightRoute() — happy path:**
- `ensureCreateFlightRouteReturnsPersistedRoute`
- `ensureCreatedRouteIsPersisted`
- `ensureCreateFlightRouteWithWhitespacePaddedInputsSucceeds`
- `ensureRouteIsAssociatedWithAuthenticatedCollaboratorsCompany`
- `ensureMultipleRoutesCanBeCreatedForSameCompany`

**createFlightRoute() — validation:**
- `ensureCreateFlightRouteThrowsForDuplicateRouteName`
- `ensureCreateFlightRouteThrowsForUnknownOriginAirport`
- `ensureCreateFlightRouteThrowsForUnknownDestinationAirport`
- `ensureCreateFlightRouteThrowsForSameOriginAndDestination`
- `ensureCreateFlightRouteThrowsForInvalidRouteName`

Total: **16 domain tests** (`FlightRouteTest`) + **19 domain tests** (`RouteNameTest`) + **15 integration tests** (`CreateFlightRouteControllerTest`) = **50 automated tests**, all passing.

---

## Acceptance Tests

Authorization (ATCC role), route name uniqueness, and company binding from the authenticated session are infrastructure concerns validated through manual integration testing.

**Manual test — AC073.1 (valid route created):**

1. Run the app and login as an ATCC collaborator.
2. Navigate to `Flight Routes > Create Flight Route`.
3. Provide: route name `TP123`, origin `OPO`, destination `LIS`.
4. Expected: confirmation message `Flight route successfully created!` with all details.

**Manual test — AC073.2 (origin equals destination rejected):**

1. At the origin prompt enter `OPO` and at the destination prompt also enter `OPO`.
2. Expected: the system rejects the operation with a message indicating origin and destination must be different.

**Manual test — AC073.3 (invalid route name format rejected):**

1. At the route name prompt enter `T123` (one letter) or `TAP123` (three letters) or `TP12345` (five digits).
2. Expected: the UI rejects the input with a format validation error and re-prompts.

**Manual test — AC073.4 (duplicate route name rejected):**

1. Register a route with name `TP123`.
2. Attempt to register a second route with the same name `TP123`.
3. Expected: the system rejects the operation with a uniqueness violation message.

**Manual test — AC073.5 (role enforcement):**

1. Login as a user without the ATCC role (e.g. Backoffice Operator).
2. Expected: the `Flight Routes` menu is not available.

**Manual test — AC073.6 (company binding from session):**

1. Login as an ATCC of company `TP`.
2. Create a route `TP123`.
3. Expected: the route is associated with company `TP` automatically — no company selection required.
