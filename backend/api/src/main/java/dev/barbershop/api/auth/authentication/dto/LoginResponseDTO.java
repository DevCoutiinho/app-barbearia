package dev.barbershop.api.auth.authentication.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record LoginResponseDTO(
        String accessToken,
        @JsonIgnore String refreshToken
) {
}
