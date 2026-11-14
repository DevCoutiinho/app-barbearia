package dev.barbershop.api.scheduling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SchedulingCreateDTO(

        @Schema(description = "ID do barbeiro escolhido. Se informado, o sistema valida se o serviço pertence a ele e se o barbeiro está ativo.", nullable = true)
        UUID barberId,

        @NotNull(message = "O serviço é obrigatório")
        UUID serviceId,

        @Schema(example = "2026-11-20T09:00:00-03:00")
        @NotNull(message = "O horário de início é obrigatório")
        OffsetDateTime startAt
) {
}

