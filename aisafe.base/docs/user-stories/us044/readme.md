# US044 — Weather Person Remote Access

## 1. Context

This US is being implemented for the first time in Sprint 3. It allows a Weather Person to remotely access the AISafe system using a dedicated TCP client application — the **Weather Person Remote App**. The client communicates with a TCP server embedded in the main AISafe application; no direct interaction with the database is permitted from the client side.

The TCP server is shared across all remote access user stories (US044, US078, US086). Each actor connects to the same server on the same port and, after authentication, is granted access only to the commands corresponding to their role. For US044, the role is `WEATHER_PERSON`.

The Weather Person user stories that must be remotely available are US041 (Register weather data), US042 (Import bulk weather data), and US043 (Consult weather data). All three are fully implemented and exposed in Sprint 3.

### 1.1 List of issues

- **Analysis:** Define the TCP protocol, the authentication mechanism, and the set of Weather Person commands to be exposed remotely. Identify the existing classes that can be reused.
- **Design:** Define the architecture — TCP server, `WeatherPersonSessionHandler`, `WeatherPersonClient` and the protocol specification. Produce sequence and class diagrams.
- **Implement:** Implement the Weather Person session handler (`WeatherPersonSessionHandler`), the standalone client application (`WeatherPersonClient`), and the routing entry in `TcpClientDispatcher`.
- **Test:** Unit tests with in-memory streams + manual integration tests — connect client, authenticate, execute commands, verify responses.

---

## 2. Requirements

**US044** As a Weather Person, I want to remotely access the system in order to upload weather data.

**Acceptance Criteria:**

| AC | Description |
|----|-------------|
| AC044.1 | A specific TCP-based network client application is required to communicate with the server application embedded in the system. |
| AC044.2 | The client application interaction with the system must be limited to the TCP connection — no direct interaction with the database is acceptable. |
| AC044.3 | All Weather Person user stories must be remotely available using this client application. |
| AC044.4 | Authentication and authorization must be enforced — only authenticated users with the `WEATHER_PERSON` role may execute Weather Person commands. |

**Weather Person user stories available remotely:**

| US | Description | Status in Sprint 3 |
|----|-------------|-------------------|
| US041 | Register weather data | Implemented — exposed via `REGISTER_WEATHER` command |
| US042 | Import bulk weather data | Implemented — exposed via `IMPORT_BULK` command |
| US043 | Consult weather data | Implemented — exposed via `CONSULT_WEATHER` command |

**Dependencies/References:**

| Dependency | Reason |
|-----------|--------|
| US030 | Authentication and authorization must be in place; the `WEATHER_PERSON` role must exist. |
| US041 | Register Weather Data — `RegisterWeatherDataController` is reused server-side. |
| US042 | Import Bulk Weather Data — `ImportBulkWeatherDataController` is reused server-side. |
| US043 | Consult Weather Data — `ConsultWeatherDataController` is reused server-side. |

---

## 3. Analysis

US044 requires a standalone TCP client application and a TCP server embedded in the main AISafe application. The client connects to the server, authenticates as a Weather Person, and then issues commands that map to existing Weather Person use cases — the server executes those use cases on behalf of the remote user.

### Architecture overview

The TCP server (`AiSafeTcpServer`) runs inside the same JVM as the console application, sharing the same persistence context and EAPLI authentication infrastructure. One `TcpClientDispatcher` thread is spawned per accepted connection; it handles authentication and then delegates to a role-specific session handler. For the `WEATHER_PERSON` role this is `WeatherPersonSessionHandler`.

```
[WeatherPersonClient]  ──TCP──►  [AiSafeTcpServer]
                                        │
                                [TcpClientDispatcher]  (one thread per connection)
                                        │
                                authenticates via AuthenticationContext
                                        │
                                service == "WEATHER" && role == WEATHER_PERSON?
                                        │
                                [WeatherPersonSessionHandler]
                                        │
                    ┌───────────────────┼───────────────────┐
                    │                   │                   │
        [RegisterWeatherDataCtrl]  [ImportBulkCtrl]  [ConsultWeatherDataCtrl]
```

### TCP Protocol

The protocol is text-based and line-oriented (UTF-8, `\n` terminated).

**Authentication phase:**

```
C→S:  LOGIN <username> <password> WEATHER
S→C:  OK
  or
S→C:  FAIL <reason>
  or
S→C:  UNAUTHORIZED
```

