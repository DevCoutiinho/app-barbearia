package dev.barbershop.api.auth.authorization.dto;

import dev.barbershop.api.auth.authorization.enums.RoleName;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record RoleCreateDTO(

        @NotNull(message = "O nome da role é obrigatório")
        RoleName name,

        String description,

        @NotEmpty(message = "É necessário informar pelo menos uma permission")
        Set<UUID> permissionIds
) {
}
