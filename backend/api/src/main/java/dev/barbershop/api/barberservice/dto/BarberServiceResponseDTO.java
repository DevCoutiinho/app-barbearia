package dev.barbershop.api.barberservice.dto;

import dev.barbershop.api.barberservice.dto.servicecategory.ServiceCategoryResponseDTO;
import dev.barbershop.api.barberservice.dto.serviceimage.ServiceImageResponseDTO;
import dev.barbershop.api.barberservice.enums.BarberServiceTechnique;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BarberServiceResponseDTO(
        UUID id,
        String name,
        BarberServiceTechnique technique,
        BigDecimal price,
        Boolean active,
        Integer durationTime,
        ServiceCategoryResponseDTO category,
        List<ServiceImageResponseDTO> images
) {
}
