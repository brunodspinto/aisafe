## V4 — Sprint 2 (9 May 2026)

The following changes were made to the domain model during Sprint 2 implementation of US061.

---

### 1. `Collaborator` aggregate added

**Before:**
```
AirTransportCompany "1" --> "0..*" User : "employs"
```

**After:**
```
package "Collaborator Aggregate" {
    class Collaborator <<entity, aggregate root>> {
    }
}

Collaborator "0..*" --> "0..1" AirTransportCompany : "works for"
Collaborator "0..*" --> "0..1" AirControlArea : "works for"
Collaborator "1" --> "1" User : "is"
```

**Reason:**
The original model had a direct relation between `AirTransportCompany` and `User` (`employs`). However, US061 requires that a collaborator can belong to either an `AirTransportCompany` or an `AirControlArea`, and has its own lifecycle (can be listed, edited and disabled in US062, US063 and US064).

A separate `Collaborator` aggregate was created to:
- Model the business relationship between a user and a customer explicitly
- Support the two types of customers (`AirTransportCompany` and `AirControlArea`) without modifying the `User` aggregate
- Allow the collaborator lifecycle to be managed independently

The relation `AirTransportCompany --> User : "employs"` was removed because it is now captured by `Collaborator --> AirTransportCompany` and `Collaborator --> User`.

The client confirmed that a Flight Control Operator (FCO) is a collaborator of an `AirControlArea` — not a generic flight control entity. This justifies the addition of the `Collaborator --> AirControlArea` relation.

---

## Summary of changes

| Change | Type |
|--------|------|
| `Collaborator` aggregate added | Enhancement |
| `AirTransportCompany --> User : "employs"` removed | Correction |
| `Collaborator --> AirTransportCompany : "works for"` added | Enhancement |
| `Collaborator --> AirControlArea : "works for"` added | Enhancement |
| `Collaborator --> User : "is"` added | Enhancement |
