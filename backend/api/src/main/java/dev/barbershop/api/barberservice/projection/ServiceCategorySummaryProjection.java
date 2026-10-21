package dev.barbershop.api.barberservice.projection;

import java.util.UUID;

public record ServiceCategorySummaryProjection(
        UUID id,
        String name,
        String description,
        boolean active
) {
}
