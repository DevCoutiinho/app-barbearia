package dev.barbershop.api.barberservice.controller.documentation;

import dev.barbershop.api.barberservice.dto.BarberServiceCreateDTO;
import dev.barbershop.api.barberservice.dto.BarberServiceFilterDTO;
import dev.barbershop.api.barberservice.dto.BarberServiceResponseDTO;
import dev.barbershop.api.barberservice.dto.BarberServiceUpdateDTO;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Serviços da Barbearia", description = "Endpoints de gerenciamento dos serviços prestados pela barbearia")
@RequestMapping("/barber-services")
public interface BarberServiceControllerDoc {

    @Operation(
            summary = "Buscar serviços da barbearia",
            description = "Busca todos os serviços cadastrados na barbearia"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Serviços retornados com sucesso"
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
    ResponseEntity<ApiResponseDTO<Page<BarberServiceResponseDTO>>> findAll(
            @ParameterObject
            @ModelAttribute BarberServiceFilterDTO dto,

            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = {"name", "technique"},
                    direction = Sort.Direction.ASC
            ) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails customUser
            );

    @Operation(
            summary = "Cadastrar serviço da barbearia",
            description = "Cadastra um novo serviço para a barbearia com dados e imagem de capa opcional"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Serviço criado com sucesso"
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
                    description = "Barbeiro ou categoria não encontrada",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE)
            )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponseDTO<Void>> create(
            @RequestPart("data") @Valid BarberServiceCreateDTO dto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails customUser
    );

    @Operation(
            summary = "Atualizar serviço da barbearia",
            description = "Atualiza os dados e imagem de um serviço existente da barbearia"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Serviço atualizado com sucesso"
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
                    description = "Serviço, barbeiro ou categoria não encontrada",
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
            @PathVariable UUID id,
            @RequestPart("data") @Valid BarberServiceUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUser
    );

    @Operation(
            summary = "Deletar serviço da barbearia",
            description = "Remove um serviço da barbearia pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Serviço excluído com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails customUser);

}
