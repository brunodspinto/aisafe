# US057 — Tests and Coverage

## Scope

US057 covers adding (and removing) engine models to/from an aircraft model's list of certified engines, including engine type compatibility and duplicate prevention.

## Automated Tests

### `AircraftModelTest`

Location: `src/test/java/aisafe/aircraftmodel/domain/AircraftModelTest.java`

> Note: tests use helper methods `validMaker()`, `validEngine()`, and `validAircraftModel()` defined in the same class.

**Test:** `ensureValidAircraftModelCanBeCreated`

```java
@Test
void ensureValidAircraftModelCanBeCreated() {
    final AircraftModel model = validAircraftModel();
    assertEquals("737-800", model.modelName());
    assertEquals("Boeing", model.maker().name());
    assertEquals(AircraftType.PASSENGER, model.aircraftType());
    assertEquals(1, model.certifiedEngines().size());
}
```

**Test:** `ensureModelNameCannotBeNull`

```java
@Test
void ensureModelNameCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel(null, validMaker(), AircraftType.PASSENGER,
                    41140, 79016, 62732, 20894,
                    12500, 230, 34.3, 125.0,
                    0.026, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureMakerCannotBeNull`

```java
@Test
void ensureMakerCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", null, AircraftType.PASSENGER,
                    41140, 79016, 62732, 20894,
                    12500, 230, 34.3, 125.0,
                    0.026, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureAircraftTypeCannotBeNull`

```java
@Test
void ensureAircraftTypeCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), null,
                    41140, 79016, 62732, 20894,
                    12500, 230, 34.3, 125.0,
                    0.026, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureFirstEngineCannotBeNull`

```java
@Test
void ensureFirstEngineCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    41140, 79016, 62732, 20894,
                    12500, 230, 34.3, 125.0,
                    0.026, 1.5, 5765.0, null));
}
```

**Test:** `ensureEmptyWeightMustBePositive`

```java
@Test
void ensureEmptyWeightMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    0, 79016, 62732, 20894,
                    12500, 230, 34.3, 125.0,
                    0.026, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureMTOWMustBeGreaterThanEmptyWeight`

```java
@Test
void ensureMTOWMustBeGreaterThanEmptyWeight() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    79016, 41140, 62732, 20894,
                    12500, 230, 34.3, 125.0,
                    0.026, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureMZFWMustBePositive`

```java
@Test
void ensureMZFWMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    41140, 79016, 0, 20894,
                    12500, 230, 34.3, 125.0,
                    0.026, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureMaxFuelCapacityMustBePositive`

```java
@Test
void ensureMaxFuelCapacityMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    41140, 79016, 62732, 0,
                    12500, 230, 34.3, 125.0,
                    0.026, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureServiceCeilingMustBePositive`

```java
@Test
void ensureServiceCeilingMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    41140, 79016, 62732, 20894,
                    0, 230, 34.3, 125.0,
                    0.026, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureCruiseSpeedMustBePositive`

```java
@Test
void ensureCruiseSpeedMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    41140, 79016, 62732, 20894,
                    12500, 0, 34.3, 125.0,
                    0.026, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureWingSpanMustBePositive`

```java
@Test
void ensureWingSpanMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    41140, 79016, 62732, 20894,
                    12500, 230, 0, 125.0,
                    0.026, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureWingAreaMustBePositive`

```java
@Test
void ensureWingAreaMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    41140, 79016, 62732, 20894,
                    12500, 230, 34.3, 0,
                    0.026, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureDragCoefficientMustBePositive`

```java
@Test
void ensureDragCoefficientMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    41140, 79016, 62732, 20894,
                    12500, 230, 34.3, 125.0,
                    0, 1.5, 5765.0, validEngine()));
}
```

**Test:** `ensureLiftCoefficientMustBePositive`

```java
@Test
void ensureLiftCoefficientMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    41140, 79016, 62732, 20894,
                    12500, 230, 34.3, 125.0,
                    0.026, 0, 5765.0, validEngine()));
}
```

