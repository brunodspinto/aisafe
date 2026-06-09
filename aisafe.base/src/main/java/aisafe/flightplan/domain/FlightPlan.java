package aisafe.flightplan.domain;

import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.ast.FlightType;
import aisafe.flightroute.domain.RouteName;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


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

    @Embedded
    @AttributeOverride(name = "name", column = @Column(name = "route_name"))
    private RouteName routeName;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "aircraft_registration"))
    private RegistrationNumber aircraftRegistration;

    @Column(name = "assigned_pilot_id")
    private Long assignedPilotId;

    @Column(name = "departure_date_time")
    private LocalDateTime departureDateTime;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "fuel_quantity"))
    private FuelQuantity fuelQuantity;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "T_FLIGHT_PLAN_WEATHER_DATA",
            joinColumns = @JoinColumn(name = "flight_plan_designator"))
    @Column(name = "weather_data_id")
    private Set<Long> weatherDataIds = new HashSet<>();

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

    /**
     * Creates a new FlightPlan from form input (US080).
     * The plan references its route, aircraft and pilot by identity, and starts in DRAFT status.
     * The {@code dslContent} is null for form-based plans.
     *
     * @param designator           the unique flight plan designator (non-null)
     * @param flightType           the flight type (non-null)
     * @param routeName            the route this plan is for (non-null)
     * @param aircraftRegistration the assigned aircraft's registration (non-null)
     * @param assignedPilotId      the assigned pilot's identity (non-null)
     * @param departureDateTime    the planned departure date/time (non-null, must be in the future)
     * @param fuelQuantity         the planned fuel quantity (non-null, strictly positive)
     * @throws IllegalArgumentException if any argument is null or the departure is not in the future
     */
    public FlightPlan(final FlightPlanDesignator designator,
                      final FlightType flightType,
                      final RouteName routeName,
                      final RegistrationNumber aircraftRegistration,
                      final Long assignedPilotId,
                      final LocalDateTime departureDateTime,
                      final FuelQuantity fuelQuantity) {
        if (designator == null)
            throw new IllegalArgumentException("Flight plan designator cannot be null or empty.");
        if (flightType == null)
            throw new IllegalArgumentException("Flight type cannot be null.");
        if (routeName == null)
            throw new IllegalArgumentException("Flight route cannot be null.");
        if (aircraftRegistration == null)
            throw new IllegalArgumentException("Aircraft cannot be null.");
        if (assignedPilotId == null)
            throw new IllegalArgumentException("Assigned pilot cannot be null.");
        if (departureDateTime == null)
            throw new IllegalArgumentException("Departure date/time cannot be null.");
        if (departureDateTime.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Departure date/time must be in the future.");
        if (fuelQuantity == null)
            throw new IllegalArgumentException("Fuel quantity cannot be null.");

        this.designator = designator;
        this.flightType = flightType;
        this.routeName = routeName;
        this.aircraftRegistration = aircraftRegistration;
        this.assignedPilotId = assignedPilotId;
        this.departureDateTime = departureDateTime;
        this.fuelQuantity = fuelQuantity;
        this.status = FlightPlanStatus.DRAFT;
        this.dslContent = null;
    }

    /** @return unique flight plan designator string (always upper-case) */
    public String designator() {
        return designator.toString();
    }

    /** @return the route name this plan is for, or {@code null} for DSL-imported plans */
    public RouteName routeName() {
        return routeName;
    }

    /** @return the assigned aircraft's registration, or {@code null} for DSL-imported plans */
    public RegistrationNumber aircraftRegistration() {
        return aircraftRegistration;
    }

    /** @return the assigned pilot's identity, or {@code null} for DSL-imported plans */
    public Long assignedPilotId() {
        return assignedPilotId;
    }

    /** @return the planned departure date/time, or {@code null} for DSL-imported plans */
    public LocalDateTime departureDateTime() {
        return departureDateTime;
    }

    /** @return the planned fuel quantity, or {@code null} for DSL-imported plans */
    public FuelQuantity fuelQuantity() {
        return fuelQuantity;
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

    /**
     * Attaches weather data to this flight plan (US082).
     * If the weather data id is genuinely new (it was not yet attached) and the plan had
     * already been {@link FlightPlanStatus#TESTED}, the test is voided and the status reverts
     * to {@link FlightPlanStatus#VALIDATED} — the new weather conditions invalidate the previous
     * test. The DSL/semantic validation is preserved. For {@code DRAFT}/{@code VALIDATED} plans,
     * or when the weather data was already attached, the status is unchanged.
     *
     * @param weatherDataId the identity of an existing {@code WeatherData} record (non-null)
     * @throws IllegalArgumentException if {@code weatherDataId} is null
     */
    public void addWeatherData(final Long weatherDataId) {
        if (weatherDataId == null)
            throw new IllegalArgumentException("Weather data id cannot be null.");

        final boolean added = this.weatherDataIds.add(weatherDataId);

        if (added && this.status == FlightPlanStatus.TESTED) {
            this.status = FlightPlanStatus.VALIDATED;
        }
    }

    /** @return unmodifiable set of weather data identities attached to this flight plan */
    public Set<Long> weatherDataIds() {
        return Collections.unmodifiableSet(weatherDataIds);
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
