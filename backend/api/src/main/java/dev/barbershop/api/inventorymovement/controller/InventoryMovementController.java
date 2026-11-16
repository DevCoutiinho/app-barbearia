package dev.barbershop.api.inventorymovement.controller;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.inventorymovement.dto.InventoryMovementEntryDTO;
import dev.barbershop.api.inventorymovement.dto.InventoryMovementOutputDTO;
import dev.barbershop.api.inventorymovement.dto.InventoryMovementResponseDTO;
import dev.barbershop.api.inventorymovement.service.InventoryMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Movimentações de Estoque", description = "Endpoints de gerenciamento e histórico de movimentações do estoque")
@RestController
@RequestMapping("/admin/stock/movements")
@RequiredArgsConstructor
public class InventoryMovementController {

    private final InventoryMovementService service;

    @Operation(
            summary = "Buscar histórico de movimentações por produto",
            description = "Retorna o histórico com todas as movimentações (entradas e saídas) registradas para um produto específico"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Movimentações retornadas com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador do produto em formato inválido",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado: requer permissão INVENTORY_READ",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponseDTO<InventoryMovementResponseDTO>> findByProduct(
            @Parameter(description = "Identificador único do produto", required = true)
            @PathVariable UUID productId
    ) {

        List<InventoryMovementResponseDTO> movements = service.findHistoryByProductId(productId);

        ApiResponseDTO<InventoryMovementResponseDTO> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.NO_CONTENT.value(),
                "Movimentações retornadas com sucesso",
                movements
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Operation(
            summary = "Registrar entrada de estoque",
            description = "Registra uma nova movimentação de entrada de quantidade para um determinado produto"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Movimentação de entrada realizada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou erro de validação nos campos",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado: requer permissão INVENTORY_CREATE",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário autenticado ou produto não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PostMapping("/product/{productId}/entry")
    public ResponseEntity<ApiResponseDTO<Void>> recordIncomingQuantity(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUser,
            @Parameter(description = "Identificador único do produto", required = true)
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryMovementEntryDTO dto
    ) {
        service.recordIncomingQuantity(customUser.getId(), productId, dto);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.NO_CONTENT.value(),
                "Movimentação de entrada realizada com sucesso"
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Operation(
            summary = "Registrar saída/consumo de estoque",
            description = "Registra uma movimentação de saída/consumo de quantidade de um produto por um usuário"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Movimentação de consumo realizada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos, erro de validação ou quantidade de consumo maior que a disponível no estoque",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado: requer permissão INVENTORY_CREATE",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário ou produto não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PostMapping("/product/{productId}/output")
    public ResponseEntity<ApiResponseDTO<Void>> recordOutputQuantity(
            @Parameter(description = "Identificador único do produto", required = true)
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryMovementOutputDTO dto
    ) {
        service.recordOutputQuantity(productId, dto);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.NO_CONTENT.value(),
                "Movimentação de consumo realizada com sucesso"
        );

        return ResponseEntity.ok(apiResponseDTO);
    }
}