**Test:** `ensureMaxRangeMustBePositive`

```java
@Test
void ensureMaxRangeMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
            new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                    41140, 79016, 62732, 20894,
                    12500, 230, 34.3, 125.0,
                    0.026, 1.5, 0, validEngine()));
}
```

**Test:** `ensureMaxRangeGetterWorks`

```java
@Test
void ensureMaxRangeGetterWorks() {
    final AircraftModel model = validAircraftModel();
    assertEquals(5765.0, model.maxRange());
}
```

**Test:** `ensureCannotAddNullEngineModel`

```java
@Test
void ensureCannotAddNullEngineModel() {
    final AircraftModel model = validAircraftModel();
    assertThrows(IllegalArgumentException.class, () -> model.addEngine(null));
}
```

**Test:** `ensureCannotAddIncompatibleEngineType`

```java
@Test
void ensureCannotAddIncompatibleEngineType() {
    final AircraftModel model = validAircraftModel();
    final EngineModel turboprop = new EngineModel("PT6A", "Pratt & Whitney Canada",
            EngineType.TURBOPROP, 17.0, 14.0, 0.29);
    assertThrows(IllegalArgumentException.class, () -> model.addEngine(turboprop));
}
```

**Test:** `ensureCanAddCompatibleEngineType`

```java
@Test
void ensureCanAddCompatibleEngineType() {
    final AircraftModel model = validAircraftModel();
    final EngineModel anotherTurbofan = new EngineModel("GE90", "GE Aviation",
            EngineType.TURBOFAN, 330.0, 310.0, 0.31);
    model.addEngine(anotherTurbofan);
    assertEquals(2, model.certifiedEngines().size());
}
```

**Test:** `ensureCannotAddDuplicateEngineModel`

```java
@Test
void ensureCannotAddDuplicateEngineModel() {
    final AircraftModel model = validAircraftModel();
    final EngineModel anotherTurbofan = new EngineModel("GE90", "GE Aviation", EngineType.TURBOFAN, 330.0, 310.0, 0.31);
    model.addEngine(anotherTurbofan);
    assertThrows(IllegalArgumentException.class, () -> model.addEngine(anotherTurbofan));
}
```

**Test:** `ensureCertifiedEnginesIsUnmodifiable`

```java
@Test
void ensureCertifiedEnginesIsUnmodifiable() {
    final AircraftModel model = validAircraftModel();
    assertThrows(UnsupportedOperationException.class,
            () -> model.certifiedEngines().add(validEngine()));
}
```

**Test:** `ensureCanRemoveEngineWhenMoreThanOneExists`

```java
@Test
void ensureCanRemoveEngineWhenMoreThanOneExists() {
    final AircraftModel model = validAircraftModel();
    final EngineModel second = new EngineModel("GE90", "GE Aviation", EngineType.TURBOFAN, 330.0, 310.0, 0.31);
    model.addEngine(second);
    model.removeEngine(second);
    assertEquals(1, model.certifiedEngines().size());
}
```

**Test:** `ensureCannotRemoveLastEngine`

```java
@Test
void ensureCannotRemoveLastEngine() {
    final AircraftModel model = validAircraftModel();
    assertThrows(IllegalArgumentException.class, () -> model.removeEngine(validEngine()));
}
```

**Test:** `ensureCannotRemoveNullEngine`

```java
@Test
void ensureCannotRemoveNullEngine() {
    final AircraftModel model = validAircraftModel();
    final EngineModel second = new EngineModel("GE90", "GE Aviation", EngineType.TURBOFAN, 330.0, 310.0, 0.31);
    model.addEngine(second);
    assertThrows(IllegalArgumentException.class, () -> model.removeEngine(null));
}
```

**Test:** `ensureCannotRemoveEngineThatIsNotCertified`

