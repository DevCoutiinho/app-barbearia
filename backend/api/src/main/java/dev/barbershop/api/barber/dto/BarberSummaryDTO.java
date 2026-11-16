package dev.barbershop.api.barber.dto;

import java.util.UUID;

public record BarberSummaryDTO(
        UUID id,
        String name,
        String avatar,
        String telephone
) {
}
