# US044 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 No New Aggregates — Correct Scope

US044 introduces no new aggregates, entities, or value objects. The TCP infrastructure (`AiSafeTcpServer`, `TcpClientDispatcher`, `WeatherPersonSessionHandler`) is a **delivery mechanism** — it lives outside the domain layer. All domain concepts (`WeatherData`, `AirControlArea`) are already correctly defined and mapped in previous user stories. This reflects the DDD principle that the domain model captures business concepts only, not infrastructure concerns.

---

### 1.2 Reuse of Existing Aggregates

All domain objects used by this US — `WeatherData`, `AirControlArea` — are already defined and mapped in previous user stories. `WeatherPersonSessionHandler` never creates or modifies domain objects directly; it delegates entirely to existing application-layer controllers:

- `RegisterWeatherDataController` (US041) — handles `REGISTER_WEATHER`
- `ImportBulkWeatherDataController` (US042) — handles `IMPORT_BULK`
- `ConsultWeatherDataController` (US043) — handles `CONSULT_WEATHER`, `LIST_AREAS`

```java
final WeatherData wd = new RegisterWeatherDataController()
        .registerWeatherData(areaCode, provider, format, dateTime, ...);
out.println("OK " + wd.identity());
```

---

### 1.3 Repository as Interface

`WeatherDataRepository` is a domain interface — `WeatherPersonSessionHandler` never touches it directly. Persistence is always accessed through the controller, keeping the TCP layer completely decoupled from persistence.

---

### 1.4 Low Coupling between Layers

The TCP layer (`tcpserver` package) does not import any JPA class or repository. All connections to the domain layer go through the application-layer controllers (`RegisterWeatherDataController`, `ImportBulkWeatherDataController`, `ConsultWeatherDataController`). This enforces a strict boundary between the delivery mechanism and the domain/application layers.

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`WeatherPersonSessionHandler` is the information expert for Weather Person commands — it owns the knowledge of which commands are valid and how to dispatch them:

