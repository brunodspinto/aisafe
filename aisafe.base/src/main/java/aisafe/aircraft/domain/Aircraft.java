package aisafe.aircraft.domain;

import aisafe.aircraftmodel.domain.AircraftModel;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "T_AIRCRAFT")
public class Aircraft implements AggregateRoot<String> {

    @Id
    @Column(name = "registration_number", nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private String registeredCountry;

    @Column(nullable = false)
    private int numberOfCrewElements;

    @Embedded
    private CabinConfiguration cabinConfiguration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationalStatus operationalStatus;

    @ManyToOne
    private AircraftModel aircraftModel;

    public Aircraft(final String registrationNumber,
                    final String registeredCountry,
                    final int numberOfCrewElements,
                    final CabinConfiguration cabinConfiguration,
                    final AircraftModel aircraftModel) {
        if (registrationNumber == null || registrationNumber.isBlank())
            throw new IllegalArgumentException("Registration number cannot be blank.");
        if (registeredCountry == null || registeredCountry.isBlank())
            throw new IllegalArgumentException("Registered country cannot be blank.");
        if (numberOfCrewElements < 1)
            throw new IllegalArgumentException("Number of crew elements must be at least 1.");
        if (cabinConfiguration == null)
            throw new IllegalArgumentException("Cabin configuration is required.");
        if (aircraftModel == null)
            throw new IllegalArgumentException("Aircraft model is required.");
        if (aircraftModel.maxCapacity() > 0 && cabinConfiguration.totalSeats() > aircraftModel.maxCapacity())
            throw new IllegalArgumentException(
                    "Total seats (" + cabinConfiguration.totalSeats()
                    + ") exceeds the aircraft model's maximum capacity (" + aircraftModel.maxCapacity() + ").");

        this.registrationNumber = registrationNumber.toUpperCase().trim();
        this.registeredCountry = registeredCountry.trim();
        this.numberOfCrewElements = numberOfCrewElements;
        this.cabinConfiguration = cabinConfiguration;
        this.aircraftModel = aircraftModel;
        this.operationalStatus = OperationalStatus.ACTIVE;
    }

    protected Aircraft() {
        // for ORM
    }

    public String registrationNumber() { return registrationNumber; }
    public String registeredCountry() { return registeredCountry; }
    public int numberOfCrewElements() { return numberOfCrewElements; }
    public CabinConfiguration cabinConfiguration() { return cabinConfiguration; }
    public OperationalStatus operationalStatus() { return operationalStatus; }
    public AircraftModel aircraftModel() { return aircraftModel; }

    @Override
    public String identity() { return registrationNumber; }

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
}
