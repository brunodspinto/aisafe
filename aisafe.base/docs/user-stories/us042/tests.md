# US042 — Tests and Coverage

## Scope

US042 covers bulk import of weather data from a file (CSV format). It reuses the `WeatherData` aggregate and `WeatherSource` value object from US041. New classes introduced are `WeatherDataParser` (interface), `CsvWeatherDataParser`, `ParsedWeatherRecord`, `ImportResult`, and `ImportBulkWeatherDataController`.

---

## Automated Tests

### `CsvWeatherDataParserTest`

Location: `src/test/java/aisafe/weatherdata/application/CsvWeatherDataParserTest.java`

> Note: tests write temporary CSV files using `Files.createTempFile` and delete them in `@AfterEach`.

**Test:** `ensureValidCsvRowProducesCorrectParsedRecord`

```java
@Test
void ensureValidCsvRowProducesCorrectParsedRecord() throws Exception {
    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility",
        "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0"
    );
    final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

    assertEquals(1, records.size());
    final ParsedWeatherRecord r = records.get(0);
    assertEquals("PT-N", r.areaCode());
    assertEquals("IPMA", r.provider());
    assertEquals("CSV", r.format());
    assertEquals(LocalDateTime.of(2025, 5, 14, 10, 0), r.date());
    assertEquals(20.0, r.temperature());
    assertEquals(15.0, r.windSpeed());
    assertEquals("N", r.windDirection());
    assertEquals(1013.0, r.pressure());
    assertEquals(10.0, r.visibility());
}
```

**Test:** `ensureHeaderOnlyFileReturnsEmptyList`

```java
@Test
void ensureHeaderOnlyFileReturnsEmptyList() throws Exception {
    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility"
    );
    final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

    assertTrue(records.isEmpty());
}
```

**Test:** `ensureMultipleValidRowsAreAllParsed`

```java
@Test
void ensureMultipleValidRowsAreAllParsed() throws Exception {
    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility",
        "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0",
        "PT-S,IPMA,CSV,2025-05-14T11:00:00,18.0,10.0,SW,1010.0,8.0"
    );
    final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

    assertEquals(2, records.size());
}
```

**Test:** `ensureRowWithMissingFieldIsSkipped`

```java
@Test
void ensureRowWithMissingFieldIsSkipped() throws Exception {
    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility",
        "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0"  // missing visibility
    );
    final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

    assertTrue(records.isEmpty());
}
```

**Test:** `ensureRowWithInvalidDateFormatIsSkipped`

```java
@Test
void ensureRowWithInvalidDateFormatIsSkipped() throws Exception {
    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility",
        "PT-N,IPMA,CSV,14-05-2025,20.0,15.0,N,1013.0,10.0"  // wrong date format
    );
    final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

    assertTrue(records.isEmpty());
}
```

**Test:** `ensureRowWithNonNumericTemperatureIsSkipped`

```java
@Test
void ensureRowWithNonNumericTemperatureIsSkipped() throws Exception {
    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility",
        "PT-N,IPMA,CSV,2025-05-14T10:00:00,WARM,15.0,N,1013.0,10.0"
    );
    final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

    assertTrue(records.isEmpty());
}
```

---

### `ImportResultTest`

Location: `src/test/java/aisafe/weatherdata/application/ImportResultTest.java`

**Test:** `ensureImportResultReportsCorrectSavedCount`

```java
@Test
void ensureImportResultReportsCorrectSavedCount() {
    final ImportResult result = new ImportResult(3, List.of());
    assertEquals(3, result.saved());
}
```

**Test:** `ensureImportResultReportsFailures`

```java
@Test
void ensureImportResultReportsFailures() {
    final List<String> failures = List.of("Row 2: unknown area code 'XX'", "Row 4: invalid wind direction");
    final ImportResult result = new ImportResult(1, failures);

    assertEquals(1, result.saved());
    assertEquals(2, result.failures().size());
    assertTrue(result.failures().contains("Row 2: unknown area code 'XX'"));
}
```

