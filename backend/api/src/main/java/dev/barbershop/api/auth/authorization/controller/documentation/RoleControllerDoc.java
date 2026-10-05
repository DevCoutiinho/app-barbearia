package dev.barbershop.api.auth.authorization.controller.documentation;

import dev.barbershop.api.auth.authorization.dto.RoleCreateDTO;
import dev.barbershop.api.auth.authorization.dto.RoleResponseDTO;
import dev.barbershop.api.auth.authorization.dto.RoleUpdateDTO;
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

@Tag(name = "Roles", description = "Endpoints de gerenciamento das roles do sistema")
@RequestMapping("/admin/roles")
public interface RoleControllerDoc {

    @Operation(
            summary = "Buscar roles",
            description = "Busca todas as roles do sistema"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Roles retornadas com sucesso"
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
    ResponseEntity<ApiResponseDTO<RoleResponseDTO>> findAll();

    @Operation(
            summary = "Cadastrar role",
            description = "Cadastra uma nova role no sistema vinculando suas permissões"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Role criada com sucesso"
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
                    responseCode = "404",
                    description = "Uma ou mais permissões não foram encontradas",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Já existe uma role com esse nome",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PostMapping
    ResponseEntity<ApiResponseDTO<Void>> create(@Valid @RequestBody RoleCreateDTO dto);


    @Operation(
            summary = "Atualizar role",
            description = "Atualiza a descrição e as permissões de uma role existente pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Role atualizada com sucesso"
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
                    responseCode = "404",
                    description = "Role ou permissão não encontrada",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PutMapping("/{id}")
    ResponseEntity<ApiResponseDTO<Void>> update(@Valid @RequestBody RoleUpdateDTO dto, @PathVariable UUID id);

    @Operation(
            summary = "Deletar role",
            description = "Remove uma role do sistema pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Role excluída com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Role não encontrada",
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
