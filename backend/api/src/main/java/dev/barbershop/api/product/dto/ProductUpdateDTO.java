package dev.barbershop.api.product.dto;

import dev.barbershop.api.product.enums.UnitMeasurement;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductUpdateDTO(
        @NotBlank(message = "O nome do produto é obrigatório")
        @Size(max = 100)
        String name,

        String description,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true, message = "")
        @DecimalMax(value = "9999.99", inclusive = true, message = "")
        BigDecimal minimumQuantity,

        @NotNull
        UnitMeasurement unitMeasurement,

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax(value = "9999.99", inclusive = true, message = "")
        BigDecimal priceCost
) {
}
