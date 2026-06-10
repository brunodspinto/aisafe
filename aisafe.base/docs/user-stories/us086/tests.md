# US086 — Tests and Coverage

## Scope

US086 covers remote access to the AISafe system by a Pilot via a dedicated TCP client application. The tests verify the TCP protocol (command dispatch, error handling), role-based access control, and the end-to-end flow of the `CREATE_FLIGHT_PLAN` command.

---

## Automated Tests

### `PilotSessionHandlerTest`

Location: `src/test/java/aisafe/tcpserver/pilot/PilotSessionHandlerTest.java`

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

**Test:** `ensureCreateFlightPlanWithoutByteLengthReturnsError`

```java
@Test
void ensureCreateFlightPlanWithoutByteLengthReturnsError() throws IOException {
    final String response = runSession("CREATE_FLIGHT_PLAN\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureCreateFlightPlanWithInvalidByteLengthReturnsError`

```java
@Test
void ensureCreateFlightPlanWithInvalidByteLengthReturnsError() throws IOException {
    final String response = runSession("CREATE_FLIGHT_PLAN abc\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureCreateFlightPlanWithNegativeByteLengthReturnsError`

```java
@Test
void ensureCreateFlightPlanWithNegativeByteLengthReturnsError() throws IOException {
    final String response = runSession("CREATE_FLIGHT_PLAN -10\nEXIT");
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

**Test:** `ensureInsertWeatherDataWithoutArgsReturnsError`

```java
@Test
void ensureInsertWeatherDataWithoutArgsReturnsError() throws IOException {
    final String response = runSession("INSERT_WEATHER_DATA\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureInsertWeatherDataWithMissingIdReturnsError`

```java
@Test
void ensureInsertWeatherDataWithMissingIdReturnsError() throws IOException {
    final String response = runSession("INSERT_WEATHER_DATA TP123\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureInsertWeatherDataWithNonNumericIdReturnsError`

```java
@Test
void ensureInsertWeatherDataWithNonNumericIdReturnsError() throws IOException {
    final String response = runSession("INSERT_WEATHER_DATA TP123 abc\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureInsertWeatherDataWithNegativeIdReturnsError`

```java
@Test
void ensureInsertWeatherDataWithNegativeIdReturnsError() throws IOException {
    final String response = runSession("INSERT_WEATHER_DATA TP123 -5\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureTestFlightPlanWithoutArgsReturnsError`

```java
@Test
void ensureTestFlightPlanWithoutArgsReturnsError() throws IOException {
    final String response = runSession("TEST_FLIGHT_PLAN\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

---

### `PilotSessionHandlerIT`

Location: `src/test/java/aisafe/tcpserver/pilot/PilotSessionHandlerIT.java`

Implementation tests that exercise `PilotSessionHandler` over a real loopback TCP socket. A `ServerSocket` on a random port is started in `@BeforeEach` and torn down in `@AfterEach`. These tests verify that the protocol behaves correctly over actual network I/O.

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

**Test:** `ensureCreateFlightPlanWithInvalidLengthOverRealSocketReturnsError`

```java
@Test
void ensureCreateFlightPlanWithInvalidLengthOverRealSocketReturnsError() throws Exception {
    clientOut.println("CREATE_FLIGHT_PLAN abc");
    assertTrue(clientIn.readLine().startsWith("ERROR"));
}
```

**Test:** `ensureCreateFlightPlanWithNegativeLengthOverRealSocketReturnsError`

```java
@Test
void ensureCreateFlightPlanWithNegativeLengthOverRealSocketReturnsError() throws Exception {
    clientOut.println("CREATE_FLIGHT_PLAN -1");
    assertTrue(clientIn.readLine().startsWith("ERROR"));
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

**Test:** `ensureInsertWeatherDataWithoutArgsOverRealSocketReturnsError`

```java
@Test
void ensureInsertWeatherDataWithoutArgsOverRealSocketReturnsError() throws Exception {
    clientOut.println("INSERT_WEATHER_DATA");
    assertTrue(clientIn.readLine().startsWith("ERROR"));
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

**Test:** `ensureInsertWeatherDataWithInvalidIdOverRealSocketReturnsError`

```java
@Test
void ensureInsertWeatherDataWithInvalidIdOverRealSocketReturnsError() throws Exception {
    clientOut.println("INSERT_WEATHER_DATA TP123 abc");
    assertTrue(clientIn.readLine().startsWith("ERROR"));
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

**Test:** `ensureTestFlightPlanWithoutArgsOverRealSocketReturnsError`

```java
@Test
void ensureTestFlightPlanWithoutArgsOverRealSocketReturnsError() throws Exception {
    clientOut.println("TEST_FLIGHT_PLAN");
    assertTrue(clientIn.readLine().startsWith("ERROR"));
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

---

### `TcpClientDispatcherTest`

Location: `src/test/java/aisafe/tcpserver/TcpClientDispatcherTest.java`

Integration tests that exercise `TcpClientDispatcher` over a real loopback TCP socket with a full EAPLI authentication bootstrap. These tests verify AC086.4: that the dispatcher enforces authentication and role-based authorization before granting access to the Pilot command loop.

**Test:** `ensureInvalidCredentialsReturnFail`

```java
@Test
void ensureInvalidCredentialsReturnFail() throws Exception {
    clientOut.println("LOGIN pilot-disp-test wrongpassword");
    assertTrue(clientIn.readLine().startsWith("FAIL"));
}
```

**Test:** `ensureNonPilotRoleReturnsUnauthorized`

```java
@Test
void ensureNonPilotRoleReturnsUnauthorized() throws Exception {
    // ATCC user logs in without a service token — dispatcher treats as Pilot path → UNAUTHORIZED
    clientOut.println("LOGIN atcc-disp-test Password1");
    assertEquals("UNAUTHORIZED", clientIn.readLine());
}
```

**Test:** `ensurePilotLoginReturnsOK`

```java
@Test
void ensurePilotLoginReturnsOK() throws Exception {
    clientOut.println("LOGIN pilot-disp-test Password1");
    assertEquals("OK", clientIn.readLine());
    clientOut.println("EXIT");
    assertEquals("BYE", clientIn.readLine());
}
```

---

## Coverage by Acceptance Criterion

- **AC086.1** (TCP client + command loop): `ensureExitCommandReturnsBye`, `ensureSessionHandlesUnknownCommandBeforeExit` + manual test (successful session)
- **AC086.2** (no direct DB access from client): structural — `PilotTcpClientApp` has only JDK imports; verified by code inspection
- **AC086.3** (Pilot USs available remotely): `ensureCreateFlightPlanWithoutByteLengthReturnsError`, `ensureCreateFlightPlanWithInvalidByteLengthReturnsError`, `ensureCreateFlightPlanWithNegativeByteLengthReturnsError`, `ensureInsertWeatherDataWithoutArgsReturnsError`, `ensureInsertWeatherDataWithMissingIdReturnsError`, `ensureInsertWeatherDataWithNonNumericIdReturnsError`, `ensureInsertWeatherDataWithNegativeIdReturnsError`, `ensureTestFlightPlanWithoutArgsReturnsError` + manual tests (valid flows)
- **AC086.4** (authentication and authorization): `ensureInvalidCredentialsReturnFail`, `ensureNonPilotRoleReturnsUnauthorized`, `ensurePilotLoginReturnsOK`
- **Protocol robustness**: `ensureUnknownCommandReturnsUnknownCommand`, `ensureMultipleUnknownCommandsAreEachRejected`

---

## Acceptance Tests

**Prerequisites:** `AiSafeConsoleApp` running (TCP server on port 9999) and `pilot1 / Password1` created by the bootstrap. Run the client with `./run-pilot-client.sh`.

---

**AC086.1 + AC086.4 — Successful authentication as Pilot**

1. Run `./run-pilot-client.sh`, enter host `localhost` and port `9999`.
2. Enter credentials `pilot1` / `Password1`.
3. Expected: server responds `OK` and the Pilot menu is displayed.

---

**AC086.4 — Authentication failure (wrong password)**

1. Run `./run-pilot-client.sh` and enter credentials `pilot1` / `wrongpassword`.
2. Expected: client displays `Authentication failed.` and terminates.

---

**AC086.4 — Authorization failure (non-Pilot user)**

1. Run `./run-pilot-client.sh` and enter credentials `atcc1` / `Password1`.
2. Expected: client displays `Authentication failed.` and terminates (server responded `UNAUTHORIZED`).

---

**AC086.3 + AC086.1 — CREATE_FLIGHT_PLAN with valid DSL**

1. Authenticate as `pilot1`.
2. Select option `1` and provide the path `src/test/resources/dsl/valid/01_single_leg_regular.dsl`.
3. Expected: client displays `Flight plan created: TP123`.

---

**AC086.3 — CREATE_FLIGHT_PLAN with invalid DSL**

1. Authenticate as `pilot1`.
2. Select option `1` and provide a file with invalid DSL content.
3. Expected: client displays `Failed: ERROR <parse error description>`; no flight plan is persisted.

---

**AC086.1 — EXIT command**

1. Authenticate as `pilot1` and select option `0`.
2. Expected: session terminates cleanly.
