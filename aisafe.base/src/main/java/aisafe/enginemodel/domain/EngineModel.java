package aisafe.enginemodel.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entity and Aggregate Root representing an aircraft engine model.
 *
 * <p>The maker is referenced by name (external reference) rather than
 * an embedded entity, to maintain low coupling with the Maker aggregate.</p>
 */
@Entity
@Table(name = "T_ENGINE_MODEL",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "makerName"})
        })
public class EngineModel implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String makerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EngineType engineType;

    @Column(nullable = false)
    private double thrust;

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
     * @param makerName  the manufacturer name (non-blank)
     * @param engineType the engine type (non-null)
     * @param thrust     the thrust in kN (must be &gt; 0)
     * @param tsfc       the thrust-specific fuel consumption (must be &gt; 0)
     */
    public EngineModel(final String name, final String makerName, final EngineType engineType,
                       final double thrust, final double tsfc) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Engine model name cannot be null or blank.");
        }
        if (makerName == null || makerName.isBlank()) {
            throw new IllegalArgumentException("Maker name cannot be null or blank.");
        }
        if (engineType == null) {
            throw new IllegalArgumentException("Engine type cannot be null.");
        }
        if (thrust <= 0) {
            throw new IllegalArgumentException("Thrust must be greater than zero.");
        }
        if (tsfc <= 0) {
            throw new IllegalArgumentException("TSFC must be greater than zero.");
        }
        this.name = name.trim();
        this.makerName = makerName.trim();
        this.engineType = engineType;
        this.thrust = thrust;
        this.tsfc = tsfc;
    }

    public String name() {
        return name;
    }

    public String makerName() {
        return makerName;
    }

    public EngineType engineType() {
        return engineType;
    }

    public double thrust() {
        return thrust;
    }

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
