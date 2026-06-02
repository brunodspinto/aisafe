package aisafe.flightplan.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value object representing the planned fuel quantity of a flight plan, in kilograms.
 * The amount must be strictly positive (US080, AC080.5).
 */
@Embeddable
public class FuelQuantity implements ValueObject, Comparable<FuelQuantity> {

    @Column(name = "fuel_quantity")
    private double amount;

    protected FuelQuantity() {
        // for ORM
    }

    /**
     * Creates a fuel quantity.
     *
     * @param amount the fuel amount in kilograms (must be strictly positive)
     * @throws IllegalArgumentException if {@code amount} is not strictly positive
     */
    public FuelQuantity(final double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Fuel quantity must be strictly positive.");
        this.amount = amount;
    }

    /**
     * Factory method — equivalent to the constructor.
     *
     * @param amount the fuel amount in kilograms
     * @return a new {@code FuelQuantity}
     */
    public static FuelQuantity valueOf(final double amount) {
        return new FuelQuantity(amount);
    }

    /** @return the fuel amount in kilograms */
    public double amount() {
        return amount;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FuelQuantity)) return false;
        return Double.compare(amount, ((FuelQuantity) o).amount) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(amount);
    }

    @Override
    public String toString() {
        return amount + " kg";
    }

    @Override
    public int compareTo(final FuelQuantity other) {
        return Double.compare(this.amount, other.amount);
    }
}
