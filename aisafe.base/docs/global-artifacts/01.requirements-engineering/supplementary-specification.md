# Supplementary Specification (FURPS+)

## Functionality

_Specifies functionalities that:  
&nbsp; &nbsp; (i) are common across several US/UC;  
&nbsp; &nbsp; (ii) are not related to US/UC, namely: Audit, Reporting and Security._

- **Authentication & Authorization:** All system users must authenticate before accessing any functionality. Role-based access control (RBAC) must be enforced across all use cases, ensuring each actor can only access operations permitted by their role (US030, NFR09).
- **Security Clearance Enforcement:** Every AISafe user must hold an active, non-expired security clearance to operate within the system. Clearances expire automatically and must be renewed periodically (section 3.1.1).
- **Reporting:** The system must support generation of simulation reports (US109, US111) and monthly statistics reports (US112). Report structure and branding must be consistent to serve as a foundation for future report types.
- **Bootstrap:** Core entities (air control areas, airports, aircraft models, engine models, air transport companies, users) must be initialisable via a bootstrap process (US031, US050, US052, US055, US056, US060, US061).
- **DSL Validation:** Flight plan files must undergo lexical, syntactic, and semantic validation before being accepted. All errors must be collected and reported in a single execution, with error type, line, and column information (US081, US083, section 3.4).

---

## Usability

_Evaluates the user interface. It has several subcategories,
among them: error prevention; interface aesthetics and design; help and
documentation; consistency and standards._

- **Error Reporting:** All invalid inputs and DSL errors must produce clear, descriptive error messages indicating the nature and location of the problem (section 3.4.4, US081).
- **Simulation Dashboard:** The flight simulation visualisation must be accessible via a standard web browser and update automatically using AJAX, without requiring page reloads (US114).

---

## Reliability

_Refers to the integrity, compliance and interoperability of the software. The requirements to be considered are: frequency and severity of failure, possibility of recovery, possibility of prediction, accuracy, average time between failures._

- **Data Integrity:** All domain invariants must be enforced at the persistence level. Operations that violate business rules — such as removing the last certified engine from an aircraft model, or deactivating a pilot with active flight plans — must be rejected with an appropriate error message.
- **Test Coverage:** Code coverage of domain and controller packages must not fall below 90% at any point in the project (NFR03). Any commit that breaks compilation or causes tests to fail is a policy violation with direct grade consequences (NFR06).
- **Graceful Simulation Termination:** Flight simulation processes must handle termination signals correctly, releasing shared resources and performing cleanup before exiting (US102).

---

## Performance

_Evaluates the performance requirements of the software, namely: response time, start-up time, recovery time, memory consumption, CPU usage, load capacity and application availability._

- **Simulation Scalability:** The simulation must support a large number of simultaneous flights by partitioning airspace into subareas, each handled by an independent flight control service running in parallel (section 3, US100).
- **Step Synchronisation:** All flight processes must submit position updates at each time step before the simulation advances, ensuring temporal consistency across the simulation (US103, US108).
- **Concurrent Simulation Threads:** The simulation parent process must run safety violation detection and report generation in dedicated, independent threads to avoid blocking simulation progression (US106).

---

## Supportability

_The supportability requirements gathers several characteristics, such as:
testability, adaptability, maintainability, compatibility,
configurability, installability, scalability and more._

- **Testability:** Unit tests must follow the AAA (Arrange-Act-Assert) convention. Domain and controller package coverage must remain at or above 90% at all times (NFR03).
- **Database by Configuration:** The persistence layer must support switching between in-memory and relational database (RDBMS) persistence via configuration alone, without code changes (NFR08).
- **Weather Source Extensibility:** The weather import system must be designed to support additional data source formats beyond CSV without structural changes to the core system (US042).
- **Deployability:** The repository must include scripts to build, deploy, and run the solution on a Unix-compatible machine, along with a README explaining the full process (NFR07).

---

## +

### Design Constraints

- The system must follow **Domain-Driven Design (DDD)** principles, with clear separation between aggregates, entities, value objects, repositories, and services (US010).
- **Scrum** must be used for project management, with weekly meetings with the Scrum Master and sprint deliverables committed to GitHub before each deadline (NFR01).
- All UML diagrams must be produced using **PlantUML** and committed in both source (`.puml`) and rendered vector (`.svg`) formats (NFR02).
- **Source Control:** All source code, documentation and related artifacts must be versioned in the team's GitHub repository. Only the `main` branch is used as the release source — feature work merges back into `main` and there are no long-lived integration branches (NFR04).
- **ANTLR4** must be used for DSL grammar definition and processing. At least one visitor and one listener must be implemented (section 3.4.4).
- **Maven** must be used as the build automation tool and **GitHub Actions** for continuous integration with nightly builds (NFR05).

### Implementation Constraints

- Primary implementation language is **Java** (NFR10).
- The flight simulation engine (US100–US110) and the flight plan test component (US085) must be implemented in **C**, using POSIX APIs: processes, pipes, signals, shared memory, pthreads, mutexes, condition variables, and semaphores.
- The DSL processing implementation language must be **Java** (section 3.4.4).
- The system must support both **in-memory** and **relational database (RDBMS)** persistence, switchable by configuration (NFR08).
- All commits must transition the system from one valid state to another. Commits that break compilation or cause tests to fail violate NFR06.

### Interface Constraints

- **TCP Client — Weather Person (US44):** A dedicated TCP client application must provide remote access to all Weather Person user stories. Direct database access from the client is prohibited.
- **TCP Client — Air Transport Company & Pilot (US78, US86):** A dedicated TCP client must provide remote access to all Air Transport Company Collaborator and Pilot user stories. Direct database access from the client is prohibited.
- **UDP Logging Server (US113):** A UDP server must receive position update datagrams from flight processes on each simulation step and store them in per-flight log files.
- **HTTP Dashboard (US114):** The Flights Logging Server must expose an HTTP status page, updated via AJAX, viewable in a standard web browser without page reloads.
- **External Weather Service:** The final prototype must retrieve live weather forecast data from the AI weather service for use in flight simulation (section 3).

### Physical Constraints

- All scripts and POSIX-dependent components assume a **Unix-compatible** environment (NFR07).

### Assessment Constraints

- **LPROG Assessment (NFR11):** the DSL deliverable is graded on the quality of its conceptual design, the clarity and correctness of the ANTLR grammar, the robustness of lexical and syntactic analysis, the completeness and rigor of semantic validation, the quality of error reporting, justified extensions beyond the Core Flight DSL, and the design of the internal representation (AST / domain model). Extensions that go beyond the minimum Core DSL in a coherent and well-structured way are positively valued.