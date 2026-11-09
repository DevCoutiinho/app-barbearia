package dev.barbershop.api.scheduling.dto;

import dev.barbershop.api.scheduling.enums.SchedulingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SchedulingResponseDTO(
        UUID id,
        UUID barberId,
        UUID serviceId,
        String serviceName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        SchedulingStatus status,
        BigDecimal finalPrice
) {
}

