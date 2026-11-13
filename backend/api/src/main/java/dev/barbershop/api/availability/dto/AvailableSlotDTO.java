package dev.barbershop.api.availability.dto;

import java.time.LocalTime;

public record AvailableSlotDTO(
        LocalTime startTime,
        LocalTime endTime
) {
}
