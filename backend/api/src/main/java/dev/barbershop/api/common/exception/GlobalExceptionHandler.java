package dev.barbershop.api.common.exception;

import dev.barbershop.api.common.dto.FieldErrorDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardErrorDTO handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {

        List<FieldError> errors = exception.getFieldErrors();

        List<FieldErrorDTO> fieldErrorDTOS = errors
                .stream()
                .map(e -> new FieldErrorDTO(
                        e.getField(),
                        e.getDefaultMessage())
                )
                .toList();

        return new StandardErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "",
                fieldErrorDTOS
        );
    }
}
