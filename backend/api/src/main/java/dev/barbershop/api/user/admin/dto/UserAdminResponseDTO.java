package dev.barbershop.api.user.admin.dto;

import dev.barbershop.api.auth.authorization.dto.RoleSummaryDTO;

import java.util.Set;
import java.util.UUID;

public record UserAdminResponseDTO(
        UUID id,
        String name,
        String email,
        String avatar,
        boolean active,
        Set<RoleSummaryDTO> roles
) {
}
