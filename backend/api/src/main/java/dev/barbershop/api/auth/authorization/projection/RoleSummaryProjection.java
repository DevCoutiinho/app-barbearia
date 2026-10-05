package dev.barbershop.api.auth.authorization.projection;

import dev.barbershop.api.auth.authorization.enums.RoleName;

import java.util.UUID;

public record RoleSummaryProjection(
        UUID id,
        RoleName name,
        String description
) {

}
