# US072 — Tests and Coverage

## Scope

US072 covers listing the fleet of aircraft registered to the authenticated ATCC's Air Transport Company, with optional filters by model name, maker name, minimum passenger capacity, and manufacture year.

## Automated Tests

The listing and filtering logic is implemented in `ListFleetController` and operates on the aircraft collection loaded from `AircraftRepository`. No isolated domain unit tests exist for this US as it is a query operation.

### Filter behaviour (validated manually and via integration)

- **No filter**: all aircraft of the company are returned
- **US072a — by model name**: case-insensitive match on aircraft model name
- **US072b — by maker name**: case-insensitive match on aircraft maker name
- **US072c — by minimum capacity**: only aircraft with total seats ≥ given value
- **US072d — by manufacture year**: only aircraft manufactured in given year or later

## Coverage by Acceptance Criterion

- AC072.1: Controller resolves authenticated user's company via `CollaboratorRepository`, then loads fleet via `AircraftRepository`
- AC072.2 (072a): Case-insensitive model name filter applied in `ListFleetController`
- AC072.3 (072b): Case-insensitive maker name filter applied in `ListFleetController`
- AC072.4 (072c): Minimum capacity filter (`totalSeats >= minCapacity`) applied in `ListFleetController`
- AC072.5 (072d): Manufacture year filter (`yearOfManufacture >= fromYear`) applied in `ListFleetController`
- AC072.6: Controller checks `ATCC` role via `AuthorizationService`

---

### 4.2. Acceptance Tests

Authorization (ATCC role) and company resolution via `CollaboratorRepository` are infrastructure concerns validated by manual integration testing. Filter correctness is validated manually below.

**Manual test — AC072.1 (list all fleet):**

1. Run `AiSafeBackofficeApp` and login as an ATCC user.
2. Navigate to `Fleet > List Fleet`.
3. Apply no filters.
4. Expected: all aircraft registered to the authenticated ATCC's company are listed.

**Manual test — AC072.2 (filter by model name):**

1. Apply filter `Model name: 737-800`.
2. Expected: only aircraft whose model name contains `737-800` (case-insensitive) are displayed.

**Manual test — AC072.3 (filter by maker name):**

1. Apply filter `Maker name: Boeing`.
2. Expected: only aircraft manufactured by `Boeing` (case-insensitive) are displayed.

**Manual test — AC072.4 (filter by minimum capacity):**

1. Apply filter `Minimum capacity: 200`.
2. Expected: only aircraft with a total seat count greater than or equal to `200` are displayed.

**Manual test — AC072.5 (filter by manufacture year):**

1. Apply filter `From year: 2015`.
2. Expected: only aircraft manufactured in `2015` or later are displayed.

**Manual test — AC072.6 (role enforcement):**

1. Login as a user without the ATCC role (e.g. Admin or Backoffice Operator).
2. Expected: the List Fleet option is not available in the menu.
