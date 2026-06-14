# US091 — Remote Accesses Logging Visualization

## 1. Context

This US is being implemented for the first time in Sprint 3. It adds an **HTTP server** to the Remote Accesses Logging Server application so that an Administrator can view remote access logs using a standard web browser — without installing any dedicated tool.

The Remote Accesses Logging Server is a standalone application that runs on a dedicated cloud node (see the deployment diagram in the RCOMP project description). It receives remote-access events via UDP (US90, teammate's responsibility) and exposes them via HTTP (US91).

US90 and US91 share the same in-memory store (`RemoteAccessLogStore`) and the same `RemoteAccessEvent` model. US90 fills the store; US91 reads from it.

### 1.1 List of issues

- **Analysis:** Identify the data to be displayed (last events + active users), the HTTP endpoints needed, and the AJAX update strategy.
- **Design:** Define the class structure across sub-packages `model/`, `store/`, `udp/`.
- **Implement:** Implement the HTTP server with two HTML pages and two JSON API endpoints, integrated with the US90 UDP receiver.
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
| US90 | US90 is responsible for receiving UDP datagrams and populating `RemoteAccessLogStore`. Without US90, the tables are empty but the pages are fully functional. |

---

## 3. Analysis

US91 requires an HTTP server embedded in the same JVM as the UDP receiver. The HTTP server serves two HTML pages and two JSON API endpoints. The HTML pages use `fetch()` + `setInterval` (AJAX) to poll the JSON endpoints every 5 seconds and update the tables without reloading.

### Architecture overview

```
[US78/US86 clients] ──UDP 9090──► UdpLogReceiver ──► RemoteAccessLogStore
                                                               │
[Web Browser] ──HTTP 8080──► LoggingHttpServer (US91) ──reads─┘
```

The `RemoteAccessLogStore` is thread-safe and shared between `UdpLogReceiver` (writer, US90) and `LoggingHttpServer` (reader, US91).

### Data displayed

**Last Events page (`/events`)** — last 100 events in reverse chronological order:

| Field | Source |
|-------|--------|
| Timestamp | `RemoteAccessEvent.timestamp()` |
| Username | `RemoteAccessEvent.username()` |
| Client IP | `RemoteAccessEvent.clientIp()` |
| Port | `RemoteAccessEvent.clientPort()` |
| Service | `RemoteAccessEvent.service()` (US44, US78, or US86) |
| Event | `RemoteAccessEvent.event()` (LOGIN_SUCCESS, LOGOUT, etc.) |

**Active Users page (`/active`)** — users with an open session (LOGIN_SUCCESS received, no LOGOUT or CONNECTION_LOST yet):

| Field | Source |
|-------|--------|
| Username | `ActiveUser.username()` |
| Client IP | `ActiveUser.clientIp()` |
| Port | `ActiveUser.clientPort()` |
| Service | `ActiveUser.service()` |
| Login Time | `ActiveUser.since()` (timestamp of the LOGIN_SUCCESS event) |

### Key design decisions

**Built-in `com.sun.net.httpserver.HttpServer`** — no external Maven dependency is needed. This keeps the logging server application self-contained and easy to deploy on any cloud node with a standard JRE.

**Durability across restarts** — events are appended to a file (`logs.txt`) by `LogFileWriter`. On startup, `RemoteAccessLoggingServerApp` reloads the file via `LogEventParser` so previously recorded events are preserved.

**Thread safety** — `RemoteAccessLogStore` uses `ConcurrentLinkedDeque` for the event history and `ConcurrentHashMap` for the active sessions map. Both allow concurrent reads (HTTP server threads) and writes (UDP receiver thread) without explicit synchronisation.

**AJAX with `fetch()` + `setInterval(5000)`** — the HTML is served inline as Java strings; no external static files are required. The JavaScript polls `/api/events` and `/api/active` every 5 seconds and rebuilds the table body without a page reload.

### Main classes identified

| Class | Package | Responsibility |
|-------|---------|----------------|
| `RemoteAccessEvent` | `model` | Immutable record for one remote-access event |
| `ActiveUser` | `model` | Immutable record for a currently active session |
| `RemoteAccessLogStore` | `store` | Thread-safe store; tracks events and active sessions |
| `LogFileWriter` | `store` | Appends events as pipe-delimited lines to a file |
| `LogEventParser` | `udp` | Parses pipe-delimited payload into `RemoteAccessEvent` |
| `UdpLogReceiver` | `udp` | US90 — receives UDP datagrams, populates the store |
| `LoggingHttpServer` | (root) | Embedded HTTP server on port 8080; serves HTML + JSON |
| `RemoteAccessLoggingServerApp` | (root) | Entry point; wires US90 + US91 together |

---

## 4. Design

### 4.1. HTTP Endpoints

| Method | Path | Response | Description |
|--------|------|----------|-------------|
| GET | `/` | 302 → `/events` | Root redirect |
| GET | `/events` | HTML | Last recorded events page (AJAX) |
| GET | `/active` | HTML | Currently active users page (AJAX) |
| GET | `/api/events` | JSON array | Last 100 events, polled by `/events` |
| GET | `/api/active` | JSON array | Active sessions, polled by `/active` |

### 4.2. Wire format

Pipe-delimited, produced by `RemoteAccessLogger` and also used as the file persistence format:

```
2026-06-04 14:32:01 | atcc1 | 10.0.0.5 | 54321 | US78 | LOGIN_SUCCESS
```

Fields: `timestamp | username | clientIp | clientPort | service | event`

### 4.3. Class diagram

```
RemoteAccessLoggingServerApp
        │ creates
        ├──► LoggingHttpServer (port 8080)
        │           │ reads
        │           └──► RemoteAccessLogStore
        │                       ▲
        ├──► UdpLogReceiver ────┘ (writes — US90)
        │           uses LogEventParser
        └──► LogFileWriter (appends to logs.txt)
```

### 4.4. Acceptance Tests

| Test | Expected |
|------|----------|
| Start `RemoteAccessLoggingServerApp`, open `http://localhost:8080/events` | Page loads, table shown, status line updates every 5 s |
| Open `http://localhost:8080/active` | Page loads, active users table shown |
| Navigate to `/` | Browser redirects to `/events` |
| Open browser DevTools → Network tab | Requests to `/api/events` and `/api/active` appear every 5 s |
| Send a UDP datagram with LOGIN_SUCCESS | Event appears in `/events`; user appears in `/active` on next AJAX poll |
| Send a UDP datagram with LOGOUT for same user | User disappears from `/active` on next AJAX poll |

---

## 5. Implementation

All classes are under `src/main/java/aisafe/app/loggingserver/`:

| Class | Key implementation notes |
|-------|--------------------------|
| `RemoteAccessEvent` | Record with 7 fields; `sessionKey()` builds a unique key for active-session tracking; `toLogLine()` produces the pipe-delimited persistence format |
| `ActiveUser` | Record with 5 fields; built from a `RemoteAccessEvent` when LOGIN_SUCCESS is received |
| `RemoteAccessLogStore` | `ConcurrentLinkedDeque` capped at `MAX_EVENTS = 1000` (newest first via `addFirst`, oldest dropped via `pollLast`); `ConcurrentHashMap` keyed by `sessionKey()`; LOGIN_SUCCESS adds, LOGOUT/CONNECTION_LOST removes |
| `LogFileWriter` | `synchronized append()`; `BufferedWriter` opened in append mode; implements `Closeable` |
| `LogEventParser` | Splits on `\|`; bad timestamp falls back to `LocalDateTime.now()`; bad port falls back to `-1`; returns `Optional.empty()` for malformed input |
| `UdpLogReceiver` | `DatagramSocket` loop on configurable port; `volatile boolean running` + `volatile DatagramSocket socket` for clean shutdown via `stop()` |
| `LoggingHttpServer` | `HttpServer.create()` on port 8080; inline HTML with JavaScript `fetch()` + `setInterval(5000)`; JSON built with `StringBuilder` (no external library) |
| `RemoteAccessLoggingServerApp` | Parses `--port` and `--file` args; reloads events from file on startup; starts HTTP server (US91) then blocks on UDP receiver (US90); shutdown hook closes writer |

### Unit tests

| Test class | What it tests |
|------------|---------------|
| `LogEventParserTest` | Valid parse, null/blank, wrong field count, empty mandatory fields, bad timestamp fallback, bad port fallback |
| `RemoteAccessLogStoreTest` | Newest-first ordering, limit, LOGIN_SUCCESS activates, LOGOUT/CONNECTION_LOST deactivates, LOGIN_FAILED not active, clear |
| `LogFileWriterTest` | Event appended as correct pipe-delimited line |
| `UdpLogReceiverIT` | Real UDP datagram sent over loopback is received, parsed, and stored |

---

## 6. Integration/Demonstration

**Prerequisites:** Java 21, Maven 3.9+. Run from the `aisafe.base` directory.

**Start the server:**

```bash
./run-us91.sh
```

The script compiles the project and starts the server. Expected output:
```
[US91] HTTP visualization server running on port 8080
[US90] Remote Accesses Logging Server listening on UDP 9090
```

**Open in browser:**

- `http://localhost:8080/events` — last 100 events, newest first, auto-refreshed every 5 s
- `http://localhost:8080/active` — currently active users, auto-refreshed every 5 s

**Verify AJAX:** Open browser DevTools (F12) → Network tab. Requests to `/api/events` and `/api/active` appear every 5 seconds automatically.

**Stop the server:** `Ctrl+C` — shutdown hook prints the total number of events recorded.

---

## 7. Observations

- The HTTP server uses `com.sun.net.httpserver` (built-in since Java 6), so no extra entry in `pom.xml` is required.
- The JSON serialisation is manual (`StringBuilder`) to avoid adding an external JSON library dependency.
- Events are persisted to `logs.txt` (pipe-delimited, same format as the UDP wire format) so the store survives server restarts.
- During the demonstration, the Remote Accesses Logging Server must run on a **different network node** from the Main Application, as required by the deployment diagram in the RCOMP project description. The client applications (`CollaboratorTcpClientApp`, `PilotTcpClientApp`) have `LOG_HOST` hardcoded to `"localhost"` — this must be changed to the cloud node IP before the demo.
