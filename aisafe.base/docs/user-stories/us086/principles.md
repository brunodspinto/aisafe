# US086 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 No New Aggregates — Correct Scope

US086 introduces no new aggregates, entities, or value objects. The TCP infrastructure (`AiSafeTcpServer`, `TcpClientDispatcher`, `PilotSessionHandler`) is a **delivery mechanism** — it lives outside the domain layer. All domain concepts (`FlightPlan`, `Pilot`) are already correctly defined and mapped in previous user stories. This reflects the DDD principle that the domain model captures business concepts only, not infrastructure concerns.

---

### 1.2 Reuse of Existing Aggregate

The `FlightPlan` aggregate (created in US081) is the domain object produced by this use case. `PilotSessionHandler` does not create `FlightPlan` directly — it delegates to `CreateFlightPlanFromFileController`, which enforces all domain invariants and persists via `FlightPlanRepository`.

```java
final var controller = new CreateFlightPlanFromFileController();
final var flightPlan = controller.createFromFile(tempPath);
out.println("OK " + flightPlan.identity());
```

---

### 1.3 Repository as Interface

`FlightPlanRepository` is a domain interface — `PilotSessionHandler` never touches it directly. Persistence is always accessed through the controller, keeping the TCP layer completely decoupled from persistence.

---

### 1.4 Low Coupling between Layers

The TCP layer (`tcpserver` package) does not import any JPA class or repository. The only connection to the domain layer is through `CreateFlightPlanFromFileController`. This enforces a strict boundary between the delivery mechanism and the domain/application layers.

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`PilotSessionHandler` is the information expert for Pilot commands — it owns the knowledge of which commands are valid (`CREATE_FLIGHT_PLAN`, `EXIT`) and how to dispatch them:

```java
if (line.startsWith("CREATE_FLIGHT_PLAN")) {
    handleCreateFlightPlan(line);
} else if (line.equals("EXIT")) {
    out.println("BYE");
    return;
} else {
    out.println("UNKNOWN_COMMAND");
}
```

`TcpClientDispatcher` is the information expert for authentication — it owns the socket and knows how to read the `LOGIN` command and authenticate:

```java
if (!AuthenticationContext.authenticate(username, password)) {
    out.println("FAIL invalid credentials");
    return;
}
```

---

### 2.2 Controller

`TcpClientDispatcher` is the entry-point controller for each TCP connection — it receives the raw request (socket), authenticates the user, checks the role, and delegates to the appropriate session handler. It does not contain any business logic.

`CreateFlightPlanFromFileController` (reused from US081) is the application-layer controller for the flight plan creation use case.

---

### 2.3 Creator

`TcpClientDispatcher` creates `PilotSessionHandler` because it has all the required data — the authenticated reader and writer streams, and knowledge of the authenticated role:

```java
if (AuthenticationContext.hasRole(AiSafeRoles.PILOT)) {
    out.println("OK");
    new PilotSessionHandler(in, out).handle();
}
```

`AiSafeTcpServer` creates `TcpClientDispatcher` because it owns the accepted socket:

```java
final Socket clientSocket = serverSocket.accept();
new Thread(new TcpClientDispatcher(clientSocket)).start();
```

---

### 2.4 Low Coupling

- `AiSafeTcpServer` only knows `TcpClientDispatcher` — it does not know what roles exist or what commands are handled.
- `TcpClientDispatcher` only knows `PilotSessionHandler` — it does not know the individual Pilot commands.
- `PilotSessionHandler` only knows `CreateFlightPlanFromFileController` — it does not know how flight plans are persisted.
- `PilotTcpClientApp` only knows `PilotTcpClient` — it has no dependency on any server-side class.

---

### 2.5 High Cohesion

| Class | Single Focused Responsibility |
|-------|------------------------------|
| `AiSafeTcpServer` | Accept TCP connections and spawn dispatcher threads |
| `TcpClientDispatcher` | Authenticate one client connection and dispatch to the correct session handler |
| `PilotSessionHandler` | Handle the Pilot command loop |
| `PilotTcpClient` | Encapsulate TCP communication from the client side |
| `PilotTcpClientApp` | Present the interactive client menu to the Pilot |

---

### 2.6 Protected Variations

`PilotSessionHandler` is shielded from changes to `CreateFlightPlanFromFileController` — it interacts only through the `createFromFile(path)` method. If the controller changes internally (e.g. different validation stages), `PilotSessionHandler` is unaffected.