**Command phase (after successful LOGIN as WEATHER_PERSON):**

```
# Register single weather record (US041)
C→S:  REGISTER_WEATHER <areaCode> <provider> <format> <dateTime> <temp> <windSpeed> <windDir> <pressure> <visibility>
S→C:  OK <id>
  or
S→C:  ERROR <message>

# Import bulk CSV weather data (US042)
C→S:  IMPORT_BULK <charLength>
S→C:  READY
C→S:  <exactly charLength characters of CSV content>
S→C:  OK saved=N failures=M | [msg1] [msg2]...
  or
S→C:  ERROR <message>

# Consult weather data by date and area (US043)
C→S:  CONSULT_WEATHER <date(ISO-8601)> <areaCode>
S→C:  <record1>
      <record2>
      ...
      <empty line>
  or
S→C:  ERROR <message>
      <empty line>

# List available air control areas (helper)
C→S:  LIST_AREAS
S→C:  <areaCode1>
      ...
      <empty line>

# End session
C→S:  EXIT
S→C:  BYE
```

Any unrecognised command receives:
```
S→C:  UNKNOWN_COMMAND
```

### Reuse of existing classes

- `REGISTER_WEATHER` delegates to `RegisterWeatherDataController`.
- `IMPORT_BULK` writes received CSV to a temporary file and delegates to `ImportBulkWeatherDataController`.
- `CONSULT_WEATHER` delegates to `ConsultWeatherDataController`.
- Authentication reuses `AuthenticationContext.authenticate(username, password)` and `AuthenticationContext.hasRole(AiSafeRoles.WEATHER_PERSON)`.

### Key design decisions

**Thread-per-connection** — each accepted `Socket` is handled by a dedicated `TcpClientDispatcher` thread. This supports concurrent sessions without blocking the server's accept loop.

**Role-specific session handler** — the dispatcher handles only authentication and role resolution; all Weather Person command logic lives in `WeatherPersonSessionHandler`. This keeps each class focused on a single responsibility.

**Handshake for bulk transfer** — the `IMPORT_BULK` command uses a two-step handshake (`IMPORT_BULK <length>` → `READY` → CSV content) to safely transfer multi-line CSV data over the line-oriented protocol.

**Temporary file for bulk import** — the server writes the received CSV bytes to a `Files.createTempFile(...)` and passes the path to `ImportBulkWeatherDataController`. The temp file is deleted after the controller returns.

### Main classes identified

| Class | Type | Responsibility |
|-------|------|----------------|
| `AiSafeTcpServer` | Server | Opens `ServerSocket` on port 9999; accepts connections; spawns `TcpClientDispatcher` threads |
| `TcpClientDispatcher` | `Runnable` | Handles one client connection: reads `LOGIN`, authenticates, checks role, delegates to session handler |
| `WeatherPersonSessionHandler` | Session Handler | Handles Weather Person commands: `REGISTER_WEATHER`, `IMPORT_BULK`, `CONSULT_WEATHER`, `LIST_AREAS`, `EXIT` |
| `WeatherPersonTcpClient` | Client wrapper | TCP protocol facade: `login()`, `registerWeather()`, `importBulk()`, `consultWeather()`, `listAreas()`, `exit()` |
| `WeatherPersonTcpClientApp` | Client entry point | Standalone interactive menu + UDP event logging (US090) |
| `AuthenticationContext` *(existing)* | Auth adapter | Authenticates credentials via EAPLI; verifies role |
| `RegisterWeatherDataController` *(existing)* | Controller | Registers a single weather data record; reused server-side |
| `ImportBulkWeatherDataController` *(existing)* | Controller | Imports bulk CSV weather data; reused server-side |
| `ConsultWeatherDataController` *(existing)* | Controller | Queries weather data by date and area; reused server-side |
| `AiSafeRoles.WEATHER_PERSON` *(existing)* | Role constant | Used in dispatcher to verify that the authenticated user is a Weather Person |

---

## 4. Design

### 4.1. Realization

The flow is divided into two phases: **authentication** and **command execution**.

**Authentication phase:**

