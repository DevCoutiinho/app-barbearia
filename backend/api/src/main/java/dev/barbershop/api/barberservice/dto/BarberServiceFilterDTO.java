package dev.barbershop.api.barberservice.dto;

import dev.barbershop.api.barberservice.enums.BarberServiceTechnique;

import java.math.BigDecimal;
import java.util.UUID;

public record BarberServiceFilterDTO(
        String name,
        BarberServiceTechnique technique,
        Boolean active,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        UUID categoryId

) {
}
