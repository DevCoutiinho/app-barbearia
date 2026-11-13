package dev.barbershop.api.scheduling.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record SchedulingRequestDTO(

        @NotNull(message = "O barbeiro é obrigatório")
        UUID barberId,

        @NotNull(message = "O serviço é obrigatório")
        UUID serviceId,

        @NotNull(message = "A data e o horário são obrigatórios")
        Instant scheduledAt

) {
}
