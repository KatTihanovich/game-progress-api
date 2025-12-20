package com.esdc.gameapi.exception;

import com.esdc.gameapi.domain.dto.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final int MAX_STACKTRACE_LENGTH = 2000;

  @Value("${app.debug:false}")
  private boolean debugMode;

  /**
   * Централизованная обработка всех кастомных исключений ApplicationException
   */
  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ErrorResponse> handleApplicationException(
      ApplicationException ex,
      HttpServletRequest request) {

    HttpStatus status = ex.getStatus();
    log.warn("Application exception [{}]: {} at {}",
        ex.getErrorCode(), ex.getMessage(), request.getRequestURI());

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(status.value())
        .error(status.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .trace(debugMode ? getStackTrace(ex) : null)
        .build();

    return ResponseEntity.status(status).body(error);
  }

  /**
   * Authentication exceptions (401)
   */
  @ExceptionHandler({
      BadCredentialsException.class,
      AuthenticationException.class
  })
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      Exception ex,
      HttpServletRequest request) {

    log.warn("Authentication failed: {} at {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse error = buildErrorResponse(
        HttpStatus.UNAUTHORIZED,
        "Invalid username or password",
        "AUTHENTICATION_FAILED",
        request.getRequestURI(),
        ex
    );

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  /**
   * JWT exceptions (401)
   */
  @ExceptionHandler({
      ExpiredJwtException.class,
      MalformedJwtException.class,
      SignatureException.class
  })
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ResponseEntity<ErrorResponse> handleJwtException(
      Exception ex,
      HttpServletRequest request) {

    log.warn("JWT error: {} at {}", ex.getMessage(), request.getRequestURI());

    String message = ex instanceof ExpiredJwtException ?
        "JWT token has expired" : "Invalid JWT token";
    String code = ex instanceof ExpiredJwtException ?
        "JWT_EXPIRED" : "JWT_INVALID";

    ErrorResponse error = buildErrorResponse(
        HttpStatus.UNAUTHORIZED,
        message,
        code,
        request.getRequestURI(),
        ex
    );

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  /**
   * Spring Security Access denied (403)
   */
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException ex,
      HttpServletRequest request) {

    log.warn("Access denied: {} at {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse error = buildErrorResponse(
        HttpStatus.FORBIDDEN,
        "Access denied",
        "ACCESS_DENIED",
        request.getRequestURI(),
        ex
    );

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  /**
   * Validation errors (400)
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex,
      HttpServletRequest request) {

    Map<String, String> validationErrors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            error -> error.getDefaultMessage() != null ?
                error.getDefaultMessage() : "Invalid value",
            (existing, replacement) -> existing
        ));

    log.warn("Validation failed at {}: {}", request.getRequestURI(), validationErrors);

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .message("Validation failed")
        .path(request.getRequestURI())
        .validationErrors(validationErrors)
        .trace(debugMode ? getStackTrace(ex) : null)
        .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * IllegalArgumentException (400)
   */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex,
      HttpServletRequest request) {

    log.warn("Illegal argument: {} at {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse error = buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        ex.getMessage(),
        "ILLEGAL_ARGUMENT",
        request.getRequestURI(),
        ex
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Malformed JSON (400)
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpServletRequest request) {

    log.warn("Malformed JSON request at {}: {}", request.getRequestURI(), ex.getMessage());

    ErrorResponse error = buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Malformed JSON request",
        "MALFORMED_JSON",
        request.getRequestURI(),
        ex
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Missing request parameters (400)
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleMissingParams(
      MissingServletRequestParameterException ex,
      HttpServletRequest request) {

    log.warn("Missing parameter: {} at {}", ex.getParameterName(), request.getRequestURI());

    String message = String.format("Required parameter '%s' is missing", ex.getParameterName());

    ErrorResponse error = buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        message,
        "MISSING_PARAMETER",
        request.getRequestURI(),
        ex
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Type mismatch (400)
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex,
      HttpServletRequest request) {

    log.warn("Type mismatch for parameter: {} at {}", ex.getName(), request.getRequestURI());

    String message = String.format("Invalid value for parameter '%s': expected type %s",
        ex.getName(),
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

    ErrorResponse error = buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        message,
        "TYPE_MISMATCH",
        request.getRequestURI(),
        ex
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Endpoint not found (404)
   */
  @ExceptionHandler(NoHandlerFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ErrorResponse> handleNoHandlerFound(
      NoHandlerFoundException ex,
      HttpServletRequest request) {

    log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());

    String message = String.format("No endpoint %s %s", ex.getHttpMethod(), ex.getRequestURL());

    ErrorResponse error = buildErrorResponse(
        HttpStatus.NOT_FOUND,
        message,
        "ENDPOINT_NOT_FOUND",
        request.getRequestURI(),
        ex
    );

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex,
      HttpServletRequest request) {

    log.warn("Method not supported: {} {}",
        ex.getMethod(), request.getRequestURI());

    String message = String.format(
        "Method %s is not supported for this endpoint",
        ex.getMethod()
    );

    ErrorResponse error = buildErrorResponse(
        HttpStatus.METHOD_NOT_ALLOWED,
        message,
        "METHOD_NOT_ALLOWED",
        request.getRequestURI(),
        ex
    );

    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
  }

  /**
   * All other exceptions (500)
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorResponse> handleGlobalException(
      Exception ex,
      HttpServletRequest request) {

    log.error("Unexpected error at {}: ", request.getRequestURI(), ex);

    ErrorResponse error = buildErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred",
        "INTERNAL_SERVER_ERROR",
        request.getRequestURI(),
        ex
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /**
   * Build error response
   */
  private ErrorResponse buildErrorResponse(
      HttpStatus status,
      String message,
      String code,
      String path,
      Exception ex) {

    return ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(status.value())
        .error(status.getReasonPhrase())
        .message(message)
        .path(path)
        .trace(debugMode ? getStackTrace(ex) : null)
        .build();
  }

  /**
   * Get stack trace for debug mode
   */
  private String getStackTrace(Exception ex) {
    if (ex == null) return null;

    StringBuilder sb = new StringBuilder(ex.toString()).append("\n");
    for (StackTraceElement element : ex.getStackTrace()) {
      sb.append("\tat ").append(element.toString()).append("\n");
      if (sb.length() > MAX_STACKTRACE_LENGTH) {
        sb.append("\t...");
        break;
      }
    }
    return sb.toString();
  }
}
