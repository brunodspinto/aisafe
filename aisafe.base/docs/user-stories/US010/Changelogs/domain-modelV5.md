## V5 — Sprint 2 (14 May 2026)

The following change was made after identifying an inconsistency between the domain model and the implementation.

---

### 1. `CabinConfiguration` multiplicity corrected: `0..*` → `0..1`

**Before:**
```
Aircraft "1" *-- "0..*" CabinConfiguration : "has"
```

**After:**
```
Aircraft "1" *-- "0..1" CabinConfiguration : "has"
```

**Reason:**
The multiplicity `0..*` was incorrect on two counts:

1. **Implies a collection** — the implementation uses a single `@Embedded` field, not a collection. An aircraft has at most one cabin configuration.
2. **Carries no semantics about CARGO** — the `AircraftType` enum already distinguishes `CARGO` from `PASSENGER`/`MIXED`. The correct cardinality is `0..1`:
   - `CARGO` aircraft: 0 (absent — null)
   - `PASSENGER` / `MIXED` aircraft: 1 (present — required)

The multiplicity `0..1` captures this optionality while ruling out the nonsensical "multiple configurations" that `0..*` would imply.

---

### 2. `Aircraft` constructor enforces cabin presence by type

| Aircraft type | `cabinConfiguration` |
|---------------|----------------------|
| `CARGO`       | must be `null`       |
| `PASSENGER`   | must be non-null     |
| `MIXED`       | must be non-null     |

The `cabinConfiguration()` getter always returns `null` for `CARGO` aircraft, abstracting the JPA persistence detail (Hibernate 6 stores the embedded columns as `0` for cargo and reconstructs the object on load; the getter hides this from callers).

`CabinConfiguration` retains the invariant "at least one seat" — this is enforced for `PASSENGER`/`MIXED` aircraft and is never invoked for `CARGO`.

---

## Summary of changes

| Change | Type |
|--------|------|
| `Aircraft *-- CabinConfiguration` multiplicity changed from `0..*` to `0..1` | Correction |
| `Aircraft` constructor validates cabin presence against `AircraftType` | Enhancement |
| `cabinConfiguration()` getter returns `null` for `CARGO` aircraft | Bug fix |
