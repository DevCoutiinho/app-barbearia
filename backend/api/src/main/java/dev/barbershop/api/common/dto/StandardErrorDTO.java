package dev.barbershop.api.common.dto;

import org.springframework.http.HttpStatus;

import java.util.List;

public record StandardErrorDTO(
        int status,
        String message,
        List<FieldErrorDTO> errors
) {

    public static StandardErrorDTO badRequest(String message){
        return new StandardErrorDTO(HttpStatus.BAD_REQUEST.value(), message, List.of());
    }

    public static StandardErrorDTO conflict(String message){
        return new  StandardErrorDTO(HttpStatus.CONFLICT.value(), message, List.of());
    }

    public static StandardErrorDTO notFound(String message){
        return  new StandardErrorDTO(HttpStatus.NOT_FOUND.value(), message, List.of());
    }

    public static StandardErrorDTO internalServerError(String message){
        return new StandardErrorDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, List.of());
    }
}
