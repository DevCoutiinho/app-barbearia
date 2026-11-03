package dev.barbershop.api.product.controller.documentation;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.product.dto.ProductCreateDTO;
import dev.barbershop.api.product.dto.ProductFilterDTO;
import dev.barbershop.api.product.dto.ProductUpdateDTO;
import dev.barbershop.api.product.projection.ProductProjection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "Produtos", description = "Endpoints de gerenciamento dos produtos")
@RequestMapping("/admin/stock/products")
public interface ProductControllerDoc {

    @Operation(
            summary = "Buscar produtos",
            description = "Busca todos os produtos cadastrados de forma paginada e com suporte a filtros"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produtos retornados com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros de requisição inválidos",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
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
    ResponseEntity<ApiResponseDTO<Page<ProductProjection>>> findAll(
            @ParameterObject
            @ModelAttribute ProductFilterDTO dto,

            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = {"name"},
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    );

    @Operation(
            summary = "Cadastrar produto",
            description = "Cadastra um novo produto no estoque"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto criado com sucesso"
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
                    description = "Usuário não encontrado para movimentação de estoque",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito de dados no cadastro do produto",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PostMapping
    ResponseEntity<ApiResponseDTO<Void>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUser,
            @Valid @RequestBody ProductCreateDTO dto
    );

    @Operation(
            summary = "Atualizar produto",
            description = "Atualiza os dados de um produto existente pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto atualizado com sucesso"
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
                    description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito nos dados do produto",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PutMapping("/{id}")
    ResponseEntity<ApiResponseDTO<Void>> update(
            @Parameter(description = "Identificador único do produto", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateDTO dto
    );

    @Operation(
            summary = "Desativar produto",
            description = "Desativa um produto do estoque pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto desativado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PatchMapping("/{id}/deactivate")
    ResponseEntity<ApiResponseDTO<Void>> deactivateProduct(
            @Parameter(description = "Identificador único do produto", required = true) @PathVariable UUID id
    );

    @Operation(
            summary = "Ativar produto",
            description = "Reativa um produto previamente desativado no estoque pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto ativado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PatchMapping("/{id}/activate")
    ResponseEntity<ApiResponseDTO<Void>> activateProduct(
            @Parameter(description = "Identificador único do produto", required = true) @PathVariable UUID id
    );
}
