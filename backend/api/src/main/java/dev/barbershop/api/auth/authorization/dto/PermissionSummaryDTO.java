package dev.barbershop.api.auth.authorization.dto;

import dev.barbershop.api.auth.authorization.enums.PermissionName;

import java.util.UUID;

public record PermissionSummaryDTO(
        UUID id,
        PermissionName name,
        String description
) {
}
