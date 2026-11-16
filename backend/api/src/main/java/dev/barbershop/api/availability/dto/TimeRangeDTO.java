package dev.barbershop.api.availability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record TimeRangeDTO(

        @Schema(example = "08:00", type = "string")
        @NotNull(message = "O horário inicial é obrigatório")
        LocalTime start,

        @Schema(example = "12:00", type = "string")
        @NotNull(message = "O horário final é obrigatório")
        LocalTime end
) {
}

