package aisafe.enginemodel.domain;

import aisafe.maker.domain.MakerName;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * Entity and Aggregate Root representing an aircraft engine model.
 *
 * <p>The maker is referenced by name (external reference) rather than
 * an embedded entity, to maintain low coupling with the Maker aggregate.</p>
 */
@Entity
@Table(name = "T_ENGINE_MODEL",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "maker_name"})
        })
public class EngineModel implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "maker_name", nullable = false))
    private MakerName makerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EngineType engineType;

    @Column(nullable = false)
    private double thrustAtStandstill;
    private double thrustAtCruiseSpeed;

    @Column(nullable = false)
    private double tsfc;

    /**
     * Protected constructor required by JPA (ORM).
     */
    protected EngineModel() {
        // for ORM only
    }

    /**
     * Creates a valid EngineModel with all required fields.
     *
     * @param name       the model name (non-blank)
     * @param makerName  the manufacturer name (non-null)
     * @param engineType the engine type (non-null)
     * @param thrustAtStandstill  the thrust at standstill in kN (must be &gt; 0)
     * @param thrustAtCruiseSpeed the thrust at cruise speed in kN (must be &gt; 0)
     * @param tsfc       the thrust-specific fuel consumption (must be &gt; 0)
     */
    public EngineModel(final String name, final MakerName makerName, final EngineType engineType,
                       final double thrustAtStandstill, final double thrustAtCruiseSpeed, final double tsfc) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Engine model name cannot be null or blank.");
        }
        if (makerName == null) {
            throw new IllegalArgumentException("Maker name cannot be null.");
        }
        if (engineType == null) {
            throw new IllegalArgumentException("Engine type cannot be null.");
        }
        if (thrustAtStandstill <= 0){
            throw new IllegalArgumentException("Thrust at standstill must be greater than zero.");
        }
        if (thrustAtCruiseSpeed <= 0) {
            throw new IllegalArgumentException("Thrust at cruise speed must be greater than zero.");
        }
        if (tsfc <= 0) {
            throw new IllegalArgumentException("TSFC must be greater than zero.");
        }
        this.name = name.trim();
        this.makerName = makerName;
        this.engineType = engineType;
        this.thrustAtStandstill = thrustAtStandstill;
        this.thrustAtCruiseSpeed = thrustAtCruiseSpeed;
        this.tsfc = tsfc;
    }

    /** @return the engine model name */
    public String name() {
        return name;
    }

    /** @return the name of the manufacturer */
    public String makerName() {
        return makerName.toString();
    }

    /** @return the propulsion type of this engine */
    public EngineType engineType() {
        return engineType;
    }

    /** @return maximum thrust in kN */
    public double thrustAtStandstill() { return thrustAtStandstill; }
    public double thrustAtCruiseSpeed() { return thrustAtCruiseSpeed; }

    /** @return thrust-specific fuel consumption in kg/(kN·h) */
    public double tsfc() {
        return tsfc;
    }

    @Override
    public Long identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public String toString() {
        return String.format("EngineModel [name=%s, maker=%s, type=%s]", name, makerName, engineType);
    }
}
