# US070 — Tests and Coverage

## Scope

US070 covers adding an aircraft to the fleet of an Air Transport Company, with a registration number, country, crew size, manufacture year, cabin configuration, and an associated aircraft model.

## Automated Tests

### `AircraftTest`

Location: `src/test/java/aisafe/aircraft/domain/AircraftTest.java`

> Note: tests use helper methods `validModel()`, `validCargoModel()`, `validModelWithCapacity(int)`, and `validCabin()` defined in the same class.

**Test:** `ensureValidAircraftIsCreatedSuccessfully`

```java
@Test
void ensureValidAircraftIsCreatedSuccessfully() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    assertEquals("CS-TUA", aircraft.registrationNumber());
    assertEquals("Portugal", aircraft.registeredCountry());
    assertEquals(6, aircraft.numberOfCrewElements());
}
```

**Test:** `ensureAircraftIsCreatedWithActiveOperationalStatus`

```java
@Test
void ensureAircraftIsCreatedWithActiveOperationalStatus() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    assertEquals(OperationalStatus.ACTIVE, aircraft.operationalStatus());
}
```

**Test:** `ensureAircraftRegistrationIsNormalisedToUpperCase`

```java
@Test
void ensureAircraftRegistrationIsNormalisedToUpperCase() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("cs-tua"), "Portugal", 6, 2018, validCabin(), validModel());
    assertEquals("CS-TUA", aircraft.registrationNumber());
}
```

**Test:** `ensureAircraftMustHaveValidRegistrationNumber`

```java
@Test
void ensureAircraftMustHaveValidRegistrationNumber() {
    assertThrows(IllegalArgumentException.class,
            () -> new Aircraft((RegistrationNumber) null, "Portugal", 6, 2018, validCabin(), validModel()));
}
```

**Test:** `ensureAircraftRegistrationCannotBeEmpty`

```java
@Test
void ensureAircraftRegistrationCannotBeEmpty() {
    assertThrows(IllegalArgumentException.class,
            () -> new Aircraft(RegistrationNumber.valueOf("  "), "Portugal", 6, 2018, validCabin(), validModel()));
}
```

**Test:** `ensureAircraftMustBeRegisteredToACountry`

```java
@Test
void ensureAircraftMustBeRegisteredToACountry() {
    assertThrows(IllegalArgumentException.class,
            () -> new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "  ", 6, 2018, validCabin(), validModel()));
}
```

**Test:** `ensureAircraftCannotExceedModelMaximumCapacity`

```java
@Test
void ensureAircraftCannotExceedModelMaximumCapacity() {
    final AircraftModel model = validModelWithCapacity(100);
    final CabinConfiguration oversized = new CabinConfiguration(0, 0, 150);
    assertThrows(IllegalArgumentException.class,
            () -> new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, oversized, model));
}
```

**Test:** `ensureAircraftWithExactModelCapacityIsAccepted`

```java
@Test
void ensureAircraftWithExactModelCapacityIsAccepted() {
    final AircraftModel model = validModelWithCapacity(189);
    final CabinConfiguration cabin = new CabinConfiguration(0, 0, 189);
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, cabin, model);
    assertEquals(189, aircraft.cabinConfiguration().totalSeats());
}
```

**Test:** `ensureZeroCrewElementsThrows`

```java
@Test
void ensureZeroCrewElementsThrows() {
    assertThrows(IllegalArgumentException.class,
            () -> new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 0, 2018, validCabin(), validModel()));
}
```

**Test:** `ensureNegativeCrewElementsThrows`

```java
@Test
void ensureNegativeCrewElementsThrows() {
    assertThrows(IllegalArgumentException.class,
            () -> new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", -1, 2018, validCabin(), validModel()));
}
```

**Test:** `ensurePassengerAircraftWithNullCabinThrows`

```java
@Test
void ensurePassengerAircraftWithNullCabinThrows() {
    assertThrows(IllegalArgumentException.class,
            () -> new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, null, validModel()));
}
```

**Test:** `ensureCargoAircraftWithNullCabinIsValid`

```java
@Test
void ensureCargoAircraftWithNullCabinIsValid() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-FCA"), "Portugal", 4, 2015, null, validCargoModel());
    assertNull(aircraft.cabinConfiguration());
}
```

