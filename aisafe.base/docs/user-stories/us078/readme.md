# US078 — Air Transport Company Collaborator Remote Access

## 1. Context

This US is implemented in Sprint 3. It allows an **Air Transport Company Collaborator (ATCC)** to remotely access the AISafe system using a dedicated TCP client application — the **Air Transport Company App**. The client communicates with a TCP server embedded in the main AISafe application; no direct interaction with the database is permitted from the client side.

The TCP server is shared across all remote-access user stories (US044, US078, US086). Each actor connects to the same server on the same port and, after authentication, is granted access only to the commands corresponding to their role. For US078, the role is `ATCC`.

The ATCC user stories that must be remotely available are the company-management use cases already implemented in previous sprints: List Fleet (US072), Create / Deactivate Flight Route (US073/US074), Decommission Aircraft (US071), Add Aircraft (US070) and Add Pilot (US075). US078 adds **no new business logic** — it is a delivery mechanism that invokes the existing application controllers server-side.

### 1.1 List of issues

- **Analysis:** Define the TCP protocol, the authentication mechanism, and the set of ATCC commands to be exposed remotely. Identify the existing controllers to reuse.
- **Design:** Define the architecture — the shared TCP server, the new `CollaboratorSessionHandler`, the `CollaboratorTcpClient`/`App`, the client-side UDP remote-access logging, and the protocol specification. Produce sequence and class diagrams.
- **Implement:** Add the `ATCC` branch to `TcpClientDispatcher`, implement `CollaboratorSessionHandler`, the standalone client `CollaboratorTcpClientApp`, and the `RemoteAccessLogger` (client-side UDP, US090).
- **Test:** Unit tests on the session handler (in-memory streams), implementation tests over a real loopback socket, and manual integration tests.

---

## 2. Requirements

**US078** As an Air Transport Company Collaborator, I want to remotely access the system using the Air Transport Company App.

**Acceptance Criteria:**

| AC      | Description                                                                                                                                          |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| AC078.1 | A specific TCP-based network client application is required to communicate with the server application embedded in the system.                       |
| AC078.2 | The client application interaction with the system must be limited to the TCP connection — any direct interaction with the database is unacceptable. |
| AC078.3 | All the Air Transport Company Collaborator user stories must be remotely available using this client application.                                    |
| AC078.4 | Authentication and authorization must be enforced — only authenticated users with the `ATCC` role may execute collaborator commands.                 |

**ATCC user stories available remotely:**

| US    | Description               | Command            | Reused Controller                 |
|-------|---------------------------|--------------------|-----------------------------------|
| US072 | List the company's fleet  | `LIST_FLEET`       | `ListFleetController`             |
| US074 | List active flight routes | `LIST_ROUTES`      | `DeactivateFlightRouteController` |
| US074 | Deactivate a flight route | `DEACTIVATE_ROUTE` | `DeactivateFlightRouteController` |
| US073 | Create a flight route     | `CREATE_ROUTE`     | `CreateFlightRouteController`     |

> Scope note: this iteration exposes the four commands above. The remaining ATCC use cases (US070 Add Aircraft → `ADD_AIRCRAFT`, US071 Decommission → `DECOMMISSION_AIRCRAFT`, US075 Add Pilot → `ADD_PILOT`) are foreseen as additional commands and will be wired through the same `CollaboratorSessionHandler` extension point.

**Dependencies/References:**

| Dependency    | Reason                                                                                                                    |
|---------------|---------------------------------------------------------------------------------------------------------------------------|
| US030         | Authentication and authorization must be in place; the `ATCC` role must exist.                                            |
| US060 / US061 | The ATCC is a `Collaborator` linked to an `AirTransportCompany`; the company is resolved from the authenticated session.  |
| US070–US075   | The collaborator use-case controllers reused server-side must already exist.                                              |
| US086         | Provides the shared TCP skeleton (`AiSafeTcpServer`, `TcpClientDispatcher`, `AuthenticationContext`) reused by US078.     |
| US090         | The Remote Accesses Logging Server receives the UDP datagrams emitted by the US078 client app on login/logout/disconnect. |

---

## 3. Analysis

US078 requires a standalone TCP client application and reuses the TCP server embedded in the main AISafe application. The client connects to the server, authenticates as an ATCC, and then issues commands that map to existing collaborator use cases — the server executes those use cases on behalf of the remote user.

### Architecture overview

The TCP server (`AiSafeTcpServer`) runs inside the same JVM as the console application, sharing the same persistence context and EAPLI authentication infrastructure. One `TcpClientDispatcher` thread is spawned per accepted connection; it handles authentication and then delegates to a role-specific session handler. For the `ATCC` role this is the new `CollaboratorSessionHandler`. The UDP remote-access logging is emitted by the **client** application.

```
[CollaboratorTcpClientApp]  ──TCP──►  [AiSafeTcpServer]
                                             │
                                     [TcpClientDispatcher]  (one thread per connection)
                                             │  authenticates via AuthenticationContext
                                             │  role == ATCC?
                                             ▼
                                     [CollaboratorSessionHandler]
                                             │
                          ┌──────────────────┼─────────────────────┐
                          ▼                   ▼                     ▼
            [ListFleetController]  [DeactivateFlightRouteController]  [CreateFlightRouteController] ...
                          │                   │                     │
                          ▼                   ▼                     ▼
            [AircraftRepository]   [FlightRouteRepository]   (existing repositories)

   [CollaboratorTcpClientApp] ─(login/logout/disconnect)─► [RemoteAccessLogger] ──UDP──► [US090 Logging Server]
```

