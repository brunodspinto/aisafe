# US041 — Tests and Coverage

## Scope

US041 covers registering weather data for a specific air control area, including date, temperature, wind speed, wind direction, pressure, visibility, and source.

## Automated Tests

### `WeatherDataTest`

Location: `src/test/java/aisafe/weatherdata/domain/WeatherDataTest.java`

> Note: tests use the constants `VALID_DATE` and `VALID_SOURCE` and the helper method `validWeatherData()` defined in the same class.

**Test:** `ensureValidWeatherDataCanBeCreated`

```java
@Test
void ensureValidWeatherDataCanBeCreated() {
    final WeatherData wd = validWeatherData();

    assertEquals("PT-N", wd.areaCode());
    assertEquals(VALID_SOURCE, wd.source());
    assertEquals(VALID_DATE, wd.date());
    assertEquals(20.0, wd.temperature());
    assertEquals(15.0, wd.windSpeed());
    assertEquals("N", wd.windDirection());
    assertEquals(1013.0, wd.pressure());
    assertEquals(10.0, wd.visibility());
}
```

**Test:** `ensureAreaCodeIsNormalisedToUpperCase`

```java
@Test
void ensureAreaCodeIsNormalisedToUpperCase() {
    final WeatherData wd = new WeatherData("pt-n", VALID_SOURCE, VALID_DATE, 20.0, 15.0, "N", 1013.0, 10.0);
    assertEquals("PT-N", wd.areaCode());
}
```

**Test:** `ensureAreaCodeCannotBeNull`

```java
@Test
void ensureAreaCodeCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
            () -> new WeatherData(null, VALID_SOURCE, VALID_DATE, 20.0, 15.0, "N", 1013.0, 10.0));
}
```

**Test:** `ensureSourceCannotBeNull`

```java
@Test
void ensureSourceCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
            () -> new WeatherData("PT-N", null, VALID_DATE, 20.0, 15.0, "N", 1013.0, 10.0));
}
```

**Test:** `ensureDateCannotBeNull`

```java
@Test
void ensureDateCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
            () -> new WeatherData("PT-N", VALID_SOURCE, null, 20.0, 15.0, "N", 1013.0, 10.0));
}
```

**Test:** `ensureWindSpeedCannotBeNegative`

```java
@Test
void ensureWindSpeedCannotBeNegative() {
    assertThrows(IllegalArgumentException.class,
            () -> new WeatherData("PT-N", VALID_SOURCE, VALID_DATE, 20.0, -1.0, "N", 1013.0, 10.0));
}
```

**Test:** `ensureVisibilityCannotBeNegative`

```java
@Test
void ensureVisibilityCannotBeNegative() {
    assertThrows(IllegalArgumentException.class,
            () -> new WeatherData("PT-N", VALID_SOURCE, VALID_DATE, 20.0, 15.0, "N", 1013.0, -1.0));
}
```

**Test:** `ensureWindSpeedOfZeroIsValid`

```java
@Test
void ensureWindSpeedOfZeroIsValid() {
    final WeatherData wd = new WeatherData("PT-N", VALID_SOURCE, VALID_DATE, 20.0, 0.0, "N", 1013.0, 10.0);
    assertEquals(0.0, wd.windSpeed());
}
```

**Test:** `ensureVisibilityOfZeroIsValid`

```java
@Test
void ensureVisibilityOfZeroIsValid() {
    final WeatherData wd = new WeatherData("PT-N", VALID_SOURCE, VALID_DATE, 20.0, 15.0, "N", 1013.0, 0.0);
    assertEquals(0.0, wd.visibility());
}
```

**Test:** `ensureWindDirectionCannotBeNull`

```java
@Test
void ensureWindDirectionCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
            () -> new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 15.0, null, 1013.0, 10.0));
}
```

**Test:** `ensureWindDirectionCannotBeBlank`

```java
@Test
void ensureWindDirectionCannotBeBlank() {
    assertThrows(IllegalArgumentException.class,
            () -> new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 15.0, "   ", 1013.0, 10.0));
}
```

**Test:** `ensureWindDirectionMustBeValidCompassDirection`

```java
@Test
void ensureWindDirectionMustBeValidCompassDirection() {
    assertThrows(IllegalArgumentException.class,
            () -> new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 15.0, "NORTHEAST", 1013.0, 10.0));

    final WeatherData wd = new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 15.0, "NE", 1013.0, 10.0);
    assertEquals("NE", wd.windDirection());
}
```

### `WeatherSourceTest`

Location: `src/test/java/aisafe/weatherdata/domain/WeatherSourceTest.java`

**Test:** `ensureValidWeatherSourceCanBeCreated`

