package dev.barbershop.api.user.dto;

import dev.barbershop.api.auth.authorization.enums.RoleName;

import java.util.Set;

public record UserRoleUpdateDTO(
    Set<RoleName> roles
) {}