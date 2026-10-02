package dev.barbershop.api.auth.authorization.dto;

import java.util.Set;
import java.util.UUID;

public record RoleDTO(
    UUID id,
    String name,
    String description,
    Set<PermissionDTO> permissions
) {}
