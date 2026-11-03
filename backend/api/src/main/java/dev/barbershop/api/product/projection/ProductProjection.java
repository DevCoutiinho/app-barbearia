package dev.barbershop.api.product.projection;

import dev.barbershop.api.product.enums.UnitMeasurement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductProjection(
        UUID id,
        String name,
        String description,
        BigDecimal minimumQuantity,
        BigDecimal currentQuantity,
        UnitMeasurement unitMeasurement,
        BigDecimal priceCost,
        Boolean active,
        Instant updatedAt
) {
}