```java
@Test
void ensureValidWeatherSourceCanBeCreated() {
    final WeatherSource ws = new WeatherSource("IPMA", "JSON");
    assertEquals("IPMA", ws.provider());
    assertEquals("JSON", ws.format());
}
```

**Test:** `ensureProviderCannotBeNull`

```java
@Test
void ensureProviderCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () -> new WeatherSource(null, "JSON"));
}
```

**Test:** `ensureProviderCannotBeBlank`

```java
@Test
void ensureProviderCannotBeBlank() {
    assertThrows(IllegalArgumentException.class, () -> new WeatherSource("   ", "JSON"));
}
```

**Test:** `ensureFormatCannotBeNull`

```java
@Test
void ensureFormatCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () -> new WeatherSource("IPMA", null));
}
```

**Test:** `ensureFormatCannotBeBlank`

```java
@Test
void ensureFormatCannotBeBlank() {
    assertThrows(IllegalArgumentException.class, () -> new WeatherSource("IPMA", "   "));
}
```

**Test:** `ensureEqualityForSameValues`

```java
@Test
void ensureEqualityForSameValues() {
    final WeatherSource ws1 = new WeatherSource("IPMA", "JSON");
    final WeatherSource ws2 = new WeatherSource("IPMA", "JSON");
    assertEquals(ws1, ws2);
    assertEquals(ws1.hashCode(), ws2.hashCode());
}
```

**Test:** `ensureInequalityForDifferentProvider`

```java
@Test
void ensureInequalityForDifferentProvider() {
    assertNotEquals(new WeatherSource("IPMA", "JSON"), new WeatherSource("METAR", "JSON"));
}
```

**Test:** `ensureInequalityForDifferentFormat`

```java
@Test
void ensureInequalityForDifferentFormat() {
    assertNotEquals(new WeatherSource("IPMA", "JSON"), new WeatherSource("IPMA", "XML"));
}
```

**Test:** `ensureProviderIsStoredTrimmed`

```java
@Test
void ensureProviderIsStoredTrimmed() {
    final WeatherSource ws = new WeatherSource("  IPMA  ", "JSON");
    assertEquals("IPMA", ws.provider());
}
```

**Test:** `ensureFormatIsStoredTrimmed`

```java
@Test
void ensureFormatIsStoredTrimmed() {
    final WeatherSource ws = new WeatherSource("IPMA", "  JSON  ");
    assertEquals("JSON", ws.format());
}
```

---

## Coverage by Acceptance Criterion

- AC041.1: `ensureValidWeatherDataCanBeCreated`, `ensureWindSpeedCannotBeNegative`, `ensureVisibilityCannotBeNegative`, `ensureWindSpeedOfZeroIsValid`, `ensureVisibilityOfZeroIsValid`, `ensureDateCannotBeNull`, `ensureSourceCannotBeNull`; `ensureValidWeatherSourceCanBeCreated`, `ensureProviderCannotBeNull`, `ensureProviderCannotBeBlank`, `ensureFormatCannotBeNull`, `ensureFormatCannotBeBlank`, `ensureEqualityForSameValues`, `ensureInequalityForDifferentProvider`, `ensureInequalityForDifferentFormat`, `ensureProviderIsStoredTrimmed`, `ensureFormatIsStoredTrimmed`
- AC041.2: `ensureAreaCodeCannotBeNull`, `ensureAreaCodeIsNormalisedToUpperCase`, `ensureWindDirectionCannotBeNull`, `ensureWindDirectionCannotBeBlank`, `ensureWindDirectionMustBeValidCompassDirection`; area existence validated at controller level via `AirControlAreaRepository`
- AC041.3: Controller checks `WEATHER_PERSON` role via `AuthorizationService`; validated by manual test

---

## Acceptance Tests

Authorization (`WEATHER_PERSON` role) and the link to an existing Air Control Area are infrastructure concerns primarily validated by manual integration testing. Weather field validations are fully covered by the automated unit tests above.

**Manual test — AC041.1 / AC041.2 (full registration flow):**

1. Run `AiSafeBackofficeApp` and login as a Weather Person.
2. Navigate to `Weather > Register Weather Data`.
3. Select an existing Air Control Area (e.g. `PT-N`).
4. Provide: date `2025-05-14 10:00`, temperature `20.0`, wind speed `15.0`, wind direction `N`, pressure `1013.0`, visibility `10.0`, source `IPMA`, format `JSON`.
5. Expected: confirmation message `Weather data registered successfully for area 'PT-N'.`

**Manual test — AC041.1 (negative wind speed rejected):**

1. Enter wind speed `-1.0`.
2. Expected: the system rejects the input with a validation error.

**Manual test — AC041.3 (role enforcement):**

1. Login as a user without the `WEATHER_PERSON` role (e.g. Backoffice Operator).
2. Expected: the Register Weather Data option is not available in the menu.