**Test:** `ensureImportResultWithZeroSavedAndNoFailures`

```java
@Test
void ensureImportResultWithZeroSavedAndNoFailures() {
    final ImportResult result = new ImportResult(0, List.of());
    assertEquals(0, result.saved());
    assertTrue(result.failures().isEmpty());
}
```

---

### `ImportBulkWeatherDataControllerTest`

Location: `src/test/java/aisafe/weatherdata/application/ImportBulkWeatherDataControllerTest.java`

> Note: uses `AuthenticationContext` and `PersistenceContext` with in-memory repositories. A Weather Person user is created in `@BeforeAll`. Test CSV files are written to temp files and deleted in `@AfterEach`. Tests instantiate via the public no-arg constructor, which wires `CsvWeatherDataParser` by default; the package-private constructor allows injecting any `WeatherDataParser` implementation (AC042.6).

**Test:** `ensureAllValidRecordsAreImported` *(AC042.1)*

```java
@Test
void ensureAllValidRecordsAreImported() throws Exception {
    AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
    ensureAreaExists("PT-N");

    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility",
        "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0",
        "PT-N,IPMA,CSV,2025-05-14T11:00:00,21.0,12.0,NE,1012.0,9.0"
    );

    final ImportResult result = controller.importWeatherData(csv.toString());

    assertEquals(2, result.saved());
    assertTrue(result.failures().isEmpty());
}
```

**Test:** `ensureRecordWithUnknownAreaCodeIsRejected` *(AC042.2)*

```java
@Test
void ensureRecordWithUnknownAreaCodeIsRejected() throws Exception {
    AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
    ensureAreaExists("PT-N");

    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility",
        "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0",
        "XX-UNKNOWN,IPMA,CSV,2025-05-14T11:00:00,21.0,12.0,NE,1012.0,9.0"
    );

    final ImportResult result = controller.importWeatherData(csv.toString());

    assertEquals(1, result.saved());
    assertEquals(1, result.failures().size());
    assertTrue(result.failures().get(0).contains("XX-UNKNOWN"));
}
```

**Test:** `ensureUnauthorizedUserCannotImport` *(AC042.3)*

```java
@Test
void ensureUnauthorizedUserCannotImport() throws Exception {
    AuthenticationContext.authenticate(BACKOFFICE_USERNAME, BACKOFFICE_PASSWORD);

    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility",
        "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0"
    );

    assertThrows(UnauthorizedException.class,
            () -> controller.importWeatherData(csv.toString()));
}
```

**Test:** `ensureMalformedRowIsSkippedAndValidRowIsSaved` *(AC042.4)*

```java
@Test
void ensureMalformedRowIsSkippedAndValidRowIsSaved() throws Exception {
    AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
    ensureAreaExists("PT-N");

    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility",
        "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,-5.0,N,1013.0,10.0",  // negative wind speed — invalid domain
        "PT-N,IPMA,CSV,2025-05-14T11:00:00,21.0,12.0,NE,1012.0,9.0"   // valid
    );

    final ImportResult result = controller.importWeatherData(csv.toString());

    assertEquals(1, result.saved());
    assertEquals(1, result.failures().size());
}
```

**Test:** `ensureImportResultContainsCountAndFailureReasons` *(AC042.5)*

```java
@Test
void ensureImportResultContainsCountAndFailureReasons() throws Exception {
    AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
    ensureAreaExists("PT-N");

    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility",
        "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0",
        "UNKNOWN,IPMA,CSV,2025-05-14T11:00:00,21.0,12.0,NE,1012.0,9.0",
        "PT-N,IPMA,CSV,2025-05-14T12:00:00,21.0,12.0,INVALID,1012.0,9.0"  // invalid wind direction
    );

    final ImportResult result = controller.importWeatherData(csv.toString());

    assertEquals(1, result.saved());
    assertEquals(2, result.failures().size());
    assertFalse(result.failures().get(0).isBlank());
    assertFalse(result.failures().get(1).isBlank());
}
```

