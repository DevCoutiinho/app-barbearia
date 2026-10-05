package dev.barbershop.api.auth.authorization.dto;

import dev.barbershop.api.auth.authorization.enums.RoleName;

import java.util.Set;
import java.util.UUID;

public record RoleResponseDTO(
        UUID id,
        RoleName name,
        String description,
        Set<PermissionSummaryDTO> permissions
) {
}
