# Planning and Technical Documentation

This folder contains all technical documentation produced throughout the project, organised by sprint and type of artifact.

---

## Structure

```
docs/
├── README.md
├── globalArtifacts/
│   ├── domain_model.puml
│   └── ...
│   ├── us010/
│   ├── us011/
│   └── ...
```

---

## Global Artifacts

Global artifacts are documents that apply to the system as a whole and are not specific to any single user story. They are kept in the `GlobalArtifacts/` folder and updated as the project evolves.

| Artifact | File                                | Description |
|---|-------------------------------------|---|
| Domain Model | `GlobalArtifacts/domain_model.puml` | DDD domain model covering all aggregates, entities, value objects and relationships |

---

## User Story Documentation

Each user story has its own folder under the respective sprint. Each folder contains the artifacts produced during analysis, design and implementation of that user story.

The following artifacts are produced per user story, where applicable:

| Artifact | Description |
|---|---|
| `readme.md` | Overview of the user story, acceptance criteria and notes |
| `ssd.puml` | System Sequence Diagram |
| `cd.puml` | Class Diagram |
| `tests.md` | Test cases and coverage notes |

---

## References

- [GlobalArtifacts](global-artifacts)
  