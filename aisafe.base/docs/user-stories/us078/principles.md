# US078 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 No New Aggregates — Correct Scope

US078 introduces no new aggregates, entities, or value objects. The TCP/UDP infrastructure (`AiSafeTcpServer`, `TcpClientDispatcher`, `CollaboratorSessionHandler`, `RemoteAccessLogger`) is a **delivery mechanism** — it lives outside the domain layer. All domain concepts (`Aircraft`, `FlightRoute`, `Pilot`, `Collaborator`, `AirTransportCompany`) are already defined and mapped in previous user stories (Domain Model V8). This reflects the DDD principle that the model captures business concepts only, not infrastructure concerns.

---

### 1.2 Reuse of Existing Aggregates

The aggregates manipulated by this use case (`Aircraft`, `FlightRoute`, `Pilot`) are produced and persisted by their existing controllers. `CollaboratorSessionHandler` never manipulates an aggregate directly — it delegates to the existing application controllers, which enforce all invariants.

```java
final FlightRoute saved = new DeactivateFlightRouteController()
        .deactivateFlightRoute(new RouteName(routeName), LocalDate.parse(date));
```

---

### 1.3 Repository as Interface

The repositories (`FlightRouteRepository`, `AircraftRepository`, …) are domain interfaces — the TCP layer never touches them. Persistence is always accessed through the reused controllers, keeping the TCP layer completely decoupled from persistence.

---

### 1.4 Low Coupling between Layers

The `tcpserver` package imports no JPA class and no repository. Its only link to the application layer is through the existing controllers; its only link to authentication is through `AuthenticationContext`. This enforces a strict boundary between the delivery mechanism and the domain/application layers.

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`CollaboratorSessionHandler` is the information expert for ATCC commands — it owns the knowledge of which commands are valid and how to dispatch each one to the right controller:

```java
switch (command) {
    case "LIST_FLEET"        -> handleListFleet();
    case "LIST_ROUTES"       -> handleListRoutes();
    case "DEACTIVATE_ROUTE"  -> handleDeactivateRoute(args);
    case "CREATE_ROUTE"      -> handleCreateRoute(args);
    case "EXIT"              -> { out.println("BYE"); return; }
    default                  -> out.println("UNKNOWN_COMMAND");
}
```

`TcpClientDispatcher` is the information expert for authentication — it owns the socket and knows how to read `LOGIN` and authenticate. `RemoteAccessLogger` (client-side) is the information expert for the UDP log format — it owns the knowledge of how a remote-access event is serialized and sent.

---

### 2.2 Controller

`TcpClientDispatcher` is the entry-point controller for each TCP connection — it authenticates, checks the role and delegates to the appropriate session handler, containing no business logic. The reused `DeactivateFlightRouteController`, `ListFleetController`, `CreateFlightRouteController`, … are the application-layer controllers for each collaborator use case.

---

### 2.3 Creator

`TcpClientDispatcher` creates `CollaboratorSessionHandler` because it holds the authenticated reader/writer streams and knows the authenticated role:

```java
} else if (AuthenticationContext.hasRole(AiSafeRoles.ATCC)) {
    out.println("OK");
    new CollaboratorSessionHandler(in, out).handle();
}
```

`AiSafeTcpServer` creates `TcpClientDispatcher` because it owns the accepted socket. `CollaboratorTcpClientApp` creates and uses `RemoteAccessLogger`, since it owns the session lifecycle events that must be logged.

---

### 2.4 Low Coupling

- `AiSafeTcpServer` only knows `TcpClientDispatcher`.
- `TcpClientDispatcher` only knows the session handlers — not the individual commands.
- `CollaboratorSessionHandler` only knows the existing controllers — not how aggregates are persisted.
- `CollaboratorTcpClientApp` only knows `CollaboratorTcpClient` and the client-side `RemoteAccessLogger` — no dependency on any server-side class.

---

### 2.5 High Cohesion

| Class | Single Focused Responsibility |
|-------|------------------------------|
| `AiSafeTcpServer` | Accept TCP connections and spawn dispatcher threads |
| `TcpClientDispatcher` | Authenticate one connection and dispatch by role |
| `CollaboratorSessionHandler` | Handle the ATCC command loop |
| `RemoteAccessLogger` | Send remote-access events over UDP (client-side) |
| `CollaboratorTcpClient` | Encapsulate client-side TCP communication |
| `CollaboratorTcpClientApp` | Present the interactive client menu and emit UDP log events |

---

### 2.6 Protected Variations

