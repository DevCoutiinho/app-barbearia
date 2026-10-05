package dev.barbershop.api.auth.authorization.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record RoleUpdateDTO(
        String description,

        @NotEmpty(message = "É necessário informar pelo menos uma permissão")
        Set<UUID> permissionIds
) {
}
