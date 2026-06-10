# US086 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 No New Aggregates — Correct Scope

US086 introduces no new aggregates, entities, or value objects. The TCP infrastructure (`AiSafeTcpServer`, `TcpClientDispatcher`, `PilotSessionHandler`) is a **delivery mechanism** — it lives outside the domain layer. All domain concepts (`FlightPlan`, `Pilot`) are already correctly defined and mapped in previous user stories. This reflects the DDD principle that the domain model captures business concepts only, not infrastructure concerns.

---

### 1.2 Reuse of Existing Aggregates

All domain objects used by this US — `FlightPlan`, `WeatherData`, `Pilot` — are already defined and mapped in previous user stories. `PilotSessionHandler` never creates or modifies domain objects directly; it delegates entirely to existing application-layer controllers:

- `CreateFlightPlanFromFileController` (US081) — handles `CREATE_FLIGHT_PLAN`
- `InsertWeatherDataController` (US082) — handles `INSERT_WEATHER_DATA`, `LIST_MY_PLANS`, `LIST_WEATHER_DATA`
- `TestFlightPlanController` (US085) — handles `TEST_FLIGHT_PLAN`

```java
final var plan = new InsertWeatherDataController()
        .insertWeatherData(parts[1].trim(), weatherDataId);
out.println("OK " + plan.identity());
```

---

### 1.3 Repository as Interface

`FlightPlanRepository` is a domain interface — `PilotSessionHandler` never touches it directly. Persistence is always accessed through the controller, keeping the TCP layer completely decoupled from persistence.

---

### 1.4 Low Coupling between Layers

The TCP layer (`tcpserver` package) does not import any JPA class or repository. All connections to the domain layer go through the application-layer controllers (`CreateFlightPlanFromFileController`, `InsertWeatherDataController`, `TestFlightPlanController`). This enforces a strict boundary between the delivery mechanism and the domain/application layers.

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`PilotSessionHandler` is the information expert for Pilot commands — it owns the knowledge of which commands are valid and how to dispatch them:

```java
if (line.startsWith("CREATE_FLIGHT_PLAN")) {
    handleCreateFlightPlan(line);
} else if (line.startsWith("INSERT_WEATHER_DATA")) {
    handleInsertWeatherData(line);
} else if (line.startsWith("TEST_FLIGHT_PLAN")) {
    handleTestFlightPlan(line);
} else if (line.equals("LIST_MY_PLANS")) {
    handleListMyPlans();
} else if (line.equals("LIST_WEATHER_DATA")) {
    handleListWeatherData();
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

`CreateFlightPlanFromFileController` (US081), `InsertWeatherDataController` (US082), and `TestFlightPlanController` (US085) are the application-layer controllers reused server-side for each Pilot use case.

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
- `PilotSessionHandler` only knows the three application controllers — it does not know how domain objects are persisted.
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

`PilotSessionHandler` is shielded from changes to any of the three application controllers — it interacts only through their public method signatures. If a controller changes internally (e.g. different validation stages, new repository), `PilotSessionHandler` is unaffected.

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

`PilotSessionHandler` depends on application-layer controllers, which in turn depend on repository **interfaces** (`FlightPlanRepository`, `WeatherDataRepository`) — never on JPA implementations directly:

```java
private final FlightPlanRepository flightPlanRepo =
        PersistenceContext.repositories().flightPlans();
private final WeatherDataRepository weatherDataRepo =
        PersistenceContext.repositories().weatherData();
```

The correct implementations are injected at runtime by `PersistenceContext`.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`PilotTcpClient` is a Facade over raw TCP socket operations — it hides the details of stream creation, line formatting, reading and the multi-line list protocol behind simple method calls:

```java
public String insertWeatherData(final String designator, final long weatherDataId)
        throws IOException {
    out.println("INSERT_WEATHER_DATA " + designator + " " + weatherDataId);
    return in.readLine();
}

public String testFlightPlan(final String designator) throws IOException {
    out.println("TEST_FLIGHT_PLAN " + designator);
    return in.readLine();
}

public List<String> listMyPlans() throws IOException {
    out.println("LIST_MY_PLANS");
    return readListResponse();
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
| Reuse of existing aggregates | DDD | `FlightPlan`, `WeatherData`, `Pilot` reused via existing controllers |
| Repository as interface | DDD | `FlightPlanRepository`, `WeatherDataRepository` — never accessed directly from TCP layer |
| Low Coupling between layers | DDD | `tcpserver` package has no JPA imports |
| Information Expert | GRASP | `PilotSessionHandler` dispatches all Pilot commands; `TcpClientDispatcher` handles auth |
| Controller | GRASP | `TcpClientDispatcher` (TCP entry point); three use case controllers (US081/US082/US085) |
| Creator | GRASP | `TcpClientDispatcher` creates `PilotSessionHandler`; `AiSafeTcpServer` creates `TcpClientDispatcher` |
| Low Coupling | GRASP | Each layer only knows the next — server → dispatcher → handler → controller |
| High Cohesion | GRASP | Each class has a single focused responsibility |
| Protected Variations | GRASP | `PilotTcpClient` encapsulates protocol; `PilotSessionHandler` shields from controller changes |
| SRP | SOLID | Each class has one reason to change |
| OCP | SOLID | `TcpClientDispatcher` extensible for new roles without modification |
| DIP | SOLID | Controllers depend on `FlightPlanRepository` and `WeatherDataRepository` interfaces |
| Facade | GoF | `PilotTcpClient` hides raw TCP socket operations and multi-line list protocol |
| Factory Method | GoF | `PersistenceContext.repositories().flightPlans()` and `.weatherData()` |
| Template Method | GoF | EAPLI base repositories provide the algorithm skeleton |
