package aisafe.flightplan.domain;

import aisafe.dsl.ast.FlightPlanAst;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.util.Objects;

/**
 * Entity and Aggregate Root representing a Flight Plan.
 * A flight plan is created in DRAFT status and must undergo
 * a multi-step validation process (US080, US081, US085).
 */
@Entity
public class FlightPlan implements AggregateRoot<String> {

    @Id
    private String designator;

    private String flightType;

    @Enumerated(EnumType.STRING)
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
    public FlightPlan(final String designator, final String flightType, final String dslContent) {
        if (designator == null || designator.trim().isEmpty()) {
            throw new IllegalArgumentException("Flight plan designator cannot be null or empty.");
        }
        if (flightType == null || flightType.trim().isEmpty()) {
            throw new IllegalArgumentException("Flight type cannot be null or empty.");
        }
        if (dslContent == null || dslContent.trim().isEmpty()) {
            throw new IllegalArgumentException("DSL content cannot be null or empty.");
        }
        this.designator = designator.trim().toUpperCase();
        this.flightType = flightType.trim().toUpperCase();
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
                ast.identifier(),
                ast.flightType().name(),
                dslContent
        );
    }

    /** @return unique flight plan designator (always upper-case) */
    public String designator() {
        return designator;
    }

    /** @return flight type (e.g. "IFR", "VFR") */
    public String flightType() {
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

    // --- Identity Methods ---

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FlightPlan that = (FlightPlan) o;
        return Objects.equals(designator, that.designator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(designator);
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public String identity() {
        return this.designator;
    }

    @Override
    public String toString() {
        return String.format("FlightPlan{designator='%s', type='%s', status=%s}",
                designator, flightType, status);
    }
}