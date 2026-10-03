package dev.barbershop.api.user.admin.dto;

public record UserFilterDTO(
        String name,
        String email,
        Boolean active
) {

}
