# US044 — Tests and Coverage

## Scope

US044 covers remote access to the AISafe system by a Weather Person via a dedicated TCP client application. The tests verify the TCP protocol (command dispatch, error handling), role-based access control, and the end-to-end validation of command parameters.

---

## Automated Tests

### `WeatherPersonSessionHandlerTest`

Location: `src/test/java/aisafe/tcpserver/weatherperson/WeatherPersonSessionHandlerTest.java`

Uses in-memory streams (`StringReader` / `StringWriter`) to test the command loop without a real socket, server, or EAPLI authentication context.

**Test:** `ensureExitCommandReturnsBye`

```java
@Test
void ensureExitCommandReturnsBye() throws IOException {
    final String response = runSession("EXIT");
    assertEquals("BYE", response);
}
```

**Test:** `ensureUnknownCommandReturnsUnknownCommand`

```java
@Test
void ensureUnknownCommandReturnsUnknownCommand() throws IOException {
    final String response = runSession("HELLO\nEXIT");
    assertTrue(response.contains("UNKNOWN_COMMAND"));
}
```

**Test:** `ensureMultipleUnknownCommandsAreEachRejected`

```java
@Test
void ensureMultipleUnknownCommandsAreEachRejected() throws IOException {
    final String response = runSession("FOO\nBAR\nEXIT");
    assertEquals(2, response.lines().filter(l -> l.equals("UNKNOWN_COMMAND")).count());
}
```

**Test:** `ensureRegisterWeatherWithMissingParamsReturnsError`

```java
@Test
void ensureRegisterWeatherWithMissingParamsReturnsError() throws IOException {
    final String response = runSession("REGISTER_WEATHER\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureImportBulkWithMissingLengthReturnsError`

```java
@Test
void ensureImportBulkWithMissingLengthReturnsError() throws IOException {
    final String response = runSession("IMPORT_BULK\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureImportBulkWithInvalidLengthReturnsError`

```java
@Test
void ensureImportBulkWithInvalidLengthReturnsError() throws IOException {
    final String response = runSession("IMPORT_BULK abc\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureImportBulkWithNegativeLengthReturnsError`

```java
@Test
void ensureImportBulkWithNegativeLengthReturnsError() throws IOException {
    final String response = runSession("IMPORT_BULK -5\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureConsultWeatherWithMissingParamsReturnsError`

```java
@Test
void ensureConsultWeatherWithMissingParamsReturnsError() throws IOException {
    final String response = runSession("CONSULT_WEATHER\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureSessionHandlesUnknownCommandBeforeExit`

```java
@Test
void ensureSessionHandlesUnknownCommandBeforeExit() throws IOException {
    final String response = runSession("UNKNOWN\nEXIT");
    assertTrue(response.contains("UNKNOWN_COMMAND"));
    assertTrue(response.contains("BYE"));
}
```

**Test:** `ensureRegisterWeatherWithInvalidDateTimeReturnsError`

```java
@Test
void ensureRegisterWeatherWithInvalidDateTimeReturnsError() throws IOException {
    final String response = runSession(
            "REGISTER_WEATHER LPPC MeteoGroup CSV not-a-date 15.2 30.5 NW 1013.2 9999\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureConsultWeatherWithMissingAreaCodeReturnsError`

```java
@Test
void ensureConsultWeatherWithMissingAreaCodeReturnsError() throws IOException {
    final String response = runSession("CONSULT_WEATHER 2026-06-01\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureListAreasReturnsEmptyLineAsEndMarker`

```java
@Test
void ensureListAreasReturnsEmptyLineAsEndMarker() throws IOException {
    final String response = runSession("LIST_AREAS\nEXIT");
    // Response ends with empty line marker then BYE — verify no crash
    assertTrue(response.contains("BYE"));
}
```

**Test:** `ensureImportBulkZeroLengthReturnsError`

