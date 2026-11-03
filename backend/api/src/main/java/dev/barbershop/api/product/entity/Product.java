package dev.barbershop.api.product.entity;


import dev.barbershop.api.common.auditing.Auditable;
import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import dev.barbershop.api.product.enums.UnitMeasurement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(
        onlyExplicitlyIncluded = true,
        callSuper = false
)
@ToString(exclude = {"inventoryMovements"})
@Table(name = "products")
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(name = "minimum_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumQuantity;

    @Column(name = "current_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_measurement", nullable = false, length = 50)
    private UnitMeasurement unitMeasurement;

    @Column(name = "price_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceCost;

    @Column(nullable = false)
    private Boolean active;

    @OneToMany(mappedBy = "product")
    @Builder.Default
    List<InventoryMovement> inventoryMovements = new ArrayList<>();

    public void changeName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeMinimumQuantity(BigDecimal minimumQuantity) {
        Objects.requireNonNull(minimumQuantity);

        if (minimumQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantidade minima do produto menor que zero");
        }

        this.minimumQuantity = minimumQuantity;
    }

    public void addStock(BigDecimal quantity) {
        Objects.requireNonNull(quantity);

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade de entrada deve ser maior que zero");
        }

        this.currentQuantity = this.currentQuantity.add(quantity);
    }

    public void removeStock(BigDecimal quantity) {
        decreaseQuantity(quantity);
    }

    public void decreaseQuantity(BigDecimal quantity) {
        Objects.requireNonNull(quantity);

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade de consumo deve ser maior que zero");
        }

        if (quantity.compareTo(this.currentQuantity) > 0) {
            throw new IllegalArgumentException("Quantidade de consumo maior que o permitido");
        }

        this.currentQuantity = this.currentQuantity.subtract(quantity);
    }

    public void changeUnitMeasurement(UnitMeasurement unitMeasurement) {
        this.unitMeasurement = Objects.requireNonNull(unitMeasurement);
    }

    public void changePriceCost(BigDecimal priceCost) {
        Objects.requireNonNull(priceCost);

        if (priceCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço do produto menor que zero");
        }

        this.priceCost = priceCost;
    }

    public void deactivate() {
        this.active = Boolean.FALSE;
    }

    public void activate() {
        this.active = Boolean.TRUE;
    }

}
