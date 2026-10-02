package dev.barbershop.api.auth.authorization.dto;

import java.util.UUID;

public record PermissionDTO(
    UUID id,
    String name,
    String description
) {}
