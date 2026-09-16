package dev.barbershop.api.auth.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginDTO(

        @NotBlank(message = "O email de usuário é obrigatório")
        @Email(message = "O email deve estar em um formato válido")
        String email,

        @NotBlank(message = "A senha de usuário é obrigatória")
        String password
) {
}
