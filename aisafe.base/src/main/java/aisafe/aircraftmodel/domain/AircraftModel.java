package aisafe.aircraftmodel.domain;

import aisafe.enginemodel.domain.EngineModel;
import aisafe.maker.domain.Maker;
import aisafe.enginemodel.domain.EngineType;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entity and Aggregate Root representing an aircraft model.
 * The combination of modelName and maker must be unique.
 * Must have at least one certified engine model.
 */
@Entity
public class AircraftModel implements AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String modelName;

    @ManyToOne
    private Maker maker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AircraftType aircraftType;

    private double emptyWeight;
    private double mtow;
    private double mzfw;
    private double maxFuelCapacity;
    private double serviceCeiling;
    private double cruiseSpeed;
    private double wingSpan;
    private double wingArea;
    private double dragCoefficient;
    private double liftCoefficient;
    private double maxRange;

    @ManyToMany
    private List<EngineModel> certifiedEngines = new ArrayList<>();

    private int maxCapacity;

    protected AircraftModel() {}

    /**
     * Creates an aircraft model with all required performance parameters.
     * At least one certified engine model must be provided.
     *
     * @param modelName       commercial name of the model (non-blank)
     * @param maker           manufacturer (non-null)
     * @param aircraftType    type classification (non-null)
     * @param emptyWeight     operating empty weight in kg (&gt; 0)
     * @param mtow            maximum take-off weight in kg (&gt; emptyWeight)
     * @param mzfw            maximum zero-fuel weight in kg (&gt; 0)
     * @param maxFuelCapacity maximum fuel capacity in kg (&gt; 0)
     * @param serviceCeiling  maximum operating altitude in metres (&gt; 0)
     * @param cruiseSpeed     typical cruise speed in m/s (&gt; 0)
     * @param wingSpan        wing span in metres (&gt; 0)
     * @param wingArea        wing surface area in m² (&gt; 0)
     * @param dragCoefficient aerodynamic drag coefficient (&gt; 0)
     * @param liftCoefficient aerodynamic lift coefficient (&gt; 0)
     * @param firstEngine     first certified engine model (non-null)
     * @throws IllegalArgumentException if any constraint is violated
     */
    public AircraftModel(final String modelName,
                         final Maker maker,
                         final AircraftType aircraftType,
                         final double emptyWeight,
                         final double mtow,
                         final double mzfw,
                         final double maxFuelCapacity,
                         final double serviceCeiling,
                         final double cruiseSpeed,
                         final double wingSpan,
                         final double wingArea,
                         final double dragCoefficient,
                         final double liftCoefficient,
                         final double maxRange,
                         final EngineModel firstEngine) {

        if (modelName == null || modelName.isBlank())
            throw new IllegalArgumentException("Model name cannot be null or empty.");
        if (maker == null)
            throw new IllegalArgumentException("Maker cannot be null.");
        if (aircraftType == null)
            throw new IllegalArgumentException("Aircraft type cannot be null.");
        if (firstEngine == null)
            throw new IllegalArgumentException("At least one engine model must be provided.");
        if (emptyWeight <= 0)
            throw new IllegalArgumentException("Empty weight must be positive.");
        if (mtow <= 0)
            throw new IllegalArgumentException("MTOW must be positive.");
        if (mtow < emptyWeight)
            throw new IllegalArgumentException("MTOW must be greater than or equal to empty weight.");
        if (mzfw <= 0)
            throw new IllegalArgumentException("MZFW must be positive.");
        if (mzfw > mtow)
            throw new IllegalArgumentException("MZFW cannot exceed MTOW.");
        if (maxFuelCapacity <= 0)
            throw new IllegalArgumentException("Max fuel capacity must be positive.");
        if (serviceCeiling <= 0)
            throw new IllegalArgumentException("Service ceiling must be positive.");
        if (cruiseSpeed <= 0)
            throw new IllegalArgumentException("Cruise speed must be positive.");
        if (wingSpan <= 0)
            throw new IllegalArgumentException("Wing span must be positive.");
        if (wingArea <= 0)
            throw new IllegalArgumentException("Wing area must be positive.");
        if (dragCoefficient <= 0)
            throw new IllegalArgumentException("Drag coefficient must be positive.");
        if (liftCoefficient <= 0)
            throw new IllegalArgumentException("Lift coefficient must be positive.");
        if (maxRange <= 0)
            throw new IllegalArgumentException("Max range must be positive.");

        this.modelName = modelName.trim();
        this.maker = maker;
        this.aircraftType = aircraftType;
        this.emptyWeight = emptyWeight;
        this.mtow = mtow;
        this.mzfw = mzfw;
        this.maxFuelCapacity = maxFuelCapacity;
        this.serviceCeiling = serviceCeiling;
        this.cruiseSpeed = cruiseSpeed;
        this.wingSpan = wingSpan;
        this.wingArea = wingArea;
        this.dragCoefficient = dragCoefficient;
        this.liftCoefficient = liftCoefficient;
        this.maxRange = maxRange;
        this.certifiedEngines.add(firstEngine);
    }

    /**
     * Certifies an additional engine model for this aircraft.
     * The engine type must match the type already certified.
     *
     * @param engine the engine model to add (non-null, not already certified, same type as existing)
     * @throws IllegalArgumentException if the engine is null, already certified, or of a different type
     */
    public void addEngine(final EngineModel engine) {
        if (engine == null)
            throw new IllegalArgumentException("Engine model cannot be null.");

        final boolean alreadyCertified = certifiedEngines.stream()
                .anyMatch(e -> e.name().equals(engine.name())
                        && e.makerName().equals(engine.makerName()));
        if (alreadyCertified)
            throw new IllegalArgumentException("Engine model already certified for this aircraft.");

        final EngineType expectedType = certifiedEngines.get(0).engineType();
        if (!engine.engineType().equals(expectedType))
            throw new IllegalArgumentException(
                    "Engine type " + engine.engineType() + " is not compatible. Expected " + expectedType + ".");

        certifiedEngines.add(engine);
    }

    /**
     * Removes a certified engine from this model.
     * The last remaining engine cannot be removed.
     *
     * @param engine the engine model to remove (must currently be certified)
     * @throws IllegalArgumentException if the engine is null, not certified, or is the last one
     */
    public void removeEngine(final EngineModel engine) {
        if (engine == null)
            throw new IllegalArgumentException("Engine model cannot be null.");
        if (certifiedEngines.size() <= 1)
            throw new IllegalArgumentException("Cannot remove the last certified engine from an aircraft model.");
        final boolean removed = certifiedEngines.removeIf(e ->
                e.name().equals(engine.name()) && e.makerName().equals(engine.makerName()));
        if (!removed)
            throw new IllegalArgumentException("Engine model is not certified for this aircraft.");
    }

    /** @return commercial name of this model */
    public String modelName() { return modelName; }

    /** @return manufacturer of this model */
    public Maker maker() { return maker; }

    /** @return primary purpose classification */
    public AircraftType aircraftType() { return aircraftType; }

    /** @return operating empty weight in kg */
    public double emptyWeight() { return emptyWeight; }

    /** @return maximum take-off weight in kg */
    public double mtow() { return mtow; }

    /** @return maximum zero-fuel weight in kg */
    public double mzfw() { return mzfw; }

    /** @return maximum fuel capacity in kg */
    public double maxFuelCapacity() { return maxFuelCapacity; }

    /** @return maximum operating altitude in metres */
    public double serviceCeiling() { return serviceCeiling; }

    /** @return typical cruise speed in m/s */
    public double cruiseSpeed() { return cruiseSpeed; }

    /** @return wing span in metres */
    public double wingSpan() { return wingSpan; }

    /** @return wing area in m² */
    public double wingArea() { return wingArea; }

    /** @return aerodynamic drag coefficient */
    public double dragCoefficient() { return dragCoefficient; }

    /** @return aerodynamic lift coefficient */
    public double liftCoefficient() { return liftCoefficient; }

    public double maxRange() { return maxRange; }

    /** @return unmodifiable list of certified engine models */
    public List<EngineModel> certifiedEngines() { return Collections.unmodifiableList(certifiedEngines); }

    /** @return maximum seat capacity (0 means no limit defined) */
    public int maxCapacity() { return maxCapacity; }

    /**
     * Sets the maximum passenger capacity for this model.
     *
     * @param maxCapacity non-negative number of seats (0 = no limit)
     * @return {@code this} for method chaining
     * @throws IllegalArgumentException if {@code maxCapacity} is negative
     */
    public AircraftModel withMaxCapacity(final int maxCapacity) {
        if (maxCapacity < 0)
            throw new IllegalArgumentException("Max capacity cannot be negative.");
        this.maxCapacity = maxCapacity;
        return this;
    }

    @Override
    public Long identity() { return id; }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AircraftModel)) return false;
        final AircraftModel that = (AircraftModel) o;
        return Objects.equals(modelName, that.modelName) && Objects.equals(maker, that.maker);
    }

    @Override
    public int hashCode() { return Objects.hash(modelName, maker); }

    @Override
    public String toString() {
        return String.format("AircraftModel{modelName='%s', maker='%s', type=%s}",
                modelName, maker.name(), aircraftType);
    }
}
