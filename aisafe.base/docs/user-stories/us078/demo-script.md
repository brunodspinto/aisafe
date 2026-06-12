# Demo Script — US078 (Air Transport Company Collaborator Remote Access)

Step-by-step demonstration plan covering all acceptance criteria, with the exact commands,
inputs and expected outputs, plus the talking points (EAPLI + RCOMP).

> Legend: 🔵 RCOMP (networking) · 🟢 EAPLI (architecture) · ⚪ both

---

## Part 0 — Persistence decision (do this BEFORE the demo)

For a reliable, always-fresh demo, **in-memory** is recommended (no H2 server, and the data
resets to the seeded state on every restart — important, since we deactivate and create routes).

**Option A (recommended) — temporary in-memory:**
1. In `aisafe.base/src/main/resources/application.properties`, set:
   ```properties
   persistence.repositoryFactory=aisafe.infrastructure.persistence.inmemory.InMemoryRepositoryFactory
   ```
2. ⚠️ **Revert to JPA after the demo** (do not commit the in-memory line).

**Option B — JPA (shows the real DB, NFR08):** keep JPA, but **delete the `db\aisafe*` files**
in the repo root to start clean, and start the H2 server (JDK 21) on port 9093 first.
(More steps, more risk in a live demo.)

---

## Part 1 — Credibility first: automated tests passing

Before opening the app, show it is tested. In a terminal:
```powershell
$env:JAVA_HOME = "C:\Users\hugom\.jdks\ms-21.0.11"
cd "C:\Users\hugom\Documents\GitHub\sem4pi2526-sem4pi2526_2dc2\aisafe.base"
& "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" "-Dtest=CollaboratorSessionHandlerTest,CollaboratorSessionHandlerIT,RemoteAccessLoggerTest" test
```
**Say:** "14 US078 tests — unit (in-memory streams), integration (real socket) and the UDP logging — all green."

---

## Part 2 — Open the 3 windows (in this order)

| Window | What runs | How |
|--------|-----------|-----|
| **A** | UDP receiver (US90 simulated) | PowerShell (below) |
| **B** | Server — `AiSafeConsoleApp` | Run in IntelliJ |
| **C** | Client — `CollaboratorTcpClientApp` | Run in IntelliJ |

**Window A — UDP receiver (open first, leave running):**
```powershell
$u = New-Object System.Net.Sockets.UdpClient(9090)
$ep = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Any, 0)
Write-Host "US90 (simulated) listening on UDP 9090..."
while ($true) { $b = $u.Receive([ref]$ep); [Text.Encoding]::ASCII.GetString($b) }
```

**Window B — server:** Run `AiSafeConsoleApp`. Wait for:
```
[TCP Server] Listening on port 9999
```
**Say:** "The TCP server runs embedded in the main app, in the same JVM — it shares the EAPLI
persistence and authentication."

---

## Part 3 — Guided demo (mapped to the Acceptance Criteria)

Each scenario = a fresh run of `CollaboratorTcpClientApp` (Window C). Inputs: host `Enter`, port `Enter`.

### 🟢 AC078.1 + AC078.4 — TCP client + successful authentication
1. Run `CollaboratorTcpClientApp` → `atcc1` / `Password1`.
2. **Window C:** `Authenticated. Welcome, atcc1.` + menu.
3. **Window A:** `... | atcc1 | 127.0.0.1 | <port> | US78 | LOGIN_SUCCESS`

> **Say:** "Dedicated TCP client app (AC078.1). Login is validated server-side via EAPLI. And note —
> as soon as it connects, the **client** sends a UDP datagram to the US90 logging server."

### 🟢 AC078.3 — Read commands (US072, US074)
4. Option **1 (List Fleet):**
   ```
   --- Fleet (1) ---
   CS-TUA | 737-800 | Boeing | 2018 | ACTIVE
   ```
5. Option **2 (List Flight Routes):**
   ```
   --- Routes (2) ---
   TP100 | LIS | OPO | ACTIVE
   TP200 | OPO | LIS | ACTIVE
   ```

> **Say:** "The server handler has no logic — it calls `ListFleetController` and
> `DeactivateFlightRouteController` that **already existed** (US072/US074). Full reuse, zero duplication.
> The company (TAP) is resolved from the authenticated session."

