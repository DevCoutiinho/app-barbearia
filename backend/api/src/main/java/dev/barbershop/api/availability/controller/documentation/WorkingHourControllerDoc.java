package dev.barbershop.api.availability.controller.documentation;

import dev.barbershop.api.availability.dto.WeeklyScheduleDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Agenda - Grade semanal", description = "Horários de trabalho padrão do barbeiro autenticado")
@RequestMapping("/barbers/me/working-hours")
public interface WorkingHourControllerDoc {

    @Operation(
            summary = "Consultar minha grade semanal",
            description = "Retorna os 7 dias da semana. Dia com lista de intervalos vazia representa folga."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @GetMapping
    ResponseEntity<ApiResponseDTO<WeeklyScheduleDTO>> findMine(@AuthenticationPrincipal CustomUserDetails customUser);

    @Operation(
            summary = "Criar/definir minha grade semanal",
            description = """
                    Cadastra a grade de trabalho padrão do barbeiro.
                    Dias não informados (ou com lista vazia) são folga.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Grade criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dias repetidos, intervalos inválidos ou sobrepostos",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @PostMapping
    ResponseEntity<ApiResponseDTO<WeeklyScheduleDTO>> create(
            @RequestBody @Valid WeeklyScheduleDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUser
    );

    @Operation(
            summary = "Substituir minha grade semanal",
            description = """
                    Substitui a grade inteira. Dias não informados (ou com lista vazia) são folga.
                    Não altera, move ou cancela agendamentos existentes: a nova grade vale apenas para novos agendamentos.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dias repetidos, intervalos inválidos ou sobrepostos",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class)))
    })
    @PutMapping
    ResponseEntity<ApiResponseDTO<WeeklyScheduleDTO>> replaceMine(
            @RequestBody @Valid WeeklyScheduleDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUser
    );
}