`CollaboratorSessionHandler` is shielded from changes inside the reused controllers — it interacts only through their public methods. `CollaboratorTcpClient` encapsulates all TCP protocol details — if the wire format changes, only the client and the session handler change, never `CollaboratorTcpClientApp`. `RemoteAccessLogger` encapsulates the UDP payload format — if the format changes, only the logger changes.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `TcpClientDispatcher` | Authenticate one connection and dispatch by role |
| `CollaboratorSessionHandler` | Handle the ATCC command loop over streams |
| `RemoteAccessLogger` | Emit UDP remote-access events (client-side) |
| `CollaboratorTcpClient` | Encapsulate client-side TCP communication |
| `CollaboratorTcpClientApp` | Provide the interactive menu for the ATCC |

---

### 3.2 Open/Closed Principle (OCP)

`TcpClientDispatcher` is open for extension — US078 adds support for the `ATCC` role by adding **one `else if` branch and one handler class**, with no change to the existing `PILOT` logic:

```java
if (AuthenticationContext.hasRole(AiSafeRoles.PILOT)) {
    new PilotSessionHandler(in, out).handle();
} else if (AuthenticationContext.hasRole(AiSafeRoles.ATCC)) {   // US078 — added
    new CollaboratorSessionHandler(in, out).handle();
} else {
    out.println("UNAUTHORIZED");
}
```

---

### 3.3 Dependency Inversion Principle (DIP)

`CollaboratorSessionHandler` depends on the application controllers, which in turn depend on the repository **interfaces** (`FlightRouteRepository`, `AircraftRepository`, …) — never on the JPA implementations. `PersistenceContext` injects the correct implementation at runtime.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`CollaboratorTcpClient` is a Facade over raw TCP socket operations — it hides stream creation, line formatting and reading behind simple method calls:

```java
public boolean login(final String username, final String password) throws IOException {
    out.println("LOGIN " + username + " " + password);
    return "OK".equals(in.readLine());
}

public String deactivateRoute(final String routeName, final String date) throws IOException {
    out.println("DEACTIVATE_ROUTE " + routeName + " " + date);
    return in.readLine();
}
```

---

### 4.2 Factory Method

`PersistenceContext.repositories().flightRoutes()` (and the other factory methods) return the correct repository implementation (in-memory or JPA) based on runtime configuration, without the reused controllers knowing which one.

---

### 4.3 Template Method (via EAPLI)

The reused repositories (`JpaFlightRouteRepository`, `JpaAircraftRepository`, …) extend EAPLI base classes (`JpaAutoTxRepository`, `InMemoryDomainRepository`) which provide the template for `save()`, `findAll()`, `ofIdentity()`. Each subclass only supplies its specific queries.

---

## 5. Summary Table

| Principle / Pattern          | Category   | Where in US078                                                                                                                 |
|------------------------------|------------|--------------------------------------------------------------------------------------------------------------------------------|
| No new domain objects        | DDD        | TCP/UDP layer is a delivery mechanism only                                                                                     |
| Reuse of existing aggregates | DDD        | `Aircraft`, `FlightRoute`, `Pilot` via existing controllers                                                                    |
| Repository as interface      | DDD        | Repositories never accessed from the TCP layer                                                                                 |
| Low Coupling between layers  | DDD        | `tcpserver` package has no JPA imports                                                                                         |
| Information Expert           | GRASP      | `CollaboratorSessionHandler` dispatches commands; `TcpClientDispatcher` handles auth; `RemoteAccessLogger` owns the UDP format |
| Controller                   | GRASP      | `TcpClientDispatcher` (TCP entry point); reused use-case controllers                                                           |
| Creator                      | GRASP      | `TcpClientDispatcher` creates `CollaboratorSessionHandler`; client app creates `RemoteAccessLogger`                            |
| Low Coupling                 | GRASP      | server → dispatcher → handler → controller                                                                                     |
| High Cohesion                | GRASP      | Each class has a single focused responsibility                                                                                 |
| Protected Variations         | GRASP      | `CollaboratorTcpClient` encapsulates the protocol; `RemoteAccessLogger` encapsulates the UDP format                            |
| SRP                          | SOLID      | Each class has one reason to change                                                                                            |
| OCP                          | SOLID      | `TcpClientDispatcher` extended for `ATCC` without modifying `PILOT` logic                                                      |
| DIP                          | SOLID      | Controllers depend on repository interfaces                                                                                    |
| Facade                       | GoF        | `CollaboratorTcpClient` hides raw TCP socket operations                                                                        |
| Factory Method               | GoF        | `PersistenceContext.repositories().xxx()`                                                                                      |
| Template Method              | GoF        | EAPLI base repositories provide the algorithm skeleton                                                                         |