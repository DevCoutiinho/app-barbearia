package dev.barbershop.api.barberservice.controller;

import dev.barbershop.api.barberservice.dto.BarberServiceCatalogDTO;
import dev.barbershop.api.barberservice.service.BarberServiceApplicationService;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Catálogo de Serviços", description = "Endpoints de consulta ao catálogo de serviços da barbearia")
@RestController
@RequestMapping("/services/catalog")
@RequiredArgsConstructor
public class ServiceCatalogController {
    private final BarberServiceApplicationService service;

    @Operation(
            summary = "Listar catálogo de serviços",
            description = "Retorna todos os serviços ativos da barbearia disponíveis no catálogo, acompanhados das informações dos respectivos barbeiros"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Catálogo de serviços retornado com sucesso"
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
    public ResponseEntity<ApiResponseDTO<BarberServiceCatalogDTO>> findAll() {
        List<BarberServiceCatalogDTO> services = service.findAllCatalog();

        ApiResponseDTO<BarberServiceCatalogDTO> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Catalogo de serviços retornados com sucesso",
                services
        );

        return ResponseEntity.ok(apiResponseDTO);
    }
}
