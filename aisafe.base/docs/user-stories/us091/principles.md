# US091 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 No Aggregates — Correct Scope

US091 is a standalone infrastructure application (the Remote Accesses Logging Server) — it has no domain model in the DDD sense. `RemoteAccessEvent` and `ActiveUser` are **value objects** (Java records, immutable) that represent data read from the network. There are no aggregates, no repositories, and no business invariants beyond the invariants of the event fields themselves.

This is correct: the logging server is a cross-cutting infrastructure concern, not part of the AISafe domain.

---

### 1.2 Value Objects — Immutable Records

`RemoteAccessEvent` and `ActiveUser` are Java `record` classes — immutable by construction. Once created, their fields cannot be changed. This is the correct DDD treatment for data that flows through the system without changing state.

```java
public record RemoteAccessEvent(
        LocalDateTime timestamp, String username, String clientIp,
        int clientPort, String service, String event, String sourceUdpIp) { ... }

public record ActiveUser(
        String username, String clientIp, int clientPort,
        String service, LocalDateTime since) { }
```

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`RemoteAccessLogStore` is the information expert for the state of the logging server — it is the only class that knows which events are recorded and which users are currently active. Both `UdpLogReceiver` (writer) and `LoggingHttpServer` (reader) go through `RemoteAccessLogStore`:

```java
// UdpLogReceiver (writer):
store.add(event);

// LoggingHttpServer (reader):
store.recent(100)
store.activeUsers()
```

`LogEventParser` is the information expert for parsing the UDP wire format — all field extraction logic is isolated inside it.

---

### 2.2 Controller

`RemoteAccessLoggingServerApp` is the entry-point controller — it wires all components together (store, file writer, UDP receiver, HTTP server) and starts them in the correct order. It does not contain any business logic.

---

### 2.3 Creator

`RemoteAccessLoggingServerApp` creates all components because it has the configuration data (port numbers, file path) needed to construct them:

```java
final var store  = new RemoteAccessLogStore();
final var writer = new LogFileWriter(filePath);
final var http   = new LoggingHttpServer(store, 8080);
final var udp    = new UdpLogReceiver(store, writer, 9090);
```

`LogEventParser` creates `RemoteAccessEvent` instances because it owns the parsing logic that produces them.

---

### 2.4 Low Coupling

- `LoggingHttpServer` only knows `RemoteAccessLogStore` — it has no knowledge of UDP, file I/O, or parsing.
- `UdpLogReceiver` only knows `RemoteAccessLogStore`, `LogFileWriter`, and `LogEventParser` — it has no knowledge of HTTP.
- `LogEventParser` has no dependencies on any other class in the project.
- `RemoteAccessEvent` and `ActiveUser` have no dependencies beyond the JDK.

---

### 2.5 High Cohesion

| Class | Single Focused Responsibility |
|-------|------------------------------|
| `RemoteAccessLoggingServerApp` | Wire and start all components |
| `LoggingHttpServer` | Serve HTML pages and JSON API endpoints via HTTP |
| `UdpLogReceiver` | Receive UDP datagrams and feed the store (US090) |
| `LogEventParser` | Parse one pipe-delimited payload into a `RemoteAccessEvent` |
| `RemoteAccessLogStore` | Thread-safe shared state between writer and readers |
| `LogFileWriter` | Append events to a file for persistence across restarts |
| `RemoteAccessEvent` | Represent one immutable remote-access event |
| `ActiveUser` | Represent one immutable active session |

---

### 2.6 Protected Variations

`RemoteAccessLogStore` shields both `UdpLogReceiver` and `LoggingHttpServer` from each other — neither knows the other exists. If the HTTP layer changes (e.g. a different web framework), `UdpLogReceiver` is unaffected. If the UDP layer changes (e.g. a different protocol), `LoggingHttpServer` is unaffected.

`LogEventParser` shields `UdpLogReceiver` from changes to the wire format — if the payload format changes, only `LogEventParser` needs to change.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

Each class in the logging server has exactly one reason to change:

| Class | One reason to change |
|-------|---------------------|
| `LoggingHttpServer` | HTTP presentation layer changes |
| `UdpLogReceiver` | UDP receive loop changes |
| `LogEventParser` | Wire format changes |
| `RemoteAccessLogStore` | Store semantics change |
| `LogFileWriter` | File persistence format changes |

