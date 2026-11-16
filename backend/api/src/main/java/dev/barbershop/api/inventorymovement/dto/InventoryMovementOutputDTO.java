package dev.barbershop.api.inventorymovement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryMovementOutputDTO(
        @NotNull
        UUID userId,

        @NotNull
        @DecimalMin(value = "0.01", inclusive = true, message = "")
        @DecimalMax(value = "9999.99", inclusive = true, message = "")
        BigDecimal quantity
) {
}
