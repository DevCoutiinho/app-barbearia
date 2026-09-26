package dev.barbershop.api.auth.authentication.exception;

public class MissingRefreshTokenException extends RuntimeException {
  public MissingRefreshTokenException(String message) {
    super(message);
  }
}
