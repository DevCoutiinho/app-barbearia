package dev.barbershop.api.user.dto;

import java.util.Set;

public record UserRoleUpdateDTO(
    Set<String> roles
) {}