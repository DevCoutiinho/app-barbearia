package dev.barbershop.api.availability.controller.documentation;

import dev.barbershop.api.availability.dto.ScheduleBlockCreateDTO;
import dev.barbershop.api.availability.dto.ScheduleBlockResponseDTO;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Agenda - Bloqueios", description = "Bloqueios pontuais da agenda do barbeiro autenticado")
@RequestMapping("/barbers/me/blocks")
public interface ScheduleBlockControllerDoc {

    @Operation(
            summary = "Listar meus bloqueios",
            description = "Lista os bloqueios no período informado (padrão: hoje + 30 dias; máximo 93 dias)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bloqueios retornados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Período inválido",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @GetMapping
    ResponseEntity<ApiResponseDTO<ScheduleBlockResponseDTO>> findMine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails customUser
    );

    @Operation(
            summary = "Criar bloqueio",
            description = """
                    Bloqueia um intervalo de uma data (horários no fuso da barbearia).
                    Omitindo startTime e endTime, o dia inteiro é bloqueado.
                    É recusado se houver agendamento ativo ou outro bloqueio sobreposto.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bloqueio criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou período no passado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Existe agendamento ou bloqueio no período",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @PostMapping
    ResponseEntity<ApiResponseDTO<ScheduleBlockResponseDTO>> create(
            @RequestBody @Valid ScheduleBlockCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUser
    );

    @Operation(summary = "Remover bloqueio", description = "Remove um bloqueio do barbeiro autenticado, liberando o período")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bloqueio removido com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Bloqueio não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponseDTO<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails customUser
    );
}

