package dev.barbershop.api.user.dto;

import java.util.UUID;

public record UserSummaryDTO(
        UUID id,
        String name
) {
}