**Test:** `ensureCargoAircraftWithNonNullCabinThrows`

```java
@Test
void ensureCargoAircraftWithNonNullCabinThrows() {
    assertThrows(IllegalArgumentException.class,
            () -> new Aircraft(RegistrationNumber.valueOf("CS-FCA"), "Portugal", 4, 2015, validCabin(), validCargoModel()));
}
```

**Test:** `ensureNullAircraftModelThrows`

```java
@Test
void ensureNullAircraftModelThrows() {
    assertThrows(IllegalArgumentException.class,
            () -> new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), null));
}
```

**Test:** `ensureYearOfManufactureIsStored`

```java
@Test
void ensureYearOfManufactureIsStored() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2015, validCabin(), validModel());
    assertEquals(2015, aircraft.yearOfManufacture());
}
```

**Test:** `ensureYearOfManufactureBefore1900IsRejected`

```java
@Test
void ensureYearOfManufactureBefore1900IsRejected() {
    assertThrows(IllegalArgumentException.class,
            () -> new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 1899, validCabin(), validModel()));
}
```

**Test:** `ensureYearOfManufactureInFutureIsRejected`

```java
@Test
void ensureYearOfManufactureInFutureIsRejected() {
    final int futureYear = java.time.Year.now().getValue() + 1;
    assertThrows(IllegalArgumentException.class,
            () -> new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, futureYear, validCabin(), validModel()));
}
```

**Test:** `ensureDecommissionChangesStatus`

```java
@Test
void ensureDecommissionChangesStatus() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    aircraft.decommission();
    assertEquals(OperationalStatus.DECOMMISSIONED, aircraft.operationalStatus());
}
```

**Test:** `ensureCannotDecommissionAlreadyDecommissioned`

```java
@Test
void ensureCannotDecommissionAlreadyDecommissioned() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    aircraft.decommission();
    assertThrows(IllegalStateException.class, aircraft::decommission);
}
```

**Test:** `ensureIsActiveReturnsTrueForActiveAircraft`

```java
@Test
void ensureIsActiveReturnsTrueForActiveAircraft() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    assertTrue(aircraft.isActive());
}
```

**Test:** `ensureIsActiveReturnsFalseAfterDecommission`

```java
@Test
void ensureIsActiveReturnsFalseAfterDecommission() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    aircraft.decommission();
    assertFalse(aircraft.isActive());
}
```

**Test:** `ensureTwoAircraftWithSameRegistrationAreEqual`

```java
@Test
void ensureTwoAircraftWithSameRegistrationAreEqual() {
    final Aircraft a = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    final Aircraft b = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Spain", 4, 2018, validCabin(), validModel());
    assertEquals(a, b);
}
```

**Test:** `ensureTwoAircraftWithDifferentRegistrationAreNotEqual`

```java
@Test
void ensureTwoAircraftWithDifferentRegistrationAreNotEqual() {
    final Aircraft a = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    final Aircraft b = new Aircraft(RegistrationNumber.valueOf("CS-TUB"), "Portugal", 6, 2018, validCabin(), validModel());
    assertNotEquals(a, b);
}
```

**Test:** `ensureGettersReturnCorrectValues`

```java
@Test
void ensureGettersReturnCorrectValues() {
    final AircraftModel model = validModel();
    final CabinConfiguration cabin = validCabin();
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, cabin, model);
    assertEquals(cabin, aircraft.cabinConfiguration());
    assertEquals(model, aircraft.aircraftModel());
    assertEquals("Portugal", aircraft.registeredCountry());
    assertEquals(6, aircraft.numberOfCrewElements());
}
```

**Test:** `ensureIdentityReturnsRegistrationNumber`

```java
@Test
void ensureIdentityReturnsRegistrationNumber() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    assertEquals(RegistrationNumber.valueOf("CS-TUA"), aircraft.identity());
}
```

**Test:** `ensureToStringContainsRegistrationNumber`

```java
@Test
void ensureToStringContainsRegistrationNumber() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    assertTrue(aircraft.toString().contains("CS-TUA"));
}
```

**Test:** `ensureEqualsReturnsTrueForSameInstance`

```java
@Test
void ensureEqualsReturnsTrueForSameInstance() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    assertEquals(aircraft, aircraft);
}
```

