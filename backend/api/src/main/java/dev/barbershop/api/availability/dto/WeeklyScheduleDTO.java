package dev.barbershop.api.availability.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Grade semanal completa. Na escrita, dias não informados são considerados folga.
 * Na leitura, os 7 dias são sempre retornados.
 */
public record WeeklyScheduleDTO(

        @NotNull(message = "A lista de dias é obrigatória")
        List<@Valid @NotNull WorkingDayDTO> days
) {
}

