package dev.barbershop.api.scheduling.dto;

import dev.barbershop.api.scheduling.enums.SchedulingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record BarberSchedulingFilterDTO(

        @Schema(description = "Data inicial para filtro do período (YYYY-MM-DD)", example = "2026-11-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @Schema(description = "Data final para filtro do período (YYYY-MM-DD)", example = "2026-11-30")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        @Schema(description = "Nome do cliente para busca parcial e case-insensitive", example = "Carlos")
        String clientName,

        @Schema(description = "Status do agendamento (PENDING, SCHEDULED, COMPLETED, CANCELED)")
        SchedulingStatus status
) {
}

