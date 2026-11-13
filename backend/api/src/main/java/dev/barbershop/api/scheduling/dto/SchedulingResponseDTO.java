package dev.barbershop.api.scheduling.dto;

import dev.barbershop.api.scheduling.enums.SchedulingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SchedulingResponseDTO(

        UUID id,

        String serviceName,

        String barberName,

        Instant scheduledAt,

        Integer durationTime,

        BigDecimal finalPrice,

        SchedulingStatus status

) {
}
