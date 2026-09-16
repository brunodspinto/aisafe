# Planning and Technical Documentation

This folder contains the technical documentation produced during the project, organised by type of artifact. Diagrams are written in PlantUML (`puml/`) and exported to SVG (`svg/`) with `libs/scripts/generate-plantuml-diagrams.sh`.

---

## Structure

```
docs/
├── README.md
├── persistence.md                      # In-memory vs JPA/H2 persistence modes
├── scripts.md                          # Run scripts in aisafe.base/
├── global-artifacts/
│   ├── README.md
│   ├── 01.requirements-engineering/    # Glossary, supplementary specification, use case diagram
│   ├── 02.analysis/                    # Domain model
│   └── 03.design/                      # Design decisions
└── user-stories/
    ├── US001/ … US011/                 # Sprint 1 infrastructure and domain model
    ├── us030/ … us121/                 # Functional user stories
    └── …
```

---

## Global Artifacts

Artifacts that apply to the system as a whole, updated as the project evolves.

| Artifact | File | Description |
|---|---|---|
| Glossary | [`01.requirements-engineering/glossary.md`](global-artifacts/01.requirements-engineering/glossary.md) | Domain terms |
| Supplementary Specification | [`01.requirements-engineering/supplementary-specification.md`](global-artifacts/01.requirements-engineering/supplementary-specification.md) | Non-functional requirements |
| Use Case Diagram | [`01.requirements-engineering/use-case-diagram.md`](global-artifacts/01.requirements-engineering/use-case-diagram.md) | Actors and use cases (`puml/UCD.puml`) |
| Domain Model | [`02.analysis/analysis.md`](global-artifacts/02.analysis/analysis.md) | DDD domain model: aggregates, entities, value objects (`puml/domain-model.puml`) |
| Design | [`03.design/design.md`](global-artifacts/03.design/design.md) | Architecture, persistence and authorisation design decisions |

---

## User Story Documentation

Each user story has its own folder under `user-stories/`. Depending on the story, it contains:

| Artifact | Description |
|---|---|
| `readme.md` | Context, requirements and acceptance criteria, analysis, design, implementation and observations |
| `tests.md` | Test cases and coverage notes |
| `principles.md` | Design principles and patterns applied |
| `puml/` | PlantUML sources: domain model excerpt, sequence diagram (SD) and class diagram (names vary per story, e.g. `US044-SD.puml`) |
| `svg/` | SVG exports of the diagrams above |

---

## References

- [Global Artifacts](global-artifacts)
- [User Stories](user-stories)
- [Flight Plan DSL Specification](user-stories/us003/flight-plan-dsl-spec.md)
- [Persistence](persistence.md)
- [Scripts](scripts.md)
- [C Simulation](../simulation/README.md)
- [SCOMP Sprint 3 Summary (US105–US110)](../simulation/scomp-sprint3-summary.md): technical overview of the C simulator concurrency design
