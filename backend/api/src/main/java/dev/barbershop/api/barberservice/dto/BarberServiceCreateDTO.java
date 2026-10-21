package dev.barbershop.api.barberservice.dto;

import dev.barbershop.api.barberservice.enums.BarberServiceTechnique;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record BarberServiceCreateDTO(

        @NotBlank(message = "O nome do serviço é obrigatório")
        String name,

        @NotNull(message = "A técnica não pode ser nula")
        BarberServiceTechnique technique,

        @DecimalMin(value = "0.01", inclusive = true, message = "O preço deve ser maior ou igual a 0.01")
        @DecimalMax(value = "9999.99", inclusive = true, message = "O preço não pode ultrapassar 9999.99")
        BigDecimal price,

        @NotNull(message = "Você deve informar se aceita ou não os termos.")
        Boolean active,

        @NotNull(message = "O campo quantidade é obrigatório.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        Integer durationTime,

        @NotNull(message = "O ID é obrigatório.")
        UUID categoryId

) {
}
