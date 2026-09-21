package dev.barbershop.api.auth.authentication.controller.documentation;

import dev.barbershop.api.auth.authentication.dto.GoogleLoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginResponseDTO;
import dev.barbershop.api.auth.authentication.dto.UserCreateDTO;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import dev.barbershop.api.user.dto.UserCreatedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Autenticação", description = "Endpoints de cadastro e login de usuários")
@RequestMapping("/auth")
@SecurityRequirements({})
public interface AuthControllerDoc {

    @Operation(
            summary = "Cadastrar usuário",
            description = "Cria um novo usuário na plataforma"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário criado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou erro de validação nos campos",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email já cadastrado",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PostMapping("register")
    ResponseEntity<ApiResponseDTO<UserCreatedDTO>> create(@RequestBody @Valid UserCreateDTO dto);

    @Operation(
            summary = "Login",
            description = "Realizar login no sistema com email e senha"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login realizado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de requisição inválidos ou mal formatados",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciais inválidas: e-mail ou senha incorretos",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PostMapping("login")
    ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@RequestBody @Valid LoginDTO dto);

    @Operation(
            summary = "Login com Google",
            description = "Realizar login ou cadastro automático utilizando o token do Google"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login realizado com sucesso via Google"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token do Google inválido ou sem e-mail",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Falha de autenticação com o Google",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno no servidor",
                    content = @Content(schema = @Schema(implementation = StandardErrorDTO.class))
            )
    })
    @PostMapping("login/google")
    ResponseEntity<ApiResponseDTO<LoginResponseDTO>> loginWithGoogle(@RequestBody @Valid GoogleLoginDTO dto);
}