**Test:** `ensureEqualsReturnsFalseForNull`

```java
@Test
void ensureEqualsReturnsFalseForNull() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    assertNotEquals(null, aircraft);
}
```

**Test:** `ensureSameAsReturnsTrueForSameInstance`

```java
@Test
void ensureSameAsReturnsTrueForSameInstance() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    assertTrue(aircraft.sameAs(aircraft));
}
```

**Test:** `ensureHashCodeIsConsistent`

```java
@Test
void ensureHashCodeIsConsistent() {
    final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf("CS-TUA"), "Portugal", 6, 2018, validCabin(), validModel());
    assertEquals(aircraft.hashCode(), aircraft.hashCode());
}
```

---

### `CabinConfigurationTest`

Location: `src/test/java/aisafe/aircraft/domain/CabinConfigurationTest.java`

**Test:** `ensureValidCabinConfigurationIsCreatedSuccessfully`

```java
@Test
void ensureValidCabinConfigurationIsCreatedSuccessfully() {
    final CabinConfiguration cabin = new CabinConfiguration(8, 20, 150);
    assertEquals(8, cabin.firstClassSeats());
    assertEquals(20, cabin.businessClassSeats());
    assertEquals(150, cabin.economyClassSeats());
    assertEquals(178, cabin.totalSeats());
}
```

**Test:** `ensureCabinConfigurationCannotHaveNegativeFirstClassSeats`

```java
@Test
void ensureCabinConfigurationCannotHaveNegativeFirstClassSeats() {
    assertThrows(IllegalArgumentException.class,
            () -> new CabinConfiguration(-1, 20, 150));
}
```

**Test:** `ensureCabinConfigurationCannotHaveNegativeBusinessClassSeats`

```java
@Test
void ensureCabinConfigurationCannotHaveNegativeBusinessClassSeats() {
    assertThrows(IllegalArgumentException.class,
            () -> new CabinConfiguration(8, -1, 150));
}
```

**Test:** `ensureCabinConfigurationCannotHaveNegativeEconomyClassSeats`

```java
@Test
void ensureCabinConfigurationCannotHaveNegativeEconomyClassSeats() {
    assertThrows(IllegalArgumentException.class,
            () -> new CabinConfiguration(8, 20, -1));
}
```

**Test:** `ensureTotalSeatsIsCalculatedCorrectly`

```java
@Test
void ensureTotalSeatsIsCalculatedCorrectly() {
    final CabinConfiguration cabin = new CabinConfiguration(4, 12, 100);
    assertEquals(4 + 12 + 100, cabin.totalSeats());
}
```

**Test:** `ensureCabinWithZeroAllSeatsThrows`

```java
@Test
void ensureCabinWithZeroAllSeatsThrows() {
    assertThrows(IllegalArgumentException.class,
            () -> new CabinConfiguration(0, 0, 0));
}
```

**Test:** `ensureEconomyOnlyConfigurationIsValid`

```java
@Test
void ensureEconomyOnlyConfigurationIsValid() {
    final CabinConfiguration cabin = new CabinConfiguration(0, 0, 189);
    assertEquals(189, cabin.totalSeats());
}
```

**Test:** `ensureToStringContainsSeatCounts`

```java
@Test
void ensureToStringContainsSeatCounts() {
    final CabinConfiguration cabin = new CabinConfiguration(8, 20, 150);
    final String str = cabin.toString();
    assertTrue(str.contains("8"));
    assertTrue(str.contains("20"));
    assertTrue(str.contains("150"));
}
```

**Test:** `ensureFirstClassSeatsGetterReturnsCorrectValue`

```java
@Test
void ensureFirstClassSeatsGetterReturnsCorrectValue() {
    final CabinConfiguration cabin = new CabinConfiguration(10, 0, 1);
    assertEquals(10, cabin.firstClassSeats());
}
```

**Test:** `ensureBusinessClassSeatsGetterReturnsCorrectValue`

```java
@Test
void ensureBusinessClassSeatsGetterReturnsCorrectValue() {
    final CabinConfiguration cabin = new CabinConfiguration(0, 15, 1);
    assertEquals(15, cabin.businessClassSeats());
}
```

