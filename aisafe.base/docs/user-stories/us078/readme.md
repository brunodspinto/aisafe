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

