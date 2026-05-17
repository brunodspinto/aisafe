# Persistence

## Overview

The application supports two persistence modes that are selected at startup via `application.properties`.

---

## Modes

### InMemory

- Data lives only for the duration of the process.
- Nothing is written to disk; everything is lost when the JVM exits.
- Bootstrap must run on every start to seed data.
- No external server required.
- Suitable for quick local testing.

### JPA / H2

- Data is written to an H2 database file on disk.
- Data survives process restarts.
- Requires an H2 TCP server to be running before the application starts.
- Bootstrap is idempotent: if entities already exist they are skipped, so calling it again is safe.
- Suitable for development and integration testing.

---

## Selecting a Mode

The active factory is read from `aisafe.base/src/main/resources/application.properties`:

```properties
# InMemory
persistence.repositoryFactory=aisafe.infrastructure.persistence.inmemory.InMemoryRepositoryFactory

# JPA / H2
persistence.repositoryFactory=aisafe.infrastructure.persistence.jpa.JpaRepositoryFactory
```

Each shell script writes this file before starting the JVM, so you never need to edit it manually.

---

## JPA Configuration (`persistence.xml`)

`aisafe.base/src/main/resources/META-INF/persistence.xml` configures the JPA provider (Hibernate) for H2:

- **JDBC URL**: `jdbc:h2:tcp://localhost:9093/./db/aisafe` — connects to the running H2 TCP server (port 9093) and places the database in `aisafe.base/db/`.
- **`hbm2ddl.auto=update`**: Hibernate creates or updates tables automatically on startup. No SQL migration scripts are needed during development.

---

## Database File Location

```
aisafe.base/
└── db/
    └── aisafe.mv.db   ← H2 database file (created on first run)
```

The `db/` directory is ignored by git (listed in `.gitignore`) so the database file is never committed.

---

## Connecting with a GUI Client

You can inspect the database while the H2 TCP server is running.

**IntelliJ Database tool / DBeaver / any JDBC client:**

| Setting | Value |
|---|---|
| Driver | H2 |
| URL | `jdbc:h2:tcp://localhost:9093/./db/aisafe` |
| User | `sa` |
| Password | *(empty)* |

The path `./db/aisafe` is relative to the directory from which `start-h2.sh` was launched (i.e., `aisafe.base/`).
