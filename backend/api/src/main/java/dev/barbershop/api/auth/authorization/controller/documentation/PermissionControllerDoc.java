package dev.barbershop.api.auth.authorization.controller.documentation;

import dev.barbershop.api.auth.authorization.dto.PermissionCreateDTO;
import dev.barbershop.api.auth.authorization.dto.PermissionSummaryDTO;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Permissões", description = "Endpoints de gerenciamento das permissões do sistema")
@RequestMapping("/admin/permissions")
public interface PermissionControllerDoc {

    @Operation(
            summary = "Buscar permissões",
            description = "Busca todas as permissões do sistema"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Permissões retornadas com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @GetMapping
    ResponseEntity<ApiResponseDTO<PermissionSummaryDTO>> findAll();

    @Operation(
            summary = "Buscar permissão por ID",
            description = "Busca os detalhes de uma permissão específica pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Permissão retornada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Permissão não encontrada",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @GetMapping("/{id}")
    ResponseEntity<ApiResponseDTO<PermissionSummaryDTO>> findById(@PathVariable UUID id);

    @Operation(
            summary = "Cadastrar permissão",
            description = "Cadastra uma nova permissão no sistema"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Permissão criada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou erro de validação nos campos",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Já existe uma permissão com esse nome",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PostMapping
    ResponseEntity<ApiResponseDTO<Void>> create(@Valid @RequestBody PermissionCreateDTO dto);

    @Operation(
            summary = "Deletar permissão",
            description = "Remove uma permissão do sistema pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Permissão excluída com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Permissão não encontrada",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable UUID id);
}