---

### 3.2 Open/Closed Principle (OCP)

`RemoteAccessLogStore.add()` uses a `switch` on the event type. Adding a new event type (e.g. `SESSION_TIMEOUT`) requires only adding a new case — existing cases are unchanged:

```java
switch (event.event()) {
    case "LOGIN_SUCCESS"                 -> activeSessions.put(...);
    case "LOGOUT", "CONNECTION_LOST"     -> activeSessions.remove(...);
    default -> { /* unrecognised — recorded but no session state change */ }
}
```

---

### 3.3 Dependency Inversion Principle (DIP)

`UdpLogReceiver` and `LoggingHttpServer` both depend on `RemoteAccessLogStore` — a concrete class, but one that could be extracted to an interface if needed. `LogEventParser` is a pure static utility with no dependencies, making it easy to test and swap.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`RemoteAccessLogStore` is a Facade over the two underlying concurrent data structures (`ConcurrentLinkedDeque` for history, `ConcurrentHashMap` for active sessions). Callers see only `add()`, `recent()`, `activeUsers()` — they never interact with the deque or map directly:

```java
public void add(final RemoteAccessEvent event) {
    events.addFirst(event);
    switch (event.event()) {
        case "LOGIN_SUCCESS" -> activeSessions.put(event.sessionKey(), event);
        case "LOGOUT", "CONNECTION_LOST" -> activeSessions.remove(event.sessionKey());
        default -> {}
    }
}
```

---

### 4.2 Observer (partial — AJAX polling)

The browser pages implement a polling variant of Observer: `setInterval(5000)` triggers a `fetch()` to `/api/events` and `/api/active` every 5 seconds. The store is the subject; the browser tables are the observers. The update is pull-based (AJAX poll) rather than push-based (WebSocket), which satisfies AC091.4 without requiring a persistent connection.

---

### 4.3 Template Method (via `run()`)

`UdpLogReceiver.run()` defines the overall receive loop skeleton:

```
receive datagram → parse payload → add to store → append to file → repeat
```

Each step is well-defined and isolated. A subclass could override individual steps without changing the loop structure.

---

## 5. Thread Safety

`RemoteAccessLogStore` is the shared mutable state between two concurrent threads (UDP receiver writes, HTTP server reads). Thread safety is achieved without explicit synchronisation by using:

- `ConcurrentLinkedDeque` — lock-free, allows concurrent `addFirst()` and iteration
- `ConcurrentHashMap` — lock-free, allows concurrent `put()`, `remove()`, and `values()` iteration

This is an application of the **immutable value + concurrent collection** pattern: because `RemoteAccessEvent` is immutable (a Java record), there is no risk of a partially-constructed object being read by the HTTP thread.

---

## 6. Summary Table

| Principle / Pattern | Category | Where in US091 |
|---------------------|----------|----------------|
| Immutable value objects | DDD | `RemoteAccessEvent` and `ActiveUser` are Java records |
| No aggregates in infrastructure | DDD | Logging server is not part of the AISafe domain |
| Information Expert | GRASP | `RemoteAccessLogStore` owns all store state; `LogEventParser` owns parsing |
| Controller | GRASP | `RemoteAccessLoggingServerApp` wires and starts all components |
| Creator | GRASP | App creates all components; `LogEventParser` creates `RemoteAccessEvent` |
| Low Coupling | GRASP | HTTP and UDP layers are decoupled via `RemoteAccessLogStore` |
| High Cohesion | GRASP | Each class has one focused responsibility |
| Protected Variations | GRASP | `RemoteAccessLogStore` shields HTTP from UDP; `LogEventParser` shields receiver from format |
| SRP | SOLID | Each class has exactly one reason to change |
| OCP | SOLID | `store.add()` switch is open for new event types |
| DIP | SOLID | Both consumers depend on `RemoteAccessLogStore` — not on each other |
| Facade | GoF | `RemoteAccessLogStore` hides `ConcurrentLinkedDeque` + `ConcurrentHashMap` |
| Observer (polling) | GoF | AJAX `setInterval` polls JSON API — AC091.4 |
| Template Method | GoF | `UdpLogReceiver.run()` defines the receive loop skeleton |
| Immutable + concurrent collection | Thread safety | `RemoteAccessEvent` record + `ConcurrentLinkedDeque` / `ConcurrentHashMap` |
