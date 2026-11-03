package dev.barbershop.api.inventorymovement.dto;

import dev.barbershop.api.inventorymovement.enums.InventoryMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class InventoryMovementCreateDTO {

    @NotNull
    private UUID productId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal quantity;

    @NotNull
    private InventoryMovementType type;
}
