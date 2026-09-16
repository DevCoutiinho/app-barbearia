package dev.barbershop.api.auth.authentication.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginDTO(

        @NotBlank(message = "O Token não foi enviado ou está vazio")
        String idToken
) {
}
