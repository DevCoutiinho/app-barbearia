package dev.barbershop.api.auth.authentication.controller;

import dev.barbershop.api.auth.authentication.controller.documentation.AuthControllerDoc;
import dev.barbershop.api.auth.authentication.dto.GoogleLoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginResponseDTO;
import dev.barbershop.api.auth.authentication.service.AuthService;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.auth.authentication.dto.UserCreateDTO;
import dev.barbershop.api.user.dto.UserCreatedDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerDoc {

    private final AuthService authService;

    @Override
    public ResponseEntity<ApiResponseDTO<UserCreatedDTO>> create(@RequestBody @Valid UserCreateDTO dto) {

        UserCreatedDTO user = authService.register(dto);

        ApiResponseDTO<UserCreatedDTO> responseDTO = new ApiResponseDTO<>(
                HttpStatus.CREATED.value(),
                "Usuário criado com sucesso",
                List.of(user)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@RequestBody @Valid LoginDTO dto) {
        LoginResponseDTO loginResponseDTO = authService.login(dto);

        ApiResponseDTO<LoginResponseDTO> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(), "Login realizado com sucesso", List.of(loginResponseDTO)
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> loginWithGoogle(@RequestBody @Valid GoogleLoginDTO dto) {

        LoginResponseDTO loginResponseDTO = authService.loginWithGoogle(dto);

        ApiResponseDTO<LoginResponseDTO> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(), "Login realizado com sucesso", List.of(loginResponseDTO)
        );

        return ResponseEntity.ok(apiResponseDTO);
    }


}
