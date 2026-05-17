package aisafe.aircraft.domain;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.domain.AircraftType;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Aggregate root representing a physical aircraft registered in an air transport company's fleet.
 * An aircraft is uniquely identified by its registration number and starts in {@link OperationalStatus#ACTIVE} status.
 */
@Entity
@Table(name = "T_AIRCRAFT")
public class Aircraft implements AggregateRoot<RegistrationNumber> {

    @EmbeddedId
    private RegistrationNumber registrationNumber;

    @Version
    private Long version;

    @Column(nullable = false)
    private String registeredCountry;

    @Column(nullable = false)
    private int numberOfCrewElements;

    @Column(nullable = false)
    private int yearOfManufacture;

    @Embedded
    private CabinConfiguration cabinConfiguration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationalStatus operationalStatus;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {})
    @JoinColumn(name = "aircraft_model_id")
    private AircraftModel aircraftModel;

    /**
     * Creates a new aircraft and sets its status to {@link OperationalStatus#ACTIVE}.
     *
     * <p>For {@link AircraftType#CARGO} aircraft, {@code cabinConfiguration} must be {@code null}.
     * For {@link AircraftType#PASSENGER} and {@link AircraftType#MIXED} aircraft,
     * {@code cabinConfiguration} must be non-null.
     *
     * @param registrationNumber   unique ICAO/national registration (e.g. "CS-TUG"); must not be null
     * @param registeredCountry    country where the aircraft is registered; must not be blank
     * @param numberOfCrewElements minimum crew size; must be at least 1
     * @param yearOfManufacture    year the aircraft was built; must be between 1900 and the current year
     * @param cabinConfiguration   seat distribution; null for CARGO, required for PASSENGER/MIXED
     * @param aircraftModel        the aircraft model; must not be {@code null}
     * @throws IllegalArgumentException if any constraint is violated
     */
    public Aircraft(final RegistrationNumber registrationNumber,
                    final String registeredCountry,
                    final int numberOfCrewElements,
                    final int yearOfManufacture,
                    final CabinConfiguration cabinConfiguration,
                    final AircraftModel aircraftModel) {
        if (registrationNumber == null)
            throw new IllegalArgumentException("Registration number cannot be blank.");
        if (registeredCountry == null || registeredCountry.isBlank())
            throw new IllegalArgumentException("Registered country cannot be blank.");
        if (numberOfCrewElements < 1)
            throw new IllegalArgumentException("Number of crew elements must be at least 1.");
        if (yearOfManufacture < 1900 || yearOfManufacture > java.time.Year.now().getValue())
            throw new IllegalArgumentException("Year of manufacture must be between 1900 and the current year.");
        if (aircraftModel == null)
            throw new IllegalArgumentException("Aircraft model is required.");
        if (aircraftModel.aircraftType() == AircraftType.CARGO) {
            if (cabinConfiguration != null)
                throw new IllegalArgumentException("Cargo aircraft must not have a cabin configuration.");
        } else {
            if (cabinConfiguration == null)
                throw new IllegalArgumentException("Cabin configuration is required for passenger and mixed aircraft.");
            if (aircraftModel.maxCapacity() > 0 && cabinConfiguration.totalSeats() > aircraftModel.maxCapacity())
                throw new IllegalArgumentException(
                        "Total seats (" + cabinConfiguration.totalSeats()
                        + ") exceeds the aircraft model's maximum capacity (" + aircraftModel.maxCapacity() + ").");
        }

        this.registrationNumber = registrationNumber;
        this.registeredCountry = registeredCountry.trim();
        this.numberOfCrewElements = numberOfCrewElements;
        this.yearOfManufacture = yearOfManufacture;
        this.cabinConfiguration = cabinConfiguration;
        this.aircraftModel = aircraftModel;
        this.operationalStatus = OperationalStatus.ACTIVE;
    }

    protected Aircraft() {
        // for ORM
    }

    /** @return unique registration number (always upper-case) */
    public String registrationNumber() { return registrationNumber.toString(); }

    /** @return country where the aircraft is registered */
    public String registeredCountry() { return registeredCountry; }

    /** @return minimum number of crew members required */
    public int numberOfCrewElements() { return numberOfCrewElements; }

    /** @return year the aircraft was manufactured */
    public int yearOfManufacture() { return yearOfManufacture; }

    /**
     * @return cabin seat distribution, or {@code null} for CARGO aircraft
     */
    public CabinConfiguration cabinConfiguration() {
        if (aircraftModel.aircraftType() == AircraftType.CARGO) return null;
        return cabinConfiguration;
    }

    /** @return current operational status */
    public OperationalStatus operationalStatus() { return operationalStatus; }

    /** @return aircraft model associated with this aircraft */
    public AircraftModel aircraftModel() { return aircraftModel; }

    @Override
    public RegistrationNumber identity() { return registrationNumber; }

    @Override
    public boolean sameAs(final Object other) { return DomainEntities.areEqual(this, other); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return registrationNumber.equals(((Aircraft) o).registrationNumber);
    }

    @Override
    public int hashCode() { return registrationNumber.hashCode(); }

    @Override
    public String toString() {
        return String.format("Aircraft{registration='%s', country='%s', model='%s', status=%s}",
                registrationNumber, registeredCountry, aircraftModel.modelName(), operationalStatus);
    }

    /**
     * Permanently retires this aircraft by setting its status to {@link OperationalStatus#DECOMMISSIONED}.
     *
     * @throws IllegalStateException if the aircraft is already decommissioned
     */
    public void decommission() {
        if (this.operationalStatus == OperationalStatus.DECOMMISSIONED)
            throw new IllegalStateException("Aircraft is already decommissioned.");
        this.operationalStatus = OperationalStatus.DECOMMISSIONED;
    }

    /**
     * @return {@code true} if the aircraft's status is {@link OperationalStatus#ACTIVE}
     */
    public boolean isActive() {
        return this.operationalStatus == OperationalStatus.ACTIVE;
    }
}
