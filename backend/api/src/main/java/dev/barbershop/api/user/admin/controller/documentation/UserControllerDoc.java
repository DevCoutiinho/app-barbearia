package dev.barbershop.api.user.admin.controller.documentation;


import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.user.admin.dto.UserAdminResponseDTO;
import dev.barbershop.api.user.admin.dto.UserFilterDTO;
import dev.barbershop.api.user.admin.dto.UserUpdateRolesDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Usuarios", description = "Endpoints de gerenciamento dos usuários")
@RequestMapping("/admin/users")
public interface UserControllerDoc {

    @Operation(
            summary = "Buscar usuários",
            description = "Busca todos os usuários com ou sem filtros"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuários retornados com sucesso"
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
    ResponseEntity<ApiResponseDTO<Page<UserAdminResponseDTO>>> findAll(
            @ParameterObject
            @ModelAttribute UserFilterDTO dto,

            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "name,email",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    );

    @Operation(
            summary = "Atualizar permissões do usuário",
            description = "Atualiza as roles (permissões) de um usuário específico"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Permissões atualizadas com sucesso"
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
                    description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PutMapping("/{id}")
    ResponseEntity<ApiResponseDTO<Void>> updateRoles(@PathVariable UUID id, @RequestBody UserUpdateRolesDTO dto);

    @Operation(
            summary = "Deletar usuário",
            description = "Remove um usuário do sistema pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuário removido com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
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
