package dev.barbershop.api.availability.dto;

import java.time.OffsetDateTime;

/**
 * Horário disponível para o serviço solicitado: o intervalo inteiro [startAt, endAt) está livre.
 */
public record AvailableSlotDTO(
        OffsetDateTime startAt,
        OffsetDateTime endAt
) {
}

