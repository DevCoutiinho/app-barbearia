package dev.barbershop.api.barberservice.dto.servicecategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceCategoryCreateDTO(

        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(max = 100, message = "Máximo de 100 caracteres permitidos")
        String name,

        String description,

        boolean active
) {

    public ServiceCategoryCreateDTO(String name, String description){
        this(name, description, true);
    }
}
