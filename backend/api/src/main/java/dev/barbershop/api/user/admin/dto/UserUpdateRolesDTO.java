package dev.barbershop.api.user.admin.dto;

import java.util.Set;
import java.util.UUID;

public record UserUpdateRolesDTO(
        Set<UUID> roleIds
) {

    public UserUpdateRolesDTO {
        if (roleIds == null) {
            roleIds = Set.of();
        }
    }
}