```java
@Test
void ensureImportBulkZeroLengthReturnsError() throws IOException {
    final String response = runSession("IMPORT_BULK 0\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

---

### `WeatherPersonSessionHandlerIT`

Location: `src/test/java/aisafe/tcpserver/weatherperson/WeatherPersonSessionHandlerIT.java`

Implementation tests that exercise `WeatherPersonSessionHandler` over a real loopback TCP socket. A `ServerSocket` on a random port is started in `@BeforeEach` and torn down in `@AfterEach`. These tests verify that the protocol behaves correctly over actual network I/O.

**Test:** `ensureExitOverRealSocketReturnsBye`

```java
@Test
void ensureExitOverRealSocketReturnsBye() throws Exception {
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

**Test:** `ensureUnknownCommandOverRealSocketReturnsUnknownCommand`

```java
@Test
void ensureUnknownCommandOverRealSocketReturnsUnknownCommand() throws Exception {
    clientOut.println("HELLO");
    assertEquals("UNKNOWN_COMMAND", clientIn.readLine());
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

**Test:** `ensureRegisterWeatherWithMissingParamsOverRealSocketReturnsError`

```java
@Test
void ensureRegisterWeatherWithMissingParamsOverRealSocketReturnsError() throws Exception {
    clientOut.println("REGISTER_WEATHER");
    assertTrue(clientIn.readLine().startsWith("ERROR"));
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

**Test:** `ensureImportBulkWithInvalidLengthOverRealSocketReturnsError`

```java
@Test
void ensureImportBulkWithInvalidLengthOverRealSocketReturnsError() throws Exception {
    clientOut.println("IMPORT_BULK abc");
    assertTrue(clientIn.readLine().startsWith("ERROR"));
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

**Test:** `ensureImportBulkWithNegativeLengthOverRealSocketReturnsError`

```java
@Test
void ensureImportBulkWithNegativeLengthOverRealSocketReturnsError() throws Exception {
    clientOut.println("IMPORT_BULK -1");
    assertTrue(clientIn.readLine().startsWith("ERROR"));
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

**Test:** `ensureConsultWeatherWithMissingParamsOverRealSocketReturnsError`

```java
@Test
void ensureConsultWeatherWithMissingParamsOverRealSocketReturnsError() throws Exception {
    clientOut.println("CONSULT_WEATHER");
    assertTrue(clientIn.readLine().startsWith("ERROR"));
    clientIn.readLine(); // consume end-of-data empty line
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

**Test:** `ensureMultipleCommandsOverRealSocketAreHandledInSequence`

```java
@Test
void ensureMultipleCommandsOverRealSocketAreHandledInSequence() throws Exception {
    clientOut.println("FOO");
    assertEquals("UNKNOWN_COMMAND", clientIn.readLine());
    clientOut.println("BAR");
    assertEquals("UNKNOWN_COMMAND", clientIn.readLine());
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

---

### `TcpClientDispatcherTest`

Location: `src/test/java/aisafe/tcpserver/TcpClientDispatcherTest.java`

Integration tests that exercise `TcpClientDispatcher` over a real loopback TCP socket with a full EAPLI authentication bootstrap. These tests verify AC044.4: that the dispatcher enforces authentication and role-based authorization before granting access to the Weather Person command loop.

**Test:** `ensureWeatherPersonLoginWithServiceTokenReturnsOK`

```java
@Test
void ensureWeatherPersonLoginWithServiceTokenReturnsOK() throws Exception {
    clientOut.println("LOGIN weather-disp-test Password1 WEATHER");
    assertEquals("OK", clientIn.readLine());
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

**Test:** `ensureWeatherPersonLoginWithoutServiceTokenReturnsUnauthorized`

```java
@Test
void ensureWeatherPersonLoginWithoutServiceTokenReturnsUnauthorized() throws Exception {
    // WEATHER_PERSON user without WEATHER token — dispatcher treats as Pilot path → UNAUTHORIZED
    clientOut.println("LOGIN weather-disp-test Password1");
    assertEquals("UNAUTHORIZED", clientIn.readLine());
}
```

---

## Coverage by Acceptance Criterion

- **AC044.1** (TCP client + command loop): `ensureExitCommandReturnsBye`, `ensureSessionHandlesUnknownCommandBeforeExit` + manual test (successful session)
- **AC044.2** (no direct DB access from client): structural — `WeatherPersonTcpClientApp` has only JDK imports; verified by code inspection
- **AC044.3** (Weather Person USs available remotely): `ensureRegisterWeatherWithMissingParamsReturnsError`, `ensureImportBulkWithInvalidLengthReturnsError`, `ensureImportBulkWithNegativeLengthReturnsError`, `ensureConsultWeatherWithMissingParamsReturnsError`, `ensureRegisterWeatherWithInvalidDateTimeReturnsError`, `ensureConsultWeatherWithMissingAreaCodeReturnsError`, `ensureImportBulkZeroLengthReturnsError` + manual tests (valid flows)
- **AC044.4** (authentication and authorization): `ensureWeatherPersonLoginWithServiceTokenReturnsOK`, `ensureWeatherPersonLoginWithoutServiceTokenReturnsUnauthorized` (from `TcpClientDispatcherTest`)
- **Protocol robustness**: `ensureUnknownCommandReturnsUnknownCommand`, `ensureMultipleUnknownCommandsAreEachRejected`

---

## Acceptance Tests

**Prerequisites:** `AiSafeConsoleApp` running (TCP server on port 9999) and `weather_person / Password1` created by the bootstrap.

---

**AC044.1 + AC044.4 — Successful authentication as Weather Person**

1. Run `WeatherPersonTcpClientApp`, enter host `localhost` and port `9999`.
2. Enter credentials `weather_person` / `Password1`.
3. Expected: server responds `OK` and the Weather Person menu is displayed.

---

**AC044.4 — Authentication failure (wrong password)**

1. Run `WeatherPersonTcpClientApp` and enter credentials `weather_person` / `wrongpassword`.
2. Expected: client displays `Authentication failed.` and terminates.

---

**AC044.4 — Authorization failure (non-Weather-Person user)**

1. Run `WeatherPersonTcpClientApp` and enter credentials `pilot1` / `Password1`.
2. Expected: client displays `Authentication failed.` and terminates (server responded `UNAUTHORIZED`).

---

**AC044.3 — REGISTER_WEATHER with valid parameters**

1. Authenticate as `weather_person`.
2. Select option `1` and provide valid parameters (e.g. area code `LPPC`, provider `MeteoGroup`, format `CSV`, date-time `2026-06-01T12:00`, temp `15.2`, wind speed `30.5`, wind dir `NW`, pressure `1013.2`, visibility `9999`).
3. Expected: server responds `OK <id>`.

---

**AC044.3 — IMPORT_BULK with valid CSV file**

1. Authenticate as `weather_person`.
2. Select option `2` and provide the path to a valid CSV file.
3. Expected: server responds `OK saved=N failures=M`.

---

**AC044.1 — EXIT command**

1. Authenticate as `weather_person` and select option `0`.
2. Expected: session terminates cleanly.
