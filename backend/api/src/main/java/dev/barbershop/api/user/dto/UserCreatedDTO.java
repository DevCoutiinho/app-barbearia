package dev.barbershop.api.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedDTO(
        UUID id,
        String name,
        String email,
        Instant createdAt
) {
}
