package dev.barbershop.api.auth.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateDTO(
        @NotBlank(message = "O nome de usuário é obrigatório")
        @Size(max = 100, message = "A quantidade máxima permitida é 100 caracteres")
        String name,

        @NotBlank(message = "O email de usuário é obrigatório")
        @Email(message = "O email deve estar em um formato válido")
        String email,

        @NotBlank(message = "A senha de usuário é obrigatória")
        String password
) {
}
