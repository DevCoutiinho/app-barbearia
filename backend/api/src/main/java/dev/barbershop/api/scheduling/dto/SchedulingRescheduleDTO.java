package dev.barbershop.api.scheduling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record SchedulingRescheduleDTO(

        @Schema(example = "2026-11-20T16:00:00-03:00")
        @NotNull(message = "O novo horário de início é obrigatório")
        OffsetDateTime newStartAt
) {
}

