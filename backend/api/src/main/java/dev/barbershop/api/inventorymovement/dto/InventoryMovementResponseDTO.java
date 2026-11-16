package dev.barbershop.api.inventorymovement.dto;

import dev.barbershop.api.inventorymovement.enums.InventoryMovementType;
import dev.barbershop.api.user.dto.UserSummaryDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryMovementResponseDTO(
        UUID id,
        Instant createdAt,
        UserSummaryDTO user,
        BigDecimal quantity,
        InventoryMovementType type
) {
    public InventoryMovementResponseDTO(UUID id, Instant createdAt, UUID userId, String userName, BigDecimal quantity, InventoryMovementType type) {
        this(id, createdAt, new UserSummaryDTO(userId, userName), quantity, type);
    }
}