**Test:** `ensureEconomyClassSeatsGetterReturnsCorrectValue`

```java
@Test
void ensureEconomyClassSeatsGetterReturnsCorrectValue() {
    final CabinConfiguration cabin = new CabinConfiguration(0, 0, 200);
    assertEquals(200, cabin.economyClassSeats());
}
```

---

## Coverage by Acceptance Criterion

- AC070.1: `ensureValidCabinConfigurationIsCreatedSuccessfully`, `ensureCabinWithZeroAllSeatsThrows`, `ensureTotalSeatsIsCalculatedCorrectly`, `ensureCabinConfigurationCannotHaveNegativeFirstClassSeats`, `ensureCabinConfigurationCannotHaveNegativeBusinessClassSeats`, `ensureCabinConfigurationCannotHaveNegativeEconomyClassSeats`, `ensureEconomyOnlyConfigurationIsValid`
- AC070.2: `ensureAircraftCannotExceedModelMaximumCapacity`, `ensureAircraftWithExactModelCapacityIsAccepted`
- AC070.3: `ensureAircraftMustHaveValidRegistrationNumber`, `ensureAircraftRegistrationCannotBeEmpty`, `ensureAircraftRegistrationIsNormalisedToUpperCase`, `ensureTwoAircraftWithSameRegistrationAreEqual`, `ensureTwoAircraftWithDifferentRegistrationAreNotEqual`, `ensureIdentityReturnsRegistrationNumber`; uniqueness enforced by `@EmbeddedId`
- AC070.4: `ensureAircraftMustBeRegisteredToACountry`
- AC070.5: `ensureAircraftIsCreatedWithActiveOperationalStatus`, `ensureDecommissionChangesStatus`, `ensureCannotDecommissionAlreadyDecommissioned`, `ensureIsActiveReturnsTrueForActiveAircraft`, `ensureIsActiveReturnsFalseAfterDecommission`
- AC070.6: Controller checks `ATCC` role via `AuthorizationService`; validated by manual test
- Domain invariants (construction): `ensureValidAircraftIsCreatedSuccessfully`, `ensureZeroCrewElementsThrows`, `ensureNegativeCrewElementsThrows`, `ensurePassengerAircraftWithNullCabinThrows`, `ensureCargoAircraftWithNullCabinIsValid`, `ensureCargoAircraftWithNonNullCabinThrows`, `ensureNullAircraftModelThrows`, `ensureYearOfManufactureIsStored`, `ensureYearOfManufactureBefore1900IsRejected`, `ensureYearOfManufactureInFutureIsRejected`, `ensureGettersReturnCorrectValues`, `ensureToStringContainsRegistrationNumber`, `ensureEqualsReturnsTrueForSameInstance`, `ensureEqualsReturnsFalseForNull`, `ensureSameAsReturnsTrueForSameInstance`, `ensureHashCodeIsConsistent`

---

## Acceptance Tests

Authorization (ATCC role) and company-fleet association are infrastructure concerns validated by manual integration testing. Cabin configuration and registration number validations are fully covered by the automated unit tests above.

**Manual test — AC070.1 / AC070.3 / AC070.4 / AC070.5 (full registration flow):**

1. Run `AiSafeBackofficeApp` and login as an ATCC user.
2. Navigate to `Fleet > Add Aircraft`.
3. Select an aircraft model (e.g. `737-800` by `Boeing`).
4. Provide: registration `CS-TNA`, country `Portugal`, crew size `6`, manufacture year `2010`, first class seats `0`, business class seats `20`, economy class seats `150`.
5. Expected: confirmation message displayed; aircraft appears in the fleet list with operational status `ACTIVE`.

**Manual test — AC070.2 (cabin capacity exceeded rejected):**

1. Provide a seat configuration whose total (first + business + economy) exceeds the selected model's maximum passenger capacity.
2. Expected: the system rejects the operation with a capacity validation error.

**Manual test — AC070.3 (duplicate registration rejected):**

1. Attempt to add a second aircraft with registration `CS-TNA`.
2. Expected: the system rejects the operation with a uniqueness violation message.

**Manual test — AC070.6 (role enforcement):**

1. Login as a user without the ATCC role (e.g. Admin or Backoffice Operator).
2. Expected: the Add Aircraft option is not available in the menu.
