package dev.barbershop.api.availability.dto;

import dev.barbershop.api.availability.enums.BlockReason;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleBlockResponseDTO(
        UUID id,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        BlockReason reason,
        String reasonLabel,
        String description
) {
}