### 🟢 AC078.3 — Write command (US074)
6. Option **3 (Deactivate)** → Route `TP100`, date `2026-12-01`:
   ```
   OK TP100 deactivated from 2026-12-01
   ```
7. Option **2** again → now only `TP200` (TP100 became inactive):
   ```
   --- Routes (1) ---
   TP200 | OPO | LIS | ACTIVE
   ```

> **Say:** "Remote write works and is reflected in the state."

### 🟢 AC078.3 — Business rule enforced over TCP (AC074.3)
8. Option **3 (Deactivate)** → Route `TP200`, date `2026-08-01`:
   ```
   ERROR Cannot deactivate: there are planned flights on this route from 2026-08-01 onwards.
   ```

> **Say (strong point):** "This is rule AC074.3 — a route with planned flights cannot be deactivated.
> It lives in the domain controller and works **intact over the remote channel**. The
> `CollaboratorSessionHandler` only parsed and delegated."

### 🟢 AC078.3 — Create route (US073)
9. Option **4 (Create Route)** → `TP300` / `LIS` / `OPO`:
   ```
   OK TP300
   ```

> **Say:** "Reuse of `CreateFlightRouteController` (US073)."

### 🟢 US90 — Logout
10. Option **0 (Exit).**
11. **Window A:** `... | atcc1 | ... | US78 | LOGOUT`

### 🟢 AC078.4 — Authentication failure (wrong password)
12. Run client → `atcc1` / `wrong`.
13. **Window C:** `Authentication failed.`
14. **Window A:** `... | atcc1 | ... | US78 | LOGIN_FAILED`

### 🟢 AC078.4 — Authorization failure (non-ATCC) ⭐
15. Run client → **`pilot1`** / `Password1` *(correct credentials!)*.
16. **Window C:** `Authentication failed.`
17. **Window A:** `... | pilot1 | ... | US78 | LOGIN_FAILED`

> **Say (strong point):** "`pilot1` has the right password but is a **PILOT, not an ATCC**.
> The collaborator app declares the `ATCC` service in the `LOGIN`, and the shared server replies
> `UNAUTHORIZED`. This enforces AC078.4 — only ATCCs run collaborator commands — without breaking
> the pilot app (US86)."

### 🟢 US90 — Disconnect (optional, impressive)
18. Run client → `atcc1` / `Password1` (enter the menu).
19. **Go to Window B and STOP `AiSafeConsoleApp`** (stop button ⏹).
20. In Window C, choose option **1**:
    ```
    Connection lost: Connection reset
    ```
21. **Window A:** `... | atcc1 | ... | US78 | CONNECTION_LOST`

> **Say:** "All four remote-access events are logged: login OK/failed, logout and connection loss."

---

## Part 4 — Talking points

**🟢 EAPLI (architecture):**
- `CollaboratorSessionHandler` is a delivery mechanism — **no business logic**, only delegates to the
  existing controllers (Information Expert, High Cohesion).
- The **same application layer** serves two channels: console and TCP.
- The ATCC branch was added to `TcpClientDispatcher` by **OCP** (without touching the PILOT path).
- The client imports **no JPA class** — persistence is server-side only (AC078.2).

**🔵 RCOMP (networking):**
- Text, line-oriented TCP protocol; lists use `OK <n>` + n lines (deterministic, no mixing of
  `readLine` with `read`).
- One thread per connection.
- UDP **fire-and-forget** to US90 (open socket, send, close; never blocks the client).
- The `ATCC` service token in the handshake lets the shared server distinguish the service.

---

## Part 5 — After the demo
- If you used in-memory (Option A): **revert** `application.properties` to `JpaRepositoryFactory`.
- Close the 3 windows.

---

## Quick cheat sheet (values)
```
TCP server:    localhost : 9999
UDP logging:   localhost : 9090
Users:         atcc1 / Password1   (ATCC, TAP)   <- primary
               pilot1 / Password1  (PILOT)        <- for UNAUTHORIZED
               atcc1 / wrong                       <- for LOGIN_FAILED
Data:          Fleet:  CS-TUA (737-800)
               Routes: TP100 (LIS->OPO), TP200 (OPO->LIS, has a planned flight)
Deactivate TP100 / 2026-12-01  -> OK
Deactivate TP200 / 2026-08-01  -> ERROR (planned flights)
Create TP300 / LIS / OPO       -> OK
```
