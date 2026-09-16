package dev.barbershop.api.common.dto;

import org.springframework.http.HttpStatus;

import java.util.List;

public record ApiResponseDTO<T>(
        int status,
        String message,
        List<T> data
) {

    public ApiResponseDTO(int status, String message) {
        this(status, message, null);
    }
}
