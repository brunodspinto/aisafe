# Shell Scripts

All scripts live in `aisafe.base/` and must be run from that directory (or they `cd` there automatically via `$(dirname "$0")`).

---

## Script Reference

| Script | Mode | Purpose |
|---|---|---|
| `start-h2.sh` | JPA | Starts H2 TCP server — prerequisite for all JPA scripts |
| `run-bootstrap.sh` | JPA | Seeds initial data — run once on a fresh database |
| `run-jpa.sh` | JPA | Runs the app with H2 persistence |
| `run-inmemory.sh` | InMemory | Runs app + bootstrap with no database required |

---

## Script Details

### `start-h2.sh`

Starts the H2 database server in TCP mode. The database file is stored under `aisafe.base/db/`.

**Prerequisites:** none
**Keep running:** yes — leave this terminal open for the lifetime of the JPA session.

```
./start-h2.sh
```

### `run-bootstrap.sh`

Seeds the initial set of users, roles, and other reference data into the H2 database. Bootstrap checks for existing records before inserting, so re-running it is safe (idempotent).

**Prerequisites:** `start-h2.sh` must be running.
**Run:** once, on a fresh database.

```
./run-bootstrap.sh
```

### `run-jpa.sh`

Starts the console application with JPA/H2 persistence. Previously saved data (from bootstrap or previous sessions) is available immediately.

**Prerequisites:** `start-h2.sh` must be running.

```
./run-jpa.sh
```

### `run-inmemory.sh`

Starts the console application with in-memory persistence. Bootstrap runs automatically at startup to seed data. No database or external server is needed. All data is lost when the process exits.

**Prerequisites:** none.

```
./run-inmemory.sh
```

---

## Workflows

### First-time JPA setup

Run each step in sequence. Keep Terminal 1 open throughout.

```
# Terminal 1 — keep running
./start-h2.sh

# Terminal 2 — seed data once
./run-bootstrap.sh

# Terminal 2 — start the app
./run-jpa.sh
```

### Subsequent JPA sessions (data already seeded)

```
# Terminal 1 — keep running
./start-h2.sh

# Terminal 2
./run-jpa.sh
```

### InMemory (no database needed)

```
./run-inmemory.sh
```
