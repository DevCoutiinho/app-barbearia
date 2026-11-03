package dev.barbershop.api.product.dto;

import dev.barbershop.api.product.enums.UnitMeasurement;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

public record ProductFilterDTO(
         String name,
         String description,
         UnitMeasurement unitMeasurement,
         BigDecimal priceCost,
         Boolean active
) {
}