1. `WeatherPersonClient` starts, connects to the server host and port.
2. The user enters credentials; the client sends `LOGIN <username> <password> WEATHER` over the socket.
3. Server-side, `TcpClientDispatcher` reads the `LOGIN` line and calls `AuthenticationContext.authenticate(username, password)`.
4. If authentication fails, the server responds `FAIL invalid credentials` and closes the connection.
5. If the user is authenticated but does not have the `WEATHER_PERSON` role, the server responds `UNAUTHORIZED` and closes the connection.
6. If authentication succeeds and the service token is `WEATHER`, the server responds `OK` and instantiates a `WeatherPersonSessionHandler`.

**Command phase — `REGISTER_WEATHER`:**

7. The Weather Person selects "Register weather data" and provides the required fields.
8. The client sends `REGISTER_WEATHER <fields...>`.
9. `WeatherPersonSessionHandler` parses the fields and calls `RegisterWeatherDataController`.
10. On success the server responds `OK <id>`; on failure `ERROR <message>`.

**Command phase — `IMPORT_BULK`:**

7. The Weather Person provides a local CSV file path.
8. The client reads the file, computes its character length, and sends `IMPORT_BULK <charLength>`.
9. The server responds `READY`.
10. The client streams exactly `charLength` characters of CSV content.
11. `WeatherPersonSessionHandler` writes the content to a temporary file and calls `ImportBulkWeatherDataController`.
12. The server responds `OK saved=N failures=M | [msgs]` or `ERROR <message>`.

**Command phase — `CONSULT_WEATHER`:**

7. The Weather Person provides a date (ISO-8601) and area code.
8. The client sends `CONSULT_WEATHER <date> <areaCode>`.
9. `WeatherPersonSessionHandler` calls `ConsultWeatherDataController` and sends one record per line, terminated by an empty line.

**Exit:**

- The Weather Person selects "Exit"; the client sends `EXIT` and the server responds `BYE`.

### 4.2. Acceptance Tests

Tests are in `src/test/java/aisafe/tcpserver/weatherperson/WeatherPersonSessionHandlerTest.java` — unit tests using in-memory streams (`StringReader` / `StringWriter`), no socket or EAPLI context required.

---

**AC044.1 — EXIT command returns BYE**

```java
@Test
void ensureExitCommandReturnsBye() throws IOException {
    final String response = runSession("EXIT");
    assertEquals("BYE", response);
}
```

---

**AC044.1 — Unknown command returns UNKNOWN_COMMAND**

```java
@Test
void ensureUnknownCommandReturnsUnknownCommand() throws IOException {
    final String response = runSession("HELLO\nEXIT");
    assertTrue(response.contains("UNKNOWN_COMMAND"));
}
```

---

**AC044.1 — REGISTER_WEATHER with missing parameters returns ERROR**

