# US011 - Aggregate Justification

---
## 1. Context

It is being executed alongside the definition of the Domain Model. The purpose of this User Story is to provide explicit justification for the architectural design decisions made during the Domain-Driven Design (DDD) process, making it easier for the Project Manager and the team to review the responsibilities of each aggregate.

### 1.1 List of issues

* **Analysis:** Identify all the Aggregate Roots from the Domain Model and extract their core business rules (*invariants*).
* **Implement:** Write the markdown documentation structuring the justifications (Aggregate Catalog).
* **Design:** Design a PlantUML Sequence Diagram (SD) for a representative scenario of each aggregate, showcasing the EAPLI framework persistence and domain patterns (e.g., Builders, Factories, Repositories).


## 2. Requirements

In this section we present the functionality that is being developed and how the team understands it.

**US 011** As Project Manager, I want the team to illustrate one representative scenario per aggregate with a sequence diagram and a short explanation, so that the aggregate’s responsibility is demonstrated through an invariant it enforces.

**Acceptance Criteria:**

- The justification must make aggregate design decisions more explicit and easier to review.
- Each aggregate must have one representative scenario illustrated with a sequence diagram.
- Each aggregate must include a short explanation demonstrating its responsibility and the business rule (*invariant*) it enforces.
- The documentation must strictly follow **NFR02 (Technical Documentation)**: Sequence diagrams must be created using PlantUML (`.puml`).

**Dependencies/References:**

* Regarding this requirement, we understand that it relates directly to **US010 (Domain Model)**. The justifications and sequence diagrams formulated in this task are entirely dependent on the aggregates, entities, and value objects identified and structured in US010.


## 3. Analysis

In order to take the best design decisions for this requirement, the team analyzed the Domain Model created in US010. We identified the main Aggregate Roots (e.g., Flight, Simulation, User, Aircraft, etc.) and selected one representative scenario (User Story) for each to show how each root keeps its internal rules safe and consistent.

To ensure consistency and readability across the documentation, the team decided to adopt an Aggregate Catalog format. For each aggregate, the following structure was adopted:
1. **Aggregate Root:** Identification of the root entity.
2. **Local Entities / Value Objects / Enums:** Clear separation of the internal structural components to prove DDD and JPA mapping knowledge.
3. **Scenario:** The representative use case chosen for the Sequence Diagram.
4. **Invariant (Business Rule):** The strict business rule the root is responsible for enforcing.
5. **Justification:** The architectural reasoning.
6. **Sequence Diagram:** A link to the PlantUML diagram demonstrating the object creation and persistence flow.

**Agregate Catalog:**
* The complete list of justifications and the links to all Sequence Diagrams are maintained in the main catalog file:
   [**Aggregate Catalog**](aggregate-justification.md)