**Test:** `ensureHeaderOnlyCsvProducesZeroImports` *(AC042.5 — empty file edge case)*

```java
@Test
void ensureHeaderOnlyCsvProducesZeroImports() throws Exception {
    AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);

    final Path csv = writeTempCsv(
        "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility"
    );

    final ImportResult result = controller.importWeatherData(csv.toString());

    assertEquals(0, result.saved());
    assertTrue(result.failures().isEmpty());
}
```

**Test:** `ensureParserInterfaceCanBeImplementedWithAlternativeFormat` *(AC042.6)*

```java
@Test
void ensureParserInterfaceCanBeImplementedWithAlternativeFormat() {
    // Verifies that WeatherDataParser is an interface — a second implementation
    // can be substituted without changing the controller.
    final WeatherDataParser parser = filePath -> List.of(
        new ParsedWeatherRecord("PT-N", "STUB", "JSON",
            LocalDateTime.of(2025, 1, 1, 0, 0), 10.0, 5.0, "N", 1013.0, 10.0)
    );
    final List<ParsedWeatherRecord> records = parser.parse("any-path");
    assertEquals(1, records.size());
    assertEquals("PT-N", records.get(0).areaCode());
}
```

---

## Coverage by Acceptance Criterion

- **AC042.1**: `ensureAllValidRecordsAreImported`, `ensureValidCsvRowProducesCorrectParsedRecord`, `ensureMultipleValidRowsAreAllParsed`
- **AC042.2**: `ensureRecordWithUnknownAreaCodeIsRejected`
- **AC042.3**: `ensureUnauthorizedUserCannotImport`
- **AC042.4**: `ensureMalformedRowIsSkippedAndValidRowIsSaved`, `ensureRowWithMissingFieldIsSkipped`, `ensureRowWithInvalidDateFormatIsSkipped`, `ensureRowWithNonNumericTemperatureIsSkipped`
- **AC042.5**: `ensureImportResultContainsCountAndFailureReasons`, `ensureImportResultReportsCorrectSavedCount`, `ensureImportResultReportsFailures`, `ensureHeaderOnlyCsvProducesZeroImports`, `ensureImportResultWithZeroSavedAndNoFailures`
- **AC042.6**: `ensureParserInterfaceCanBeImplementedWithAlternativeFormat`; additionally, the package-private constructor of `ImportBulkWeatherDataController` allows injecting any `WeatherDataParser`, so a new format implementation can be used without modifying the controller

---

## Acceptance Tests

Authorization (`WEATHER_PERSON` role) and full persistence flow are validated by the controller integration tests above, which use in-memory repositories. Field-level domain validation is covered by the existing `WeatherDataTest` (US041).

**Manual test — AC042.1 / AC042.2 (full import flow):**

1. Run `AiSafeBackofficeApp` and login as a Weather Person.
2. Ensure at least one Air Control Area exists (e.g. `PT-N`).
3. Navigate to `Weather > Import Bulk Weather Data`.
4. Enter the path to a CSV file with the header `area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility` and one or more valid data rows.
5. Expected: `Import complete: X record(s) saved.` — where X matches the number of valid rows.

**Manual test — AC042.2 (unknown area code rejected):**

1. Provide a CSV where one row references an area code that does not exist in the system.
2. Expected: that row is reported as a failure; other valid rows are still saved.

**Manual test — AC042.3 (role enforcement):**

1. Login as a user without the `WEATHER_PERSON` role (e.g. Backoffice Operator).
2. Expected: the `Import Bulk Weather Data` option is not available in the menu.

**Manual test — AC042.4 (malformed row skipped):**

1. Provide a CSV file where one row has a negative wind speed or an invalid wind direction.
2. Expected: that row is reported as a failure; the remaining valid rows are saved.
