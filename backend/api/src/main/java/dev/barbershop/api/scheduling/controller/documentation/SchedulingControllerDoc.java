package dev.barbershop.api.scheduling.controller.documentation;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.scheduling.dto.BarberAppointmentHistoryDTO;
import dev.barbershop.api.scheduling.dto.BarberSchedulingFilterDTO;
import dev.barbershop.api.scheduling.dto.SchedulingCreateDTO;
import dev.barbershop.api.scheduling.dto.SchedulingResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Agendamentos", description = "Criação e cancelamento de agendamentos")
@RequestMapping("/schedulings")
public interface SchedulingControllerDoc {

    @Operation(
            summary = "Listar meus agendamentos",
            description = "Retorna todos os agendamentos pertencentes ao cliente autenticado, ordenados do mais recente para o mais antigo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamentos retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @GetMapping("/me")
    ResponseEntity<ApiResponseDTO<SchedulingResponseDTO>> findMySchedulings(
            @AuthenticationPrincipal CustomUserDetails customUser
    );

    @Operation(
            summary = "Histórico de atendimentos do barbeiro logado",
            description = "Retorna os atendimentos/agendamentos associados ao barbeiro logado, com filtros opcionais por intervalo de data (startDate, endDate), nome do cliente e status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado - recurso exclusivo para barbeiros",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @GetMapping("/barber/history")
    ResponseEntity<ApiResponseDTO<Page<BarberAppointmentHistoryDTO>>> findBarberHistory(
            @ParameterObject @ModelAttribute BarberSchedulingFilterDTO filters,
            @ParameterObject @PageableDefault(size = 10, sort = "scheduledAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails customUser
    );

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
            summary = "Finalizar atendimento",
            description = "Muda o status do agendamento para COMPLETED após a realização do serviço."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atendimento finalizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status não permite finalização",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Agendamento pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @PatchMapping("/{id}/complete")
    ResponseEntity<ApiResponseDTO<SchedulingResponseDTO>> complete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails customUser
    );

    @Operation(
            summary = "Listar agendamentos do dia do barbeiro",
            description = "Retorna os atendimentos do dia para o barbeiro logado na data especificada (padrão: data atual)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamentos do dia retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado - recurso exclusivo para barbeiros",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @GetMapping({"/barber/daily", "/barber/today"})
    ResponseEntity<ApiResponseDTO<BarberAppointmentHistoryDTO>> findBarberDaily(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @AuthenticationPrincipal CustomUserDetails customUser
    );

    @Operation(
            summary = "Remarcar agendamento",
            description = """
                    Remarca um agendamento existente para um novo horário disponível de forma atômica.
                    Valida permissão, horário dentro da jornada, bloqueios e conflitos com outros agendamentos.
                    Se o novo horário não estiver disponível ou ocorrer concorrência, o agendamento original permanece intocado.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento remarcado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Novo horário inválido ou fora da janela permitida",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Agendamento pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Horário indisponível ou agendamento passado/finalizado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @PatchMapping("/{id}/reschedule")
    ResponseEntity<ApiResponseDTO<SchedulingResponseDTO>> reschedule(
            @PathVariable UUID id,
            @RequestBody @Valid dev.barbershop.api.scheduling.dto.SchedulingRescheduleDTO dto,
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

