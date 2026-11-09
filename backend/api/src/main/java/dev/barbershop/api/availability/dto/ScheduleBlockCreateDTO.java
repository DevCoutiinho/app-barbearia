package dev.barbershop.api.availability.dto;

import dev.barbershop.api.availability.enums.BlockReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Bloqueio pontual. Horários no fuso da barbearia.
 * Se startTime e endTime forem omitidos, o bloqueio vale para o dia inteiro.
 */
public record ScheduleBlockCreateDTO(

        @Schema(example = "2026-11-20")
        @NotNull(message = "A data do bloqueio é obrigatória")
        LocalDate date,

        @Schema(example = "12:00", type = "string", nullable = true)
        LocalTime startTime,

        @Schema(example = "14:00", type = "string", nullable = true)
        LocalTime endTime,

        @Schema(example = "LUNCH")
        @NotNull(message = "O motivo do bloqueio é obrigatório")
        BlockReason reason,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description
) {
}