```java
if (line.startsWith("REGISTER_WEATHER")) {
    handleRegisterWeather(line);
} else if (line.startsWith("IMPORT_BULK")) {
    handleImportBulk(line);
} else if (line.startsWith("CONSULT_WEATHER")) {
    handleConsultWeather(line);
} else if (line.equals("LIST_AREAS")) {
    handleListAreas();
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

`TcpClientDispatcher` is the entry-point controller for each TCP connection — it receives the raw request (socket), authenticates the user, checks the role and service token, and delegates to the appropriate session handler. It does not contain any business logic.

`RegisterWeatherDataController` (US041), `ImportBulkWeatherDataController` (US042), and `ConsultWeatherDataController` (US043) are the application-layer controllers reused server-side for each Weather Person use case.

---

### 2.3 Creator

`TcpClientDispatcher` creates `WeatherPersonSessionHandler` because it has all the required data — the authenticated reader and writer streams, and knowledge of the authenticated role and service token:

```java
if (AuthenticationContext.hasRole(AiSafeRoles.WEATHER_PERSON) && "WEATHER".equals(serviceToken)) {
    out.println("OK");
    new WeatherPersonSessionHandler(in, out).handle();
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
- `TcpClientDispatcher` only knows `WeatherPersonSessionHandler` — it does not know the individual Weather Person commands.
- `WeatherPersonSessionHandler` only knows the three application controllers — it does not know how domain objects are persisted.
- `WeatherPersonTcpClientApp` only knows `WeatherPersonTcpClient` — it has no dependency on any server-side class.

---

### 2.5 High Cohesion

| Class | Single Focused Responsibility |
|-------|------------------------------|
| `AiSafeTcpServer` | Accept TCP connections and spawn dispatcher threads |
| `TcpClientDispatcher` | Authenticate one client connection and dispatch to the correct session handler |
| `WeatherPersonSessionHandler` | Handle the Weather Person command loop |
| `WeatherPersonTcpClient` | Encapsulate TCP communication from the client side |
| `WeatherPersonTcpClientApp` | Present the interactive client menu to the Weather Person |

---

### 2.6 Protected Variations

`WeatherPersonSessionHandler` is shielded from changes to any of the three application controllers — it interacts only through their public method signatures. If a controller changes internally (e.g. different validation stages, new repository), `WeatherPersonSessionHandler` is unaffected.

`WeatherPersonTcpClient` encapsulates all TCP protocol details — if the protocol changes (e.g. binary format instead of text), only `WeatherPersonTcpClient` and `WeatherPersonSessionHandler` need to change. `WeatherPersonTcpClientApp` is not affected.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `AiSafeTcpServer` | Open server socket and accept connections |
| `TcpClientDispatcher` | Authenticate one connection and dispatch by role and service token |
| `WeatherPersonSessionHandler` | Handle the Weather Person command loop over streams |
| `WeatherPersonTcpClient` | Encapsulate client-side TCP communication |
| `WeatherPersonTcpClientApp` | Provide the interactive menu for the Weather Person |

---

### 3.2 Open/Closed Principle (OCP)

`TcpClientDispatcher` is open for extension — adding support for a new role requires only adding a new `else if` branch and a new session handler class. The `WEATHER` service token check already demonstrates this pattern:

```java
if (AuthenticationContext.hasRole(AiSafeRoles.PILOT)) {
    new PilotSessionHandler(in, out).handle();
} else if (AuthenticationContext.hasRole(AiSafeRoles.WEATHER_PERSON) && "WEATHER".equals(token)) {
    new WeatherPersonSessionHandler(in, out).handle();
} else {
    out.println("UNAUTHORIZED");
}
```

No existing code needed to change to add Weather Person support.

---

### 3.3 Dependency Inversion Principle (DIP)

`WeatherPersonSessionHandler` depends on application-layer controllers, which in turn depend on repository **interfaces** (`WeatherDataRepository`, `AirControlAreaRepository`) — never on JPA implementations directly:

```java
private final WeatherDataRepository weatherDataRepo =
        PersistenceContext.repositories().weatherData();
```

The correct implementations are injected at runtime by `PersistenceContext`.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`WeatherPersonTcpClient` is a Facade over raw TCP socket operations — it hides the details of stream creation, line formatting, reading and the multi-line list protocol behind simple method calls:

```java
public String registerWeather(final String areaCode, final String provider, ...) throws IOException {
    out.println("REGISTER_WEATHER " + areaCode + " " + provider + " ...");
    return in.readLine();
}

public List<String> listAreas() throws IOException {
    out.println("LIST_AREAS");
    return readListResponse();
}
```

---

### 4.2 Factory Method

`PersistenceContext.repositories().weatherData()` is a factory method — it returns the correct `WeatherDataRepository` implementation (in-memory or JPA) based on runtime configuration, without `RegisterWeatherDataController` knowing which one.

---

### 4.3 Template Method (via EAPLI)

`InMemoryWeatherDataRepository` and `JpaWeatherDataRepository` both extend EAPLI base classes (`InMemoryDomainRepository` and `JpaAutoTxRepository`) which provide the template for `save()`, `findAll()`, `ofIdentity()`. Each subclass only implements the specific query methods.

---

## 5. Summary Table

| Principle / Pattern | Category | Where in US044 |
|---------------------|----------|----------------|
| No new domain objects | DDD | TCP layer is delivery mechanism only |
| Reuse of existing aggregates | DDD | `WeatherData`, `AirControlArea` reused via existing controllers |
| Repository as interface | DDD | `WeatherDataRepository` — never accessed directly from TCP layer |
| Low Coupling between layers | DDD | `tcpserver` package has no JPA imports |
| Information Expert | GRASP | `WeatherPersonSessionHandler` dispatches all Weather Person commands; `TcpClientDispatcher` handles auth |
| Controller | GRASP | `TcpClientDispatcher` (TCP entry point); three use case controllers (US041/US042/US043) |
| Creator | GRASP | `TcpClientDispatcher` creates `WeatherPersonSessionHandler`; `AiSafeTcpServer` creates `TcpClientDispatcher` |
| Low Coupling | GRASP | Each layer only knows the next — server → dispatcher → handler → controller |
| High Cohesion | GRASP | Each class has a single focused responsibility |
| Protected Variations | GRASP | `WeatherPersonTcpClient` encapsulates protocol; `WeatherPersonSessionHandler` shields from controller changes |
| SRP | SOLID | Each class has one reason to change |
| OCP | SOLID | `TcpClientDispatcher` extensible for new roles without modification |
| DIP | SOLID | Controllers depend on `WeatherDataRepository` and `AirControlAreaRepository` interfaces |
| Facade | GoF | `WeatherPersonTcpClient` hides raw TCP socket operations and multi-line list protocol |
| Factory Method | GoF | `PersistenceContext.repositories().weatherData()` and `.airControlAreas()` |
| Template Method | GoF | EAPLI base repositories provide the algorithm skeleton |
