package by.taverna.shlyapnika.common;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ConsentRequiredException.class)
  ResponseEntity<ApiError> consent(ConsentRequiredException error, HttpServletRequest request) {
    return error(HttpStatus.UNPROCESSABLE_ENTITY, ConsentRequiredException.CODE, error.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<ApiError> notFound(NotFoundException error, HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, error.code(), error.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiError> badRequest(IllegalArgumentException error, HttpServletRequest request) {
    return error(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", error.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException error, HttpServletRequest request) {
    var message = error.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(FieldError::getDefaultMessage)
        .orElse("Проверьте данные.");
    return error(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message, request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> generic(Exception error, HttpServletRequest request) {
    log.error("Unhandled request error", error);
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Сервер временно недоступен. Попробуйте позже.",
        request.getRequestURI()
    );
  }

  private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, String path) {
    return ResponseEntity.status(status).body(new ApiError(code, message, Instant.now(), path));
  }
}
