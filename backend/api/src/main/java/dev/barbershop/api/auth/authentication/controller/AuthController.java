package dev.barbershop.api.auth.authentication.controller;

import dev.barbershop.api.auth.authentication.controller.documentation.AuthControllerDoc;
import dev.barbershop.api.auth.authentication.dto.GoogleLoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginDTO;
import dev.barbershop.api.auth.authentication.dto.LoginResponseDTO;
import dev.barbershop.api.auth.authentication.exception.MissingRefreshTokenException;
import dev.barbershop.api.auth.authentication.service.AuthService;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.auth.authentication.dto.UserCreateDTO;
import dev.barbershop.api.user.dto.UserCreatedDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerDoc {

    private final AuthService authService;
    private static final int MAX_AGE = 7 * 24 * 60 * 60;

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
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request) {
        LoginResponseDTO loginResponseDTO = authService.login(dto, request);

        ApiResponseDTO<LoginResponseDTO> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(), "Login realizado com sucesso", List.of(loginResponseDTO)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie(loginResponseDTO.refreshToken(), MAX_AGE).toString())
                .body(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> loginWithGoogle(@RequestBody @Valid GoogleLoginDTO dto, HttpServletRequest request) {

        LoginResponseDTO loginResponseDTO = authService.loginWithGoogle(dto, request);

        ApiResponseDTO<LoginResponseDTO> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Login realizado com sucesso",
                List.of(loginResponseDTO)
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        createCookie(loginResponseDTO.refreshToken(), MAX_AGE).toString()
                )
                .body(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> refreshToken(
            @CookieValue(value = "refreshToken", required = false)
            String refreshToken, HttpServletRequest request
    ) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new MissingRefreshTokenException("O token de atualização não foi enviado");
        }

        LoginResponseDTO loginResponseDTO = authService.refreshToken(refreshToken, request);

        ApiResponseDTO<LoginResponseDTO> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(), "Token atualizado com sucesso", List.of(loginResponseDTO)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie(loginResponseDTO.refreshToken(), MAX_AGE).toString())
                .body(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> logout(
            @CookieValue(value = "refreshToken", required = false)
            String refreshToken
    ) {

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(), "Logout realizado com sucesso", List.of()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie("", 0).toString())
                .body(apiResponseDTO);
    }

    private ResponseCookie createCookie(String token, long maxAge) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }
}
