package dev.barbershop.api.auth.authorization.dto;

import dev.barbershop.api.auth.authorization.enums.PermissionName;
import jakarta.validation.constraints.NotNull;

public record PermissionCreateDTO(

        @NotNull(message = "O nome da permissão é obrigatório")
        PermissionName name,

        String description

) {
}
