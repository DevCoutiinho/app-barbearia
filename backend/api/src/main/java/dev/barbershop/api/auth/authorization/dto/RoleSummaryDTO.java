package dev.barbershop.api.auth.authorization.dto;

import java.util.UUID;

public record RoleSummaryDTO(
        UUID id,
        String name
) {
}
