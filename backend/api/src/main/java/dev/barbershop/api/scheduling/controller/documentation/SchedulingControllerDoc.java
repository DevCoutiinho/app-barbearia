package dev.barbershop.api.scheduling.controller.documentation;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.scheduling.dto.SchedulingCreateDTO;
import dev.barbershop.api.scheduling.dto.SchedulingResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Agendamentos", description = "Criação e cancelamento de agendamentos")
@RequestMapping("/schedulings")
public interface SchedulingControllerDoc {

    @Operation(
            summary = "Criar agendamento",
            description = """
                    Reserva um horário para o serviço. O fim é calculado pela duração do serviço.
                    O intervalo inteiro precisa estar dentro da grade vigente, livre de bloqueios e de outros agendamentos.
                    Requisições simultâneas para o mesmo horário: apenas uma é aceita, as demais recebem 409.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agendamento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Horário no passado ou além da janela permitida",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cliente ou serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Horário indisponível",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @PostMapping
    ResponseEntity<ApiResponseDTO<SchedulingResponseDTO>> create(
            @RequestBody @Valid SchedulingCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUser
    );

    @Operation(
            summary = "Cancelar agendamento",
            description = "Cancela um agendamento pendente ou confirmado. Permitido ao cliente dono ou ao barbeiro responsável."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento cancelado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status não permite cancelamento",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Agendamento pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @PatchMapping("/{id}/cancel")
    ResponseEntity<ApiResponseDTO<SchedulingResponseDTO>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails customUser
    );
}

