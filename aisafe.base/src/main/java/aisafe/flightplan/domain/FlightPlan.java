package aisafe.flightplan.domain;

import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.ast.FlightType;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;


/**
 * Entity and Aggregate Root representing a Flight Plan.
 * A flight plan is created in DRAFT status and must undergo
 * a multi-step validation process (US080, US081, US085).
 */
@Entity
@Table(name = "T_FLIGHT_PLAN")
public class FlightPlan implements AggregateRoot<FlightPlanDesignator> {

    @EmbeddedId
    private FlightPlanDesignator designator;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlightType flightType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlightPlanStatus status;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String dslContent;

    /**
     * Protected constructor required by JPA.
     */
    protected FlightPlan() {
        // for ORM only
    }

    /**
     * Creates a new FlightPlan from a validated DSL file.
     * Status starts as DRAFT.
     */
    public FlightPlan(final FlightPlanDesignator designator, final FlightType flightType, final String dslContent) {
        if (designator == null) {
            throw new IllegalArgumentException("Flight plan designator cannot be null or empty.");
        }
        if (flightType == null) {
            throw new IllegalArgumentException("Flight type cannot be null.");
        }
        if (dslContent == null || dslContent.trim().isEmpty()) {
            throw new IllegalArgumentException("DSL content cannot be null or empty.");
        }
        this.designator = designator;
        this.flightType = flightType;
        this.dslContent = dslContent;
        this.status = FlightPlanStatus.DRAFT;
    }

    /**
     * Factory method that creates a {@link FlightPlan} from a parsed AST and the original DSL source.
     *
     * @param ast        the validated parse tree containing the designator and flight type
     * @param dslContent the raw DSL text to store alongside the plan
     * @return a new {@link FlightPlan} in {@link FlightPlanStatus#DRAFT} status
     */
    public static FlightPlan fromDsl(final FlightPlanAst ast, final String dslContent) {
        return new FlightPlan(
                FlightPlanDesignator.valueOf(ast.identifier()),
                ast.flightType(),
                dslContent
        );
    }

    /** @return unique flight plan designator string (always upper-case) */
    public String designator() {
        return designator.toString();
    }

    /** @return flight type classification */
    public FlightType flightType() {
        return flightType;
    }

    /** @return current lifecycle status */
    public FlightPlanStatus status() {
        return status;
    }

    /** @return original DSL source content */
    public String dslContent() {
        return dslContent;
    }

    /**
     * Advances this flight plan from {@link FlightPlanStatus#DRAFT} to
     * {@link FlightPlanStatus#VALIDATED} after all validation checks pass (US080, US081).
     *
     * @throws IllegalStateException if the current status is not DRAFT
     */
    public void markValidated() {
        if (this.status != FlightPlanStatus.DRAFT) {
            throw new IllegalStateException(
                    "Cannot validate a flight plan that is not in DRAFT status. Current status: " + status);
        }
        this.status = FlightPlanStatus.VALIDATED;
    }

    /**
     * Advances this flight plan from {@link FlightPlanStatus#VALIDATED} to
     * {@link FlightPlanStatus#TESTED} after successful simulation (US085).
     *
     * @throws IllegalStateException if the current status is not VALIDATED
     */
    public void markTested() {
        if (this.status != FlightPlanStatus.VALIDATED) {
            throw new IllegalStateException(
                    "Cannot mark a flight plan as tested unless it is in VALIDATED status. Current status: " + status);
        }
        this.status = FlightPlanStatus.TESTED;
    }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public FlightPlanDesignator identity() {
        return this.designator;
    }

    @Override
    public String toString() {
        return String.format("FlightPlan{designator='%s', type='%s', status=%s}",
                designator, flightType, status);
    }
}