```java
@Test
void ensureCannotRemoveEngineThatIsNotCertified() {
    final AircraftModel model = validAircraftModel();
    final EngineModel second = new EngineModel("GE90", "GE Aviation", EngineType.TURBOFAN, 330.0, 310.0, 0.31);
    model.addEngine(second);
    final EngineModel notCertified = new EngineModel("V2500", "IAE", EngineType.TURBOFAN, 111.0, 105.0, 0.33);
    assertThrows(IllegalArgumentException.class, () -> model.removeEngine(notCertified));
}
```

**Test:** `ensureTwoModelsWithSameNameAndMakerAreEqual`

```java
@Test
void ensureTwoModelsWithSameNameAndMakerAreEqual() {
    final AircraftModel a = validAircraftModel();
    final AircraftModel b = new AircraftModel(
            "737-800", validMaker(), AircraftType.CARGO,
            41140, 79016, 62732, 20894,
            12500, 230, 34.3, 125.0,
            0.026, 1.5, 5765.0, validEngine()
    );
    assertEquals(a, b);
}
```

**Test:** `ensureTwoModelsWithDifferentNamesAreNotEqual`

```java
@Test
void ensureTwoModelsWithDifferentNamesAreNotEqual() {
    final AircraftModel a = validAircraftModel();
    final AircraftModel b = new AircraftModel(
            "737-900", validMaker(), AircraftType.PASSENGER,
            41140, 79016, 62732, 20894,
            12500, 230, 34.3, 125.0,
            0.026, 1.5, 5765.0, validEngine()
    );
    assertNotEquals(a, b);
}
```

**Test:** `ensureGettersReturnCorrectValues`

```java
@Test
void ensureGettersReturnCorrectValues() {
    final AircraftModel model = validAircraftModel();
    assertEquals(41140, model.emptyWeight());
    assertEquals(79016, model.mtow());
    assertEquals(62732, model.mzfw());
    assertEquals(20894, model.maxFuelCapacity());
    assertEquals(12500, model.serviceCeiling());
    assertEquals(230, model.cruiseSpeed());
    assertEquals(34.3, model.wingSpan());
    assertEquals(125.0, model.wingArea());
    assertEquals(0.026, model.dragCoefficient());
    assertEquals(1.5, model.liftCoefficient());
}
```

**Test:** `ensureToStringContainsModelName`

```java
@Test
void ensureToStringContainsModelName() {
    final AircraftModel model = validAircraftModel();
    assertTrue(model.toString().contains("737-800"));
}
```

**Test:** `ensureEqualsReturnsTrueForSameInstance`

```java
@Test
void ensureEqualsReturnsTrueForSameInstance() {
    final AircraftModel model = validAircraftModel();
    assertEquals(model, model);
}
```

**Test:** `ensureEqualsReturnsFalseForNull`

```java
@Test
void ensureEqualsReturnsFalseForNull() {
    assertNotEquals(null, validAircraftModel());
}
```

**Test:** `ensureHashCodeIsConsistentWithEquals`

```java
@Test
void ensureHashCodeIsConsistentWithEquals() {
    final AircraftModel a = validAircraftModel();
    final AircraftModel b = validAircraftModel();
    assertEquals(a.hashCode(), b.hashCode());
}
```

**Test:** `ensureSameAsReturnsTrueForEqualModels`

```java
@Test
void ensureSameAsReturnsTrueForEqualModels() {
    final AircraftModel a = validAircraftModel();
    final AircraftModel b = validAircraftModel();
    assertTrue(a.sameAs(b));
}
```

---

## Coverage by Acceptance Criterion

