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
