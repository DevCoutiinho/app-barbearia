package dev.barbershop.api.barberservice.dto.servicecategory;

import java.util.UUID;

public record ServiceCategoryResponseDTO(
        UUID id,
        String name
) {
}
