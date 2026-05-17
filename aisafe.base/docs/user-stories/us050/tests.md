# US050 — Tests and Coverage

## Scope

US050 covers registering an Air Control Area with a unique area code, name, minimum fuel requirement, and valid geographic boundaries.

## Automated Tests

### `AirControlAreaTest`

Location: `src/test/java/aisafe/aircontrolarea/domain/AirControlAreaTest.java`

**Test:** `ensureValidAirControlAreaCanBeCreated`

```java
@Test
void ensureValidAirControlAreaCanBeCreated() {
    final AirControlArea area = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Northern Portugal", 1200.0, validBoundary());

    assertEquals("PT-N", area.areaCode());
    assertEquals("Northern Portugal", area.name());
    assertEquals(1200.0, area.minimumFuelRequired());
    assertEquals(validBoundary(), area.boundaries());
}
```

**Test:** `ensureAreaCodeCannotBeNullOrBlank`

```java
@Test
void ensureAreaCodeCannotBeNullOrBlank() {
    assertThrows(IllegalArgumentException.class,
            () -> new AirControlArea((AirControlAreaCode) null, "Area", 1200.0, validBoundary()));

    assertThrows(IllegalArgumentException.class,
            () -> new AirControlArea(AirControlAreaCode.valueOf("   "), "Area", 1200.0, validBoundary()));
}
```

**Test:** `ensureNameCannotBeNullOrBlank`

```java
@Test
void ensureNameCannotBeNullOrBlank() {
    assertThrows(IllegalArgumentException.class,
            () -> new AirControlArea(AirControlAreaCode.valueOf("PT-N"), null, 1200.0, validBoundary()));

    assertThrows(IllegalArgumentException.class,
            () -> new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "   ", 1200.0, validBoundary()));
}
```

**Test:** `ensureMinimumFuelCannotBeNegative`

```java
@Test
void ensureMinimumFuelCannotBeNegative() {
    assertThrows(IllegalArgumentException.class,
            () -> new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Area", -1.0, validBoundary()));
}
```

**Test:** `ensureBoundariesCannotBeNull`

```java
@Test
void ensureBoundariesCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
            () -> new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Area", 1200.0, null));
}
```

---

### `GeoBoundaryTest`

Location: `src/test/java/aisafe/aircontrolarea/domain/GeoBoundaryTest.java`

**Test:** `ensureValidCoordinatesCreateBoundary`

```java
@Test
void ensureValidCoordinatesCreateBoundary() {
    final GeoBoundary boundary = new GeoBoundary(42.15, 36.95, -6.18, -9.50);

    assertEquals(42.15, boundary.northLatitude());
    assertEquals(36.95, boundary.southLatitude());
    assertEquals(-6.18, boundary.eastLongitude());
    assertEquals(-9.50, boundary.westLongitude());
}
```

**Test:** `ensureNorthLatitudeMustBeGreaterThanSouthLatitude`

```java
@Test
void ensureNorthLatitudeMustBeGreaterThanSouthLatitude() {
    assertThrows(IllegalArgumentException.class,
            () -> new GeoBoundary(30.0, 40.0, -10.0, 10.0));
}
```

**Test:** `ensureCoordinatesMustBeWithinLatitudeLimits`

```java
@Test
void ensureCoordinatesMustBeWithinLatitudeLimits() {
    assertThrows(IllegalArgumentException.class,
            () -> new GeoBoundary(95.0, 20.0, -10.0, 10.0));
}
```

**Test:** `ensureCoordinatesMustBeWithinLongitudeLimits`

```java
@Test
void ensureCoordinatesMustBeWithinLongitudeLimits() {
    assertThrows(IllegalArgumentException.class,
            () -> new GeoBoundary(40.0, 30.0, 200.0, 10.0));
}
```

---

## Coverage by Acceptance Criterion

- AC050.1: `ensureValidAirControlAreaCanBeCreated`, `ensureAreaCodeCannotBeNullOrBlank`, `ensureNameCannotBeNullOrBlank`, `ensureMinimumFuelCannotBeNegative`, `ensureBoundariesCannotBeNull`
- AC050.2: Enforced by `@EmbeddedId` on `AirControlAreaCode` and `@UniqueConstraint` at persistence level; validated by manual test
- AC050.3: `ensureValidCoordinatesCreateBoundary`, `ensureNorthLatitudeMustBeGreaterThanSouthLatitude`, `ensureCoordinatesMustBeWithinLatitudeLimits`, `ensureCoordinatesMustBeWithinLongitudeLimits`
- AC050.4: Covered by `AiSafeBootstrap` which registers the default area idempotently

---

## Acceptance Tests

Authorization (Backoffice Operator role) and persistence-level uniqueness are infrastructure concerns primarily validated by manual integration testing. The geographic boundary validations are fully covered by the automated unit tests above.

**Manual test — AC050.1 (full registration flow):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Air Control Areas > Register Air Control Area`.
3. Provide: code `EUR`, name `European Area`, minimum fuel `500`, north latitude `71`, south latitude `35`, east longitude `40`, west longitude `-25`.
4. Expected: confirmation message displayed and area visible in the list.

**Manual test — AC050.2 (duplicate area code rejected):**

1. Attempt to register a second area with code `EUR`.
2. Expected: the system rejects the operation with a duplicate code error message.

**Manual test — AC050.3 (invalid boundaries rejected):**

1. Attempt to register an area providing south latitude `50` and north latitude `40` (south greater than north).
2. Expected: the system rejects the operation with a boundary validation error message.