`PilotTcpClient` encapsulates all TCP protocol details — if the protocol changes (e.g. binary format instead of text), only `PilotTcpClient` and `PilotSessionHandler` need to change. `PilotTcpClientApp` is not affected.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `AiSafeTcpServer` | Open server socket and accept connections |
| `TcpClientDispatcher` | Authenticate one connection and dispatch by role |
| `PilotSessionHandler` | Handle the Pilot command loop over streams |
| `PilotTcpClient` | Encapsulate client-side TCP communication |
| `PilotTcpClientApp` | Provide the interactive menu for the Pilot |

---

### 3.2 Open/Closed Principle (OCP)

`TcpClientDispatcher` is open for extension — adding support for a new role (e.g. `ATCC` for US044) requires only adding a new `else if` branch and a new session handler class. No existing code needs to change:

```java
if (AuthenticationContext.hasRole(AiSafeRoles.PILOT)) {
    new PilotSessionHandler(in, out).handle();
} else if (AuthenticationContext.hasRole(AiSafeRoles.ATCC)) {
    new AtccSessionHandler(in, out).handle();  // future US044
} else {
    out.println("UNAUTHORIZED");
}
```

---

### 3.3 Dependency Inversion Principle (DIP)

`PilotSessionHandler` depends on `CreateFlightPlanFromFileController`, which in turn depends on the `FlightPlanRepository` **interface** — not on `JpaFlightPlanRepository` directly:

```java
private final FlightPlanRepository repository =
        PersistenceContext.repositories().flightPlans();
```

The correct implementation is injected at runtime by `PersistenceContext`.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`PilotTcpClient` is a Facade over raw TCP socket operations — it hides the details of stream creation, line formatting and reading behind simple method calls:

```java
public boolean login(final String username, final String password) throws IOException {
    out.println("LOGIN " + username + " " + password);
    return "OK".equals(in.readLine());
}

public String createFlightPlanFromFile(final String filePath) throws IOException {
    final String dslContent = Files.readString(Path.of(filePath));
    out.println("CREATE_FLIGHT_PLAN " + dslContent.getBytes().length);
    out.print(dslContent);
    out.flush();
    return in.readLine();
}
```

---

### 4.2 Factory Method

`PersistenceContext.repositories().flightPlans()` is a factory method — it returns the correct `FlightPlanRepository` implementation (in-memory or JPA) based on runtime configuration, without `CreateFlightPlanFromFileController` knowing which one.

---

### 4.3 Template Method (via EAPLI)

`InMemoryFlightPlanRepository` and `JpaFlightPlanRepository` both extend EAPLI base classes (`InMemoryDomainRepository` and `JpaAutoTxRepository`) which provide the template for `save()`, `findAll()`, `ofIdentity()`. Each subclass only implements the specific query methods.

---

## 5. Summary Table

| Principle / Pattern | Category | Where in US086 |
|---------------------|----------|----------------|
| No new domain objects | DDD | TCP layer is delivery mechanism only |
| Reuse of existing aggregate | DDD | `FlightPlan` created via `CreateFlightPlanFromFileController` |
| Repository as interface | DDD | `FlightPlanRepository` — never accessed directly from TCP layer |
| Low Coupling between layers | DDD | `tcpserver` package has no JPA imports |
| Information Expert | GRASP | `PilotSessionHandler` dispatches Pilot commands; `TcpClientDispatcher` handles auth |
| Controller | GRASP | `TcpClientDispatcher` (TCP entry point); `CreateFlightPlanFromFileController` (use case) |
| Creator | GRASP | `TcpClientDispatcher` creates `PilotSessionHandler`; `AiSafeTcpServer` creates `TcpClientDispatcher` |
| Low Coupling | GRASP | Each layer only knows the next — server → dispatcher → handler → controller |
| High Cohesion | GRASP | Each class has a single focused responsibility |
| Protected Variations | GRASP | `PilotTcpClient` encapsulates protocol; `PilotSessionHandler` shields from controller changes |
| SRP | SOLID | Each class has one reason to change |
| OCP | SOLID | `TcpClientDispatcher` extensible for new roles without modification |
| DIP | SOLID | Controller depends on `FlightPlanRepository` interface |
| Facade | GoF | `PilotTcpClient` hides raw TCP socket operations |
| Factory Method | GoF | `PersistenceContext.repositories().flightPlans()` |
| Template Method | GoF | EAPLI base repositories provide the algorithm skeleton |
