# US091 — Remote Accesses Logging Visualization

## 1. Context

This US is being implemented for the first time in Sprint 3. It adds an **HTTP server** to the Remote Accesses Logging Server application so that an Administrator can view remote access logs using a standard web browser — without installing any dedicated tool.

The Remote Accesses Logging Server is a standalone application that runs on a dedicated cloud node (see the deployment diagram in the RCOMP project description). It receives remote-access events via UDP (US90, teammate's responsibility) and exposes them via HTTP (US91).

US90 and US91 share the same in-memory store (`AccessEventStore`) and the same `AccessEvent` model. US90 fills the store; US91 reads from it.

### 1.1 List of issues

- **Analysis:** Identify the data to be displayed (last events + active users), the HTTP endpoints needed, and the AJAX update strategy.
- **Design:** Define the class structure — `AccessEvent`, `AccessEventStore`, `LoggingHttpServer`, `UdpLogReceiver` stub, `RemoteAccessLoggingServerApp`.
- **Implement:** Implement the HTTP server with two HTML pages and two JSON API endpoints. Leave `UdpLogReceiver` as a stub for US90.
- **Test:** Start the server and open both pages in a browser; verify AJAX auto-refresh via browser DevTools.

---

## 2. Requirements

**US091** As an Administrator, I want to view the logs stored at the Remote Accesses Logging Server.

**Acceptance Criteria:**

| AC | Description |
|----|-------------|
| AC091.1 | The Remote Accesses Logging Server application contains an HTTP server to provide status pages to clients running a standard web browser. |
| AC091.2 | At least two pages must be available: one presenting the list of the last recorded events, and another presenting a list of the remote users currently active. |
| AC091.3 | Both pages must be kept updated to represent the current standing. |
| AC091.4 | AJAX must be used to update the presented data without the need of reloading the page. |

**Dependencies/References:**

| Dependency | Reason |
|-----------|--------|
| US90 | US90 is responsible for receiving UDP datagrams and populating `AccessEventStore`. Without US90, the tables are empty but the pages are fully functional. |

---

## 3. Analysis

US91 requires an HTTP server embedded in the same JVM as the UDP receiver. The HTTP server serves two HTML pages and two JSON API endpoints. The HTML pages use `fetch()` + `setInterval` (AJAX) to poll the JSON endpoints every 5 seconds and update the tables without reloading.

### Architecture overview

```
[Web Browser]  ──HTTP GET /events──►  [LoggingHttpServer]  ──reads──►  [AccessEventStore]
[Web Browser]  ──HTTP GET /active──►  [LoggingHttpServer]              ◄──writes──  [UdpLogReceiver]
[Web Browser]  ──HTTP GET /api/events►[LoggingHttpServer]
[Web Browser]  ──HTTP GET /api/active►[LoggingHttpServer]
```

The `AccessEventStore` is a thread-safe singleton shared between `UdpLogReceiver` (writer, US90) and `LoggingHttpServer` (reader, US91).

### Data displayed

**Last Events page (`/events`)** — last 100 events in reverse chronological order:

| Field | Source |
|-------|--------|
| Timestamp | `AccessEvent.getTimestamp()` |
| Username | `AccessEvent.getUsername()` |
| Client IP | `AccessEvent.getClientIp()` |
| Port | `AccessEvent.getClientPort()` |
| Service | `AccessEvent.getServiceId()` (US44, US78, or US86) |
| Event | `AccessEvent.getEventType()` (LOGIN_SUCCESS, LOGOUT, etc.) |

**Active Users page (`/active`)** — users with an open session (LOGIN_SUCCESS received, no LOGOUT or CONNECTION_LOST yet):

| Field | Source |
|-------|--------|
| Username | `AccessEvent.getUsername()` |
| Client IP | `AccessEvent.getClientIp()` |
| Port | `AccessEvent.getClientPort()` |
| Service | `AccessEvent.getServiceId()` |
| Login Time | `AccessEvent.getTimestamp()` (of the LOGIN_SUCCESS event) |

### Key design decisions

**Built-in `com.sun.net.httpserver.HttpServer`** — no external Maven dependency is needed. This keeps the logging server application self-contained and easy to deploy on any cloud node with a standard JRE.

**In-memory store only** — events are not persisted to disk. The store holds up to 500 events (FIFO). This is sufficient for the prototype and avoids the complexity of a database on the logging node.

**Thread safety** — `AccessEventStore` uses `CopyOnWriteArrayList` for the event history and `ConcurrentHashMap` for the active sessions map. Both allow concurrent reads (HTTP server threads) and writes (UDP receiver thread) without explicit synchronisation.

**AJAX with `fetch()` + `setInterval(5000)`** — the HTML is served inline as Java strings; no external static files are required. The JavaScript polls `/api/events` and `/api/active` every 5 seconds and rebuilds the table body without a page reload.

### Main classes identified

| Class | Responsibility |
|-------|----------------|
| `AccessEvent` | Immutable record for one remote-access event; static `parse(String)` for pipe-delimited format |
| `AccessEventStore` | Thread-safe singleton; stores up to 500 events (FIFO); tracks active sessions |
| `LoggingHttpServer` | Embedded HTTP server on port 8080; serves HTML pages and JSON API |
| `UdpLogReceiver` | Stub — receives `AccessEventStore` in constructor; US90 fills in the UDP logic |
| `RemoteAccessLoggingServerApp` | Entry point; wires the components together and blocks |

---

## 4. Design

### 4.1. HTTP Endpoints

| Method | Path | Response | Description |
|--------|------|----------|-------------|
| GET | `/` | 302 → `/events` | Root redirect |
| GET | `/events` | HTML | Last recorded events page (AJAX) |
| GET | `/active` | HTML | Currently active users page (AJAX) |
| GET | `/api/events` | JSON array | Last 100 events, used by `/events` AJAX |
| GET | `/api/active` | JSON array | Active sessions, used by `/active` AJAX |

### 4.2. AccessEvent wire format

Pipe-delimited, produced by `RemoteAccessLogger` (US90 sender side):

```
2026-06-04 14:32:01 | joana | 127.0.0.1 | 54321 | US78 | LOGIN_SUCCESS
```

Fields: `timestamp | username | clientIp | clientPort | serviceId | eventType`

### 4.3. Class diagram

```
RemoteAccessLoggingServerApp
        │ creates
        ├──► LoggingHttpServer (port 8080)
        │           │ reads
        │           └──► AccessEventStore (singleton)
        │                       ▲
        └──► UdpLogReceiver ────┘ (writes — stub, US90)
                    uses
                AccessEvent.parse(String)
```

### 4.4. Acceptance Tests

Testing is manual (no JUnit for the HTTP/AJAX layer):

| Test | Expected |
|------|----------|
| Start `RemoteAccessLoggingServerApp`, open `http://localhost:8080/events` | Page loads, shows table with 0 events, status line updates every 5 s |
| Open `http://localhost:8080/active` | Page loads, shows table with 0 active users |
| Navigate to `/` | Browser redirects to `/events` |
| Open browser DevTools → Network tab | Requests to `/api/events` and `/api/active` appear every 5 s |
| Call `AccessEventStore.getInstance().addEvent(...)` programmatically | Tables update on next AJAX poll without page reload |

---

## 5. Implementation

All classes are in `src/main/java/aisafe/app/loggingserver/`:

| Class | Key implementation notes |
|-------|--------------------------|
| `AccessEvent` | Immutable; `parse()` splits on `\s*\|\s*`, returns `null` for malformed lines |
| `AccessEventStore` | `addEvent()` enforces MAX_EVENTS=500 FIFO; updates `activeSessions` map on LOGIN_SUCCESS / LOGOUT / CONNECTION_LOST |
| `LoggingHttpServer` | `HttpServer.create()` on port 8080; inline HTML built with Java string concatenation; JSON built with `StringBuilder` (no external library) |
| `UdpLogReceiver` | Skeleton only — constructor accepts `AccessEventStore`; TODO comment guides US90 implementer |
| `RemoteAccessLoggingServerApp` | Starts HTTP server, creates UDP receiver stub, blocks with `Thread.currentThread().join()` |

---

## 6. Integration/Demonstration

**Prerequisites:** Java 21, Maven 3.9+. Run from the `aisafe.base` directory.

**Start the server:**

```bash
java -cp "target/classes:$(mvn dependency:build-classpath -DforceStdout -q 2>/dev/null)" \
  aisafe.app.loggingserver.RemoteAccessLoggingServerApp
```

Expected output:
```
[Logging Server] HTTP running on port 8080
```

**Open in browser:**

- `http://localhost:8080/events` — last events table (empty until US90 is implemented)
- `http://localhost:8080/active` — active users table

**Verify AJAX:**

Open browser DevTools (F12) → Network tab. Requests to `/api/events` and `/api/active` appear every 5 seconds automatically.

**Stop the server:** `Ctrl+C` in the terminal.

---

## 7. Observations

- `UdpLogReceiver` is intentionally left as a stub. The class exists and accepts `AccessEventStore` in its constructor so that the integration point with US90 is clear and the main application compiles cleanly. The US90 implementer only needs to fill in the UDP receiver loop inside this class.
- The HTTP server uses `com.sun.net.httpserver` (built-in since Java 6), so no extra entry in `pom.xml` is required.
- The JSON serialisation is manual (`StringBuilder`) to avoid adding an external JSON library dependency to the logging server module.
- During the demonstration, the Remote Accesses Logging Server must run on a **different network node** from the Main Application, as required by the deployment diagram in the RCOMP project description. The HTTP server binds to all interfaces (`0.0.0.0`) by default, so it is accessible from any machine on the network.
