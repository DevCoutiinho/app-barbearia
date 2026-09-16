package dev.barbershop.api.auth.authentication.controller.documentation;

import dev.barbershop.api.auth.authentication.dto.GoogleLoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginResponseDTO;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.auth.authentication.dto.UserCreateDTO;
import dev.barbershop.api.user.dto.UserCreatedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/auth")
@SecurityRequirements({})
public interface AuthControllerDoc {

    @Operation(
            summary = "Cadastrar usuário",
            description = "Cria um novo usuário"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso")
    })
    @PostMapping("register")
    ResponseEntity<ApiResponseDTO<UserCreatedDTO>> create(@RequestBody @Valid UserCreateDTO dto);

    @Operation(
            summary = "Login",
            description = "Realizar login no sistema"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso")
    })
    @PostMapping("login")
    ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@RequestBody @Valid LoginDTO dto);

    @PostMapping("login/google")
    ResponseEntity<ApiResponseDTO<LoginResponseDTO>> loginWithGoogle(@RequestBody @Valid GoogleLoginDTO dto);
}
