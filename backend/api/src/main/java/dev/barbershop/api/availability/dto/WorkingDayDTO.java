package dev.barbershop.api.availability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Um dia da grade semanal. Lista de intervalos vazia = folga.
 */
public record WorkingDayDTO(

        @Schema(example = "MONDAY")
        @NotNull(message = "O dia da semana é obrigatório")
        DayOfWeek dayOfWeek,

        @NotNull(message = "A lista de intervalos é obrigatória (use lista vazia para folga)")
        List<@Valid @NotNull TimeRangeDTO> intervals
) {
}

