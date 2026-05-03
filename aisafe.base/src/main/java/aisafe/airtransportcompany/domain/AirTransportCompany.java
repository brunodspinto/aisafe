package aisafe.airtransportcompany.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "T_AIR_TRANSPORT_COMPANY",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "company_name"),
                @UniqueConstraint(columnNames = "icao_code")
        })
public class AirTransportCompany implements AggregateRoot<IATACode> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private IATACode iataCode;

    @Column(name = "company_name", nullable = false)
    private String name;

    @Embedded
    @AttributeOverride(name = "icaoCode", column = @Column(name = "icao_code", nullable = false))
    private ICAOCode icaoCode;

    public AirTransportCompany(final String name, final IATACode iataCode, final ICAOCode icaoCode) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Company name cannot be blank");
        if (iataCode == null)
            throw new IllegalArgumentException("IATA code is required");
        if (icaoCode == null)
            throw new IllegalArgumentException("ICAO code is required");
        this.name = name.trim();
        this.iataCode = iataCode;
        this.icaoCode = icaoCode;
    }

    protected AirTransportCompany() {
        // for ORM
    }

    public String name() { return name; }
    public ICAOCode icaoCode() { return icaoCode; }

    @Override
    public IATACode identity() { return iataCode; }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }

    @Override
    public boolean sameAs(final Object other) { return DomainEntities.areEqual(this, other); }

    @Override
    public String toString() {
        return name + " (" + iataCode + " / " + icaoCode + ")";
    }
}
