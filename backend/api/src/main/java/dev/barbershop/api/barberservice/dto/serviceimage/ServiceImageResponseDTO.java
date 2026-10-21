package dev.barbershop.api.barberservice.dto.serviceimage;

import java.util.UUID;

public record ServiceImageResponseDTO(
        UUID id,
        String url
) {
}
