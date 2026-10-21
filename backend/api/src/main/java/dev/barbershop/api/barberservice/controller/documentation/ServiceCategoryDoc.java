package dev.barbershop.api.barberservice.controller.documentation;

import dev.barbershop.api.barberservice.dto.servicecategory.ServiceCategoryCreateDTO;
import dev.barbershop.api.barberservice.projection.ServiceCategorySummaryProjection;
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

@Tag(name = "Categorias de Serviços", description = "Endpoints de gerenciamento das categorias de serviços")
@RequestMapping("/service-categories")
public interface ServiceCategoryDoc {

    @Operation(
            summary = "Buscar categorias de serviços",
            description = "Busca todas as categorias de serviços cadastradas no sistema"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Categorias retornadas com sucesso"
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
    ResponseEntity<ApiResponseDTO<ServiceCategorySummaryProjection>> findAll();

    @Operation(
            summary = "Cadastrar categoria de serviço",
            description = "Cadastra uma nova categoria de serviço no sistema"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Categoria criada com sucesso"
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
                    description = "Já existe uma categoria com esse nome",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PostMapping
    ResponseEntity<ApiResponseDTO<Void>> create(@Valid @RequestBody ServiceCategoryCreateDTO dto);

    @Operation(
            summary = "Atualizar categoria de serviço",
            description = "Atualiza os dados de uma categoria de serviço existente pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Categoria atualizada com sucesso"
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
                    description = "Categoria não encontrada",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Já existe uma categoria com esse nome",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PutMapping("/{id}")
    ResponseEntity<ApiResponseDTO<Void>> update(@PathVariable UUID id, @Valid @RequestBody ServiceCategoryCreateDTO dto);

    @Operation(
            summary = "Deletar categoria de serviço",
            description = "Remove uma categoria de serviço do sistema pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Categoria excluída com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Categoria não encontrada",
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
