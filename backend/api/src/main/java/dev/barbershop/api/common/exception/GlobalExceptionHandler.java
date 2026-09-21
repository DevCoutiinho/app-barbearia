package dev.barbershop.api.common.exception;

import dev.barbershop.api.common.dto.FieldErrorDTO;
import dev.barbershop.api.common.dto.StandardErrorDTO;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public StandardErrorDTO handleResourceNotFound(ResourceNotFoundException exception) {
        return StandardErrorDTO.notFound(exception.getMessage());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public StandardErrorDTO handleResourceAlreadyExists(ResourceAlreadyExistsException exception) {
        return StandardErrorDTO.conflict(exception.getMessage());
    }

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
                "Erro de validação nos campos",
                fieldErrorDTOS
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardErrorDTO handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldErrorDTO> fieldErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorDTO(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        return new StandardErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Violação de restrição nos dados enviados",
                fieldErrors
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public StandardErrorDTO handleBadCredentials(BadCredentialsException exception) {
        return StandardErrorDTO.unauthorized("Credenciais inválidas: e-mail ou senha incorretos");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public StandardErrorDTO handleAccessDenied(AccessDeniedException exception) {
        return StandardErrorDTO.forbidden("Acesso negado: você não tem permissão para realizar esta ação");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public StandardErrorDTO handleAuthentication(AuthenticationException exception) {
        return StandardErrorDTO.unauthorized("Falha na autenticação: " + exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardErrorDTO handleIllegalArgument(RuntimeException exception) {
        return StandardErrorDTO.badRequest(exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardErrorDTO handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return StandardErrorDTO.badRequest("Corpo da requisição ausente ou mal formatado");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public StandardErrorDTO handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return StandardErrorDTO.of(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método HTTP " + exception.getMethod() + " não suportado para esta rota"
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public StandardErrorDTO handleNoResourceFound(NoResourceFoundException exception) {
        return StandardErrorDTO.notFound("Recurso não encontrado: " + exception.getResourcePath());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public StandardErrorDTO handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("Violação de integridade no banco de dados: {}", exception.getMessage());
        return StandardErrorDTO.conflict("Violação de integridade nos dados: registro conflitante ou dependência inválida");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public StandardErrorDTO handleGenericException(Exception exception) {
        log.error("Erro interno não tratado no servidor: ", exception);
        return StandardErrorDTO.internalServerError("Ocorreu um erro interno no servidor. Tente novamente mais tarde.");
    }
}