### TCP Protocol

The protocol is text-based and line-oriented (UTF-8, `\n` terminated), identical in spirit to US086. This keeps it simple to implement, test with `telnet`/`nc`, and read in logs.

**Authentication phase:**

```
C→S:  LOGIN <username> <password>
S→C:  OK
  or
S→C:  FAIL <reason>          (invalid credentials)
  or
S→C:  UNAUTHORIZED           (authenticated but not an ATCC)
```

**Command phase (after successful LOGIN as ATCC):**

```
# List the company's fleet
C→S:  LIST_FLEET
S→C:  FLEET <count>
S→C:  <registration> <model> <status>      (one line per aircraft)
S→C:  END

# List active flight routes
C→S:  LIST_ROUTES
S→C:  ROUTES <count>
S→C:  <routeName> <origin> <destination>   (one line per active route)
S→C:  END

# Deactivate a flight route
C→S:  DEACTIVATE_ROUTE <routeName> <YYYY-MM-DD>
S→C:  OK <routeName> deactivated from <date>
  or
S→C:  ERROR <message>

# Create a flight route
C→S:  CREATE_ROUTE <routeName> <originIATA> <destinationIATA>
S→C:  OK <routeName>
  or
S→C:  ERROR <message>

# End session
C→S:  EXIT
S→C:  BYE
```

Any unrecognised command receives `UNKNOWN_COMMAND`.

### Reuse of existing classes

`CollaboratorSessionHandler` contains **no business logic** — each command is delegated to the corresponding existing application controller (`ListFleetController`, `DeactivateFlightRouteController`, `CreateFlightRouteController`, …). Those controllers already enforce the ATCC role (`authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC)`) and resolve the collaborator's company from the authenticated session, so the same `atcc1 → TAP` ownership rules apply remotely exactly as in the console.

Authentication reuses `AuthenticationContext.authenticate(username, password)` (EAPLI), and role verification uses `AuthenticationContext.hasRole(AiSafeRoles.ATCC)`.

### Remote-access logging (UDP, US090)

On each authentication outcome and at session end, the **client application** (`CollaboratorTcpClientApp`) emits a fire-and-forget UDP datagram to the US090 logging server through `RemoteAccessLogger`. The client knows its own service id (`US78`) and local IP/port (`socket.getLocalAddress()` / `getLocalPort()`); it logs `LOGIN_SUCCESS`/`LOGIN_FAILED` after reading the login reply, `LOGOUT` on clean exit, and `CONNECTION_LOST` on `IOException`. An abruptly killed client cannot emit a datagram — an accepted limitation (only detectable disconnects are reported). The agreed payload is ASCII, pipe-delimited:

```
<timestamp> | <username> | <clientIP> | <clientPort> | <serviceId> | <EVENT>
```

`serviceId = US78`; `EVENT ∈ { LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, CONNECTION_LOST }`.

### Key design decisions

**Reuse of the shared server** — US078 does not create a new server; it adds an `ATCC` branch to the existing `TcpClientDispatcher`, which already foresaw this extension point (OCP).

**Role-specific session handler** — all ATCC command logic lives in `CollaboratorSessionHandler`, isolated from the dispatcher and from the other roles' handlers.

**Client-side UDP logging** — the UDP datagram is sent by the client app, not the server, because the client owns its service identity (`US78`) and its local IP/port, and because the UDP networking is precisely the exercise of the remote-access client (US044/US078/US086).

**No new domain objects** — the TCP/UDP layer is infrastructure (a delivery mechanism); all aggregates already exist in Domain Model V8.

### Main classes identified

| Class                                                                                                   | Type                     | Responsibility                                                                                                           |
|---------------------------------------------------------------------------------------------------------|--------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `CollaboratorTcpClientApp`                                                                              | Client Main              | Standalone client entry point; interactive ATCC menu; emits UDP log events                                               |
| `CollaboratorTcpClient`                                                                                 | Client                   | Encapsulates TCP communication: `login()`, `listFleet()`, `listRoutes()`, `deactivateRoute()`, `createRoute()`, `exit()` |
| `RemoteAccessLogger`                                                                                    | UDP logger (client-side) | Lives in the client app (`aisafe.app.collaborator`); sends remote-access events to the US090 logging server              |
| `AiSafeTcpServer` *(existing)*                                                                          | Server                   | Opens `ServerSocket` on port 9999; accepts connections; spawns dispatcher threads                                        |
| `TcpClientDispatcher` *(modified)*                                                                      | `Runnable`               | Authenticates one connection; adds the `ATCC` branch delegating to `CollaboratorSessionHandler`                          |
| `CollaboratorSessionHandler`                                                                            | Session Handler          | ATCC command loop; delegates each command to an existing controller                                                      |
| `AuthenticationContext` *(existing)*                                                                    | Auth adapter             | Authenticates credentials via EAPLI; verifies role                                                                       |
| `ListFleetController`, `DeactivateFlightRouteController`, `CreateFlightRouteController`, … *(existing)* | Controllers              | Execute the collaborator use cases; reused server-side                                                                   |
| `AiSafeRoles.ATCC` *(existing)*                                                                         | Role constant            | Used in the dispatcher to authorize collaborator access                                                                  |

The following domain model excerpt shows the aggregates touched by this use case:

![Domain Model](svg/US078-domain-model.svg)

