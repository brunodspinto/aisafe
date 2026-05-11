# US070 — Tests and Coverage

## Scope

US070 covers the addition of a new aircraft to an Air Transport Company's fleet by an Air Transport Company Collaborator (ATCC). The current implementation enforces:

- Unique aircraft registration numbers through the controller and repository identity (`@Id`).
- Capacity validation to ensure the cabin configuration's total seats do not exceed the associated `AircraftModel`'s maximum capacity.
- Valid seat distribution using the `CabinConfiguration` value object.
- Safe concurrent fleet updates using Optimistic Locking (`@Version`).

---

## Automated Tests

### CabinConfigurationTest

**Location:**  
`src/test/java/aisafe/fleetmanagement/domain/CabinConfigurationTest.java`

**Coverage:**

- `ensureValidCabinConfigurationIsCreatedSuccessfully`
- `ensureCabinConfigurationCannotHaveNegativeFirstClassSeats`
- `ensureCabinConfigurationCannotHaveNegativeBusinessClassSeats`
- `ensureCabinConfigurationCannotHaveNegativeEconomyClassSeats`
- `ensureTotalSeatsIsCalculatedCorrectly`

---

### AircraftTest

**Location:**  
`src/test/java/aisafe/fleetmanagement/domain/AircraftTest.java`

**Coverage:**

- `ensureValidAircraftIsCreatedSuccessfully`
- `ensureAircraftCannotExceedModelMaximumCapacity` (Validates US070.2)
- `ensureAircraftMustHaveValidRegistrationNumber`
- `ensureAircraftRegistrationCannotBeEmpty`
- `ensureAircraftMustBeRegisteredToACountry`
- `ensureAircraftIsCreatedWithActiveOperationalStatus`

---

### AirTransportCompanyTest

**Location:**  
`src/test/java/aisafe/companyconfiguration/domain/AirTransportCompanyTest.java`

**Coverage:**

- `ensureAircraftCanBeAddedToCompanyFleet`
- `ensureDuplicateAircraftRegistrationCannotBeAddedToFleet`

---

## Coverage by Acceptance Criterion

- **US070.1:** Covered by `ensureValidAircraftIsCreatedSuccessfully` and the `CabinConfigurationTest` suite, ensuring model and seat distribution are required.
- **US070.2:** Covered explicitly by `ensureAircraftCannotExceedModelMaximumCapacity`. The domain rule is enforced inside the `Aircraft` constructor, throwing an exception if `cabin.totalSeats() > aircraftModel.maxCapacity()`.
- **US070.3:** Enforced in `AddAircraftController` and the `AircraftRepository` by the JPA `@Id` constraint on the registration number, preventing global duplicates.
- **US070.4:** Covered by `ensureAircraftMustBeRegisteredToACountry`.
- **US070.5:** Covered by `ensureAircraftIsCreatedWithActiveOperationalStatus` (default state upon instantiation).
- **US070.6:** Enforced in `AddAircraftController` by calling `authz.ensureAuthenticatedUserHasAnyOf(RoleType.ATCC)`.

---

## Manual / Integration Coverage

The remaining use-case behavior is validated through the console application:

1. Login as an Air Transport Company Collaborator (ATCC).
2. Open the Fleet Management menu:
    - `Fleet Management > Add Aircraft`
3. Select an existing `AircraftModel` from the presented list.
4. Submit a valid and unique registration number, country, and a valid seat distribution (e.g., 20 First Class, 50 Business, 100 Economy).
5. Verify that the total seats do not exceed the model's capacity.
6. Attempt to enter a seat distribution that *exceeds* the selected model's capacity and verify that the system rejects it with an appropriate error message.
7. Observe the success message and verify the persisted record in the company's fleet.

---

## Bootstrap Coverage

- Run the bootstrap application.
- Confirm that the default Air Transport Company is seeded (US060) and at least one default Aircraft is instantiated and assigned to its fleet.
- Confirm that the bootstrap process does **not duplicate** the same aircraft registration on subsequent runs.

---

## Notes

- The `CabinConfiguration` acts as an autonomous validator, ensuring no negative seat values are possible (combating Primitive Obsession).
- The capacity constraint (US070.2) is guaranteed at the domain level before any data reaches the database.
- Loose coupling is strictly tested: the `AirTransportCompany` maintains a collection of String registrations rather than full `Aircraft` entities.
  Porquê esta estrutura é perfeita para EAPLI?