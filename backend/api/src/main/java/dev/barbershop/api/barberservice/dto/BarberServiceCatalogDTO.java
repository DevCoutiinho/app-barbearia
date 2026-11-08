package dev.barbershop.api.barberservice.dto;

import dev.barbershop.api.barber.dto.BarberSummaryDTO;
import dev.barbershop.api.barberservice.enums.BarberServiceTechnique;

import java.math.BigDecimal;
import java.util.UUID;

public record BarberServiceCatalogDTO(
        UUID id,
        String name,
        BarberServiceTechnique technique,
        BigDecimal price,
        Integer durationTime,
        BarberSummaryDTO barber
) {
    public BarberServiceCatalogDTO(UUID id, String name, BarberServiceTechnique technique, BigDecimal price, Integer durationTime, UUID barberId, String barberName, String avatar, String telephone ){
        this(id,name,technique,price,durationTime,new BarberSummaryDTO(barberId,barberName, avatar, telephone));
    }
}
