# US086 — Tests and Coverage

## Scope

US086 covers remote access to the AISafe system by a Pilot via a dedicated TCP client application. The tests verify the TCP protocol (authentication and command execution), role-based access control, and the end-to-end flow of the `CREATE_FLIGHT_PLAN` command.

## Automated Tests

US086 introduces no new domain classes — the TCP server and dispatcher are infrastructure components whose behaviour is validated through manual integration testing. Domain-level flight plan creation (parsing, validation, persistence) is covered by the automated test suite of US081.

---

## Coverage by Acceptance Criterion

- **AC086.1** (TCP client required): Manual test — connect `PilotTcpClientApp` to `AiSafeTcpServer`; verify the session is established and commands are exchanged over the TCP connection.
- **AC086.2** (no direct DB access from client): Structural — `PilotTcpClientApp` holds no reference to any JPA/repository class; all persistence is performed server-side by `CreateFlightPlanFromFileController`. Verified by code inspection.
- **AC086.3** (all Pilot USs available remotely): Manual test — `CREATE_FLIGHT_PLAN` command is exposed for US081. US082 and US085 are not yet implemented in Sprint 3 and are therefore not exposed.
- **AC086.4** (authentication and authorization enforced): Manual tests — invalid credentials receive `FAIL`; a user without the `PILOT` role receives `UNAUTHORIZED`; only an authenticated Pilot may execute commands.

---

## Acceptance Tests

**Prerequisites:** Run `AiSafeConsoleApp` (the TCP server starts on port 9999 at boot). At least one Pilot user must exist — use `pilot1 / Password1` created by the bootstrap. A valid DSL file must be available locally.

---

**Manual test — AC086.4 / AC086.1 (successful authentication):**

1. Start `PilotTcpClientApp`, enter host `localhost` and port `9999`.
2. Enter credentials `pilot1` / `Password1`.
3. Expected: server responds `OK` and the client displays the Pilot menu.

---

**Manual test — AC086.4 (authentication failure — wrong password):**

1. Start `PilotTcpClientApp` and attempt login with `pilot1` / `wrongpassword`.
2. Expected: server responds `FAIL invalid credentials` and the connection is closed.

---

**Manual test — AC086.4 (authorization failure — non-Pilot user):**

1. Start `PilotTcpClientApp` and attempt login with a Backoffice Operator user (e.g., `backoffice1` / `Password1`).
2. Expected: server responds `UNAUTHORIZED` and the connection is closed.

---

**Manual test — AC086.3 / AC086.1 (CREATE_FLIGHT_PLAN — valid DSL):**

1. Authenticate as `pilot1`.
2. Select "Create Flight Plan from DSL File" and provide the path to a valid DSL file, e.g.:
   ```
   FLIGHT TP800 TYPE REGULAR {
     LEG {
       DEPARTURE: OPO 2026-07-04 08:00;
       ARRIVAL: MAD 2026-07-04 09:30;
       ROUTE: OPO -> MAD;
       SEGMENT {
         START: (+41.15, -8.61);
         END: (+40.49, -3.56);
         ALTITUDE: 35000 FT WIDTH: 5 KM;
         WIND: (270, 30 KNOT);
       }
       FUEL: 6000 KG;
     }
   }
   ```
3. Expected: server responds `OK TP800` and the client displays the designator.

---

**Manual test — AC086.3 (CREATE_FLIGHT_PLAN — invalid DSL):**

1. Authenticate as `pilot1`.
2. Select "Create Flight Plan from DSL File" and provide a file with a syntax error (e.g., missing semicolon after a field).
3. Expected: server responds `ERROR` with a description of the parse error; no flight plan is persisted.

---

**Manual test — AC086.1 (unknown command):**

1. Using `telnet localhost 9999`, authenticate as `pilot1`.
2. Send an unrecognised command, e.g. `HELLO`.
3. Expected: server responds `UNKNOWN_COMMAND` and the session remains open.

---

**Manual test — AC086.1 (EXIT command):**

1. Authenticate as `pilot1`.
2. Select "Exit" in the client menu.
3. Expected: server responds `BYE` and both client and server close the connection cleanly.
