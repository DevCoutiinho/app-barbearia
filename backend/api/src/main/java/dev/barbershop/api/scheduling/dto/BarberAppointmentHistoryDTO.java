package dev.barbershop.api.scheduling.dto;

import dev.barbershop.api.scheduling.enums.SchedulingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BarberAppointmentHistoryDTO(
        UUID id,
        UUID clientId,
        String clientName,
        UUID serviceId,
        String serviceName,
        SchedulingStatus status,
        OffsetDateTime scheduledAt,
        OffsetDateTime endsAt,
        Integer durationTime,
        BigDecimal price
) {
}

