package dev.barbershop.api.product.entity;


import dev.barbershop.api.common.auditing.Auditable;
import dev.barbershop.api.inventorymovement.entity.InventoryMovement;
import dev.barbershop.api.product.enums.UnitMeasurement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

}