```java
@Test
void ensureRegisterWeatherWithMissingParamsReturnsError() throws IOException {
    final String response = runSession("REGISTER_WEATHER\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

---

**AC044.1 — IMPORT_BULK with invalid length returns ERROR**

```java
@Test
void ensureImportBulkWithInvalidLengthReturnsError() throws IOException {
    final String response = runSession("IMPORT_BULK abc\nEXIT");
    assertTrue(response.contains("ERROR"));
}
```

---

**AC044.4 — Successful authentication as Weather Person (manual)**

1. Run `WeatherPersonClient`, enter `localhost` / `9999` / `weather_person` / `Password1`.
2. Expected: server responds `OK` and the Weather Person menu is displayed.

---

**AC044.4 — Authentication failure — wrong password (manual)**

1. Run `WeatherPersonClient` and enter credentials `weather_person` / `wrongpassword`.
2. Expected: client displays login failed message and terminates.

---

**AC044.4 — Authorization failure — non-Weather-Person user (manual)**

1. Run `WeatherPersonClient` and enter credentials `pilot1` / `Password1`.
2. Expected: client displays login failed (`UNAUTHORIZED`) and terminates.

---

## 5. Implementation

| Package | Class | Role |
|---------|-------|------|
| `aisafe.tcpserver` | `AiSafeTcpServer` | Opens `ServerSocket` on port 9999; accepts connections; spawns `TcpClientDispatcher` daemon threads |
| `aisafe.tcpserver` | `TcpClientDispatcher` | Handles one connection: reads `LOGIN`, authenticates, checks `WEATHER` service + `WEATHER_PERSON` role, delegates to `WeatherPersonSessionHandler` |
| `aisafe.tcpserver.weatherperson` | `WeatherPersonSessionHandler` | Command loop: `REGISTER_WEATHER`, `IMPORT_BULK`, `CONSULT_WEATHER`, `LIST_AREAS`, `EXIT`, `UNKNOWN_COMMAND` |
| `aisafe.app.weatherperson` | `WeatherPersonTcpClient` | TCP protocol wrapper: `login()`, `registerWeather()`, `importBulk()`, `consultWeather()`, `listAreas()`, `exit()`; implements `Closeable` |
| `aisafe.app.weatherperson` | `WeatherPersonTcpClientApp` | Standalone client entry point; interactive menu + UDP access logging via `RemoteAccessLogger` (`SERVICE_ID = "US44"`) |
| `aisafe.app.logging` | `RemoteAccessLogger` *(existing)* | Fire-and-forget UDP emitter for remote-access events (LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, CONNECTION_LOST) |
| `aisafe.app.console` | `AiSafeConsoleApp` | Starts `AiSafeTcpServer` in a daemon thread before the console menu |
| `aisafe.app.console` | `AiSafeBootstrap` | Creates `weather_person / Password1` (role `WEATHER_PERSON`) |

The `IMPORT_BULK` command uses a two-step handshake and writes the received CSV to a temp file before delegating to `ImportBulkWeatherDataController`:

```java
tempFile = Files.createTempFile("aisafe-weather-bulk-", ".csv");
Files.writeString(tempFile, csvContent);
final ImportResult result = new ImportBulkWeatherDataController().importFromFile(tempFile.toString());
out.println("OK saved=" + result.saved() + " failures=" + result.failures() + " | " + ...);
```

---

## 6. Integration/Demonstration

**Prerequisites:** Run from the `aisafe.base` directory with Maven 3.9+ and Java 21. The bootstrap creates `weather_person / Password1` automatically on first run.

**Start the server:**

1. Run `AiSafeConsoleApp` — the TCP server starts on port 9999 automatically.

**Connect and authenticate:**

2. Run `WeatherPersonTcpClientApp` in a separate terminal.
3. Enter host `localhost` and port `9999`.
4. Enter credentials `weather_person` / `Password1`.
5. The server responds `OK` and the Weather Person menu is displayed:
   ```
   ===== Weather Person Menu =====
    1 - Register weather data     (US041)
    2 - Import bulk weather data  (US042)
    3 - Consult weather data      (US043)
    4 - List air control areas
    0 - Exit
   ```

**Register weather data (US041):**

6. Select option `1` and provide the required fields (area code, provider, format, date-time, temperature, wind speed, wind direction, pressure, visibility).
7. Expected: server responds `OK <id>`.

**Import bulk weather data (US042):**

6. Select option `2` and provide the path to a CSV file.
7. Expected: server responds `OK saved=N failures=M`.

**Consult weather data (US043):**

6. Select option `3` and provide a date (e.g. `2026-06-01`) and area code (e.g. `PT-N`).
7. Expected: server streams matching records, terminated by empty line.

**Authentication failure scenario:**

- Attempt login with wrong credentials → server responds `FAIL invalid credentials` and closes.
- Attempt login with a non-Weather-Person account → server responds `UNAUTHORIZED` and closes.

---

## 7. Observations

- The TCP server is shared across US044, US078, and US086. The `TcpClientDispatcher` distinguishes the three by the service token in the `LOGIN` command (`WEATHER`, `ATCC`, or absent/other for Pilot). Each role dispatches to a dedicated session handler, keeping role-specific command logic isolated.
- The `TcpClientDispatcher` always calls `AuthenticationContext.clear()` in a `finally` block to ensure the EAPLI session is released even if the connection is closed unexpectedly.
- The `IMPORT_BULK` two-step handshake ensures the server knows exactly how many characters to read, which is necessary because CSV data spans multiple lines and a plain line-by-line read would not know where the content ends.
- `WeatherPersonTcpClientApp` is a standalone application with its own `main` method. It has no dependency on any JPA or repository class — all persistence is performed exclusively server-side (AC044.2).
- `WeatherPersonTcpClientApp` uses `RemoteAccessLogger` with `SERVICE_ID = "US44"` to emit UDP events to the US090 logging server on every login attempt, logout, and unexpected disconnect.
- `LIST_AREAS` is a helper command (not part of any specific US) that allows the client to show the user the valid area codes before registering or consulting data.
