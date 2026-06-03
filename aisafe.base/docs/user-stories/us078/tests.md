# US078 — Tests and Coverage

## Scope

US078 covers remote access to the AISafe system by an Air Transport Company Collaborator (ATCC) via a dedicated TCP client application. The tests verify the TCP protocol (command dispatch, error handling), role-based access control, and the end-to-end flow of representative collaborator commands. As US078 is a delivery mechanism that reuses already-tested controllers, the automated tests focus on the **protocol/session layer** and the **client-side UDP logging**; the business rules themselves are covered by the unit tests of US070–US075.

---

## Automated Tests

### `CollaboratorSessionHandlerTest`

Location: `src/test/java/aisafe/tcpserver/collaborator/CollaboratorSessionHandlerTest.java`

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

**Test:** `ensureDeactivateRouteWithoutArgumentsReturnsError`

```java
@Test
void ensureDeactivateRouteWithoutArgumentsReturnsError() throws IOException {
    final String response = runSession("DEACTIVATE_ROUTE\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureDeactivateRouteWithInvalidDateReturnsError`

```java
@Test
void ensureDeactivateRouteWithInvalidDateReturnsError() throws IOException {
    final String response = runSession("DEACTIVATE_ROUTE TP100 not-a-date\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

**Test:** `ensureCreateRouteWithMissingArgumentsReturnsError`

```java
@Test
void ensureCreateRouteWithMissingArgumentsReturnsError() throws IOException {
    final String response = runSession("CREATE_ROUTE TP500 LIS\nEXIT");
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

---

### `CollaboratorSessionHandlerIT`

Location: `src/test/java/aisafe/tcpserver/collaborator/CollaboratorSessionHandlerIT.java`

Implementation tests that exercise `CollaboratorSessionHandler` over a real loopback TCP socket. A `ServerSocket` on a random port is started in `@BeforeEach` and torn down in `@AfterEach`. These tests verify that the protocol behaves correctly over actual network I/O.

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

**Test:** `ensureDeactivateRouteWithInvalidDateOverRealSocketReturnsError`

```java
@Test
void ensureDeactivateRouteWithInvalidDateOverRealSocketReturnsError() throws Exception {
    clientOut.println("DEACTIVATE_ROUTE TP100 not-a-date");
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

---

### `RemoteAccessLoggerTest`

Location: `src/test/java/aisafe/app/collaborator/RemoteAccessLoggerTest.java`

Verifies the client-side UDP datagram payload format (US090). A `DatagramSocket` bound to a random loopback port receives the datagram and the test asserts the pipe-delimited fields.

**Test:** `ensureLoginSuccessDatagramHasExpectedFormat`

```java
@Test
void ensureLoginSuccessDatagramHasExpectedFormat() throws Exception {
    logger.log("atcc1", "127.0.0.1", 50231, "US78", "LOGIN_SUCCESS");
    final String payload = receiveOnePacket();           // blocks on the test DatagramSocket
    final String[] fields = payload.split("\\|");
    assertEquals(6, fields.length);
    assertEquals("atcc1", fields[1].trim());
    assertEquals("US78", fields[4].trim());
    assertEquals("LOGIN_SUCCESS", fields[5].trim());
}
```

---

## Coverage by Acceptance Criterion

- **AC078.1** (TCP client + command loop): `ensureExitCommandReturnsBye`, `ensureSessionHandlesUnknownCommandBeforeExit` + manual test (successful session)
- **AC078.2** (no direct DB access from client): structural — `CollaboratorTcpClientApp` has only JDK imports; verified by code inspection
- **AC078.3** (ATCC USs available remotely): `ensureDeactivateRouteWithoutArgumentsReturnsError`, `ensureDeactivateRouteWithInvalidDateReturnsError`, `ensureCreateRouteWithMissingArgumentsReturnsError` + manual tests (valid LIST_FLEET / DEACTIVATE_ROUTE flows)
- **AC078.4** (authentication and authorization): manual tests (wrong password → `FAIL`; non-ATCC role → `UNAUTHORIZED`)
- **Protocol robustness**: `ensureUnknownCommandReturnsUnknownCommand`, `ensureMultipleUnknownCommandsAreEachRejected`
- **Client-side UDP logging (US090 dependency)**: `ensureLoginSuccessDatagramHasExpectedFormat`

---

## Acceptance Tests

**Prerequisites:** `AiSafeConsoleApp` running (TCP server on port 9999) and `atcc1 / Password1` created by the bootstrap. Run the client with `./run-collaborator-client.sh` (or `CollaboratorTcpClientApp`).

---

**AC078.1 + AC078.4 — Successful authentication as ATCC**

1. Run the client, enter host `localhost` and port `9999`.
2. Enter credentials `atcc1` / `Password1`.
3. Expected: server responds `OK`, the collaborator menu is displayed, and the client emits a `LOGIN_SUCCESS` UDP event.

---

**AC078.4 — Authentication failure (wrong password)**

1. Run the client and enter credentials `atcc1` / `wrongpassword`.
2. Expected: client displays `Authentication failed.` and terminates; the client emits a `LOGIN_FAILED` UDP event.

---

**AC078.4 — Authorization failure (non-ATCC user)**

1. Run the client and enter credentials `pilot1` / `Password1`.
2. Expected: client displays `Authentication failed.` and terminates (server responded `UNAUTHORIZED`).

---

**AC078.3 — LIST_FLEET**

1. Authenticate as `atcc1`, select "List Fleet".
2. Expected: the client prints the TAP fleet (e.g. `CS-TUA 737-800 ACTIVE`).

---

**AC078.3 — DEACTIVATE_ROUTE (valid)**

1. Authenticate as `atcc1`, select "Deactivate Flight Route", enter `TP100` and `2026-08-01`.
2. Expected: client displays `OK TP100 deactivated from 2026-08-01`.

---

**AC078.3 — DEACTIVATE_ROUTE (rejected by planned flight)**

1. Authenticate as `atcc1`, select "Deactivate Flight Route", enter `TP200` and a date on/before the seeded `TP2001` departure.
2. Expected: client displays `ERROR Cannot deactivate: there are planned flights ...`.

---

**AC078.1 — EXIT command (clean logout)**

1. Authenticate as `atcc1` and select "Exit".
2. Expected: session terminates cleanly (server responds `BYE`); the client emits a `LOGOUT` UDP event.