package by.taverna.shlyapnika.control.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiError> badRequest(IllegalArgumentException error) {
    return ResponseEntity.badRequest().body(ApiError.of(400, "bad_request", error.getMessage()));
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  ResponseEntity<ApiError> validation(Exception error) {
    return ResponseEntity.badRequest().body(ApiError.of(400, "validation_failed", "Check form fields."));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiError> denied(AccessDeniedException error) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiError.of(403, "forbidden", "Not enough permissions for this operation."));
  }
}
