package dev.barbershop.api.availability.controller.documentation;

import dev.barbershop.api.availability.dto.AvailableSlotDTO;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Agenda - Disponibilidade", description = "Horários livres de um barbeiro para um serviço")
public interface AvailabilityControllerDoc {

    @Operation(
            summary = "Consultar horários disponíveis",
            description = """
                    Calcula: grade de trabalho - bloqueios - agendamentos ativos.
                    Só retorna horários em que o intervalo inteiro necessário para o serviço está livre.
                    A disponibilidade é revalidada no momento do agendamento.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horários disponíveis retornados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Data no passado ou além da janela permitida",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado ou inativo para o barbeiro",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @GetMapping("/barbers/{barberId}/availability")
    ResponseEntity<ApiResponseDTO<AvailableSlotDTO>> findAvailableSlots(
            @PathVariable UUID barberId,
            @RequestParam UUID serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );
}

