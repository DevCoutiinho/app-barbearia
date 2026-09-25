package dev.barbershop.api.user.dto;

import dev.barbershop.api.auth.authorization.enums.RoleName;

import java.util.Set;
import java.util.UUID;

public record UserAdmDTO(
        UUID id,
        String name,
        String avatar,
        String email,
        String telephone,
        Boolean active,
        Set<RoleName> roles
) {
}