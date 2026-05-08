package aisafe.aircraftmodel.domain;

import aisafe.enginemodel.domain.EngineModel;
import aisafe.maker.domain.Maker;
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

    @ManyToMany
    private List<EngineModel> certifiedEngines = new ArrayList<>();

    protected AircraftModel() {}

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
        this.certifiedEngines.add(firstEngine);
    }

    public void addEngine(final EngineModel engine) {
        if (engine == null)
            throw new IllegalArgumentException("Engine model cannot be null.");
        if (certifiedEngines.contains(engine))
            throw new IllegalArgumentException("Engine model already certified for this aircraft.");
        certifiedEngines.add(engine);
    }

    public String modelName() { return modelName; }
    public Maker maker() { return maker; }
    public AircraftType aircraftType() { return aircraftType; }
    public double emptyWeight() { return emptyWeight; }
    public double mtow() { return mtow; }
    public double mzfw() { return mzfw; }
    public double maxFuelCapacity() { return maxFuelCapacity; }
    public double serviceCeiling() { return serviceCeiling; }
    public double cruiseSpeed() { return cruiseSpeed; }
    public double wingSpan() { return wingSpan; }
    public double wingArea() { return wingArea; }
    public double dragCoefficient() { return dragCoefficient; }
    public double liftCoefficient() { return liftCoefficient; }
    public List<EngineModel> certifiedEngines() { return Collections.unmodifiableList(certifiedEngines); }

    @Override
    public Long identity() { return id; }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AircraftModel that = (AircraftModel) o;
        return Objects.equals(modelName, that.modelName)
                && Objects.equals(maker, that.maker);
    }

    @Override
    public int hashCode() { return Objects.hash(modelName, maker); }

    @Override
    public String toString() {
        return String.format("AircraftModel{modelName='%s', maker='%s', type=%s}",
                modelName, maker.name(), aircraftType);
    }
}
