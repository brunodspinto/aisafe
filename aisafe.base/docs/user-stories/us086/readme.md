# US086 — Pilot Remote Access

## 1. Context

This US is being implemented for the first time in Sprint 3. It allows a Pilot to remotely access the AISafe system using a dedicated TCP client application — the **Pilot Remote App**. The client communicates with a TCP server embedded in the main AISafe application; no direct interaction with the database is permitted from the client side.

The TCP server is shared across all remote access user stories (US044, US078, US086). Each actor connects to the same server on the same port and, after authentication, is granted access only to the commands corresponding to their role. For US086, the role is `PILOT`.

The Pilot user stories that must be remotely available are US081 (Create a flight plan from a DSL file), US082 (Insert weather data in a flight) and US085 (Test/validate a flight plan). Of these, only US081 is fully implemented in Sprint 3.

### 1.1 List of issues

- **Analysis:** Define the TCP protocol, the authentication mechanism, and the set of Pilot commands to be exposed remotely. Identify the existing classes that can be reused.
- **Design:** Define the architecture — TCP server, `PilotSessionHandler`, `PilotTcpClient` and the protocol specification. Produce sequence and class diagrams.
- **Implement:** Implement the TCP server (`AiSafeTcpServer`), the per-connection dispatcher (`TcpClientDispatcher`), the pilot session handler (`PilotSessionHandler`), and the standalone client application (`PilotTcpClientApp`).
- **Test:** Manual integration tests — connect client, authenticate, execute Pilot commands, verify responses.

---

## 2. Requirements

**US086** As a Pilot, I want to remotely access the system using the Air Transport Company App.

**Acceptance Criteria:**

| AC | Description |
|----|-------------|
| AC086.1 | A specific TCP-based network client application is required to communicate with the server application embedded in the system. |
| AC086.2 | The client application interaction with the system must be limited to the TCP connection — no direct interaction with the database is acceptable. |
| AC086.3 | All Pilot user stories must be remotely available using this client application. |
| AC086.4 | Authentication and authorization must be enforced — only authenticated users with the `PILOT` role may execute Pilot commands. |

**Pilot user stories available remotely:**

| US | Description | Status in Sprint 3 |
|----|-------------|-------------------|
| US081 | Create a flight plan from a DSL file | Implemented — exposed via `CREATE_FLIGHT_PLAN` command |
| US082 | Insert weather data in a flight | Not yet implemented |
| US085 | Test/validate a flight plan | Not yet implemented |

**Dependencies/References:**

| Dependency | Reason |
|-----------|--------|
| US030 | Authentication and authorization must be in place; the `PILOT` role must exist. |
| US081 | Create a Flight Plan from a File — the controller `CreateFlightPlanFromFileController` is reused server-side to handle the `CREATE_FLIGHT_PLAN` command. |
| US075 | Add a Pilot — a pilot user must exist in the system before remote access can be tested. |

---

## 3. Analysis

*To be completed in the next commit.*

## 4. Design

*To be completed in the next commit.*

### 4.1. Realization

*To be completed in the next commit.*

### 4.2. Acceptance Tests

*To be completed in the next commit.*

## 5. Implementation

*To be completed in the next commit.*

## 6. Integration/Demonstration

*To be completed in the next commit.*

## 7. Observations

*To be completed in the next commit.*