- AC057.1: `ensureCanAddCompatibleEngineType`, `ensureCannotAddIncompatibleEngineType`
- AC057.2: `ensureCannotAddDuplicateEngineModel`, `ensureCannotAddNullEngineModel`, `ensureCertifiedEnginesIsUnmodifiable`
- AC057.3: Optimistic Locking enforced by `@Version` on `AircraftModel`; validated by manual integration test
- AC057.4: Controller checks `BACKOFFICE_OPERATOR` role via `AuthorizationService`; validated by manual test
- Engine removal domain invariants: `ensureCanRemoveEngineWhenMoreThanOneExists`, `ensureCannotRemoveLastEngine`, `ensureCannotRemoveNullEngine`, `ensureCannotRemoveEngineThatIsNotCertified`
- Model construction invariants: `ensureValidAircraftModelCanBeCreated`, `ensureModelNameCannotBeNull`, `ensureMakerCannotBeNull`, `ensureAircraftTypeCannotBeNull`, `ensureFirstEngineCannotBeNull`, `ensureEmptyWeightMustBePositive`, `ensureMTOWMustBeGreaterThanEmptyWeight`, `ensureMZFWMustBePositive`, `ensureMaxFuelCapacityMustBePositive`, `ensureServiceCeilingMustBePositive`, `ensureCruiseSpeedMustBePositive`, `ensureWingSpanMustBePositive`, `ensureWingAreaMustBePositive`, `ensureDragCoefficientMustBePositive`, `ensureLiftCoefficientMustBePositive`, `ensureMaxRangeMustBePositive`, `ensureMaxRangeGetterWorks`, `ensureGettersReturnCorrectValues`, `ensureTwoModelsWithSameNameAndMakerAreEqual`, `ensureTwoModelsWithDifferentNamesAreNotEqual`, `ensureToStringContainsModelName`, `ensureEqualsReturnsTrueForSameInstance`, `ensureEqualsReturnsFalseForNull`, `ensureHashCodeIsConsistentWithEquals`, `ensureSameAsReturnsTrueForEqualModels`

---

## Acceptance Tests

Authorization (`BACKOFFICE_OPERATOR` role), duplicate prevention at persistence level, and Optimistic Locking are infrastructure concerns validated by manual integration testing. Engine compatibility and duplicate detection in the domain are fully covered by the automated unit tests above.

**Manual test — AC057.1 / AC057.2 (full add engine flow):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Aircraft Configuration > Add Engine to Aircraft Model`.
3. Select an existing Aircraft Model (e.g. `737-800`).
4. Select a compatible Engine Model (e.g. `CFM56`).
5. Expected: confirmation message `Engine 'CFM56' successfully added to aircraft model '737-800'.`

**Manual test — AC057.1 (incompatible engine type rejected):**

1. Select a TURBOFAN-based aircraft model and attempt to add a TURBOPROP engine.
2. Expected: the system rejects the operation with a compatibility error.

**Manual test — AC057.2 (duplicate engine rejected):**

1. Attempt to add the same engine model a second time to the same aircraft model.
2. Expected: the system rejects the operation with a duplicate error.

**Manual test — AC057.3 (Optimistic Locking):**

1. Open two terminal sessions both logged in as Backoffice Operator.
2. In Terminal 1, start adding an engine to a model but do not confirm yet.
3. In Terminal 2, add a different engine to the same model and confirm.
4. Return to Terminal 1 and confirm.
5. Expected: Terminal 1 receives a concurrency error indicating the model was already modified.

**Manual test — AC057.4 (role enforcement):**

1. Login as a user without the `BACKOFFICE_OPERATOR` role.
2. Expected: the Add Engine option is not available in the menu.

**Manual test — remove engine (full removal flow):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Aircraft Configuration > Remove Engine from Aircraft Model`.
3. Select an aircraft model that has more than one certified engine (e.g. `737-800` with `CFM56` and `GE90`).
4. Select the engine to remove (e.g. `GE90`).
5. Expected: confirmation message displayed — `Engine 'GE90' successfully removed from aircraft model '737-800'.` The model still has at least one certified engine remaining.

**Manual test — remove last engine rejected:**

1. Select an aircraft model that has exactly one certified engine.
2. Attempt to remove that engine.
3. Expected: the system rejects the operation with a message indicating the model must retain at least one certified engine.
