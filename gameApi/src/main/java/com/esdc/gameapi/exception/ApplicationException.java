package com.esdc.gameapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception class for application errors with HTTP status and error code.
 */
@Getter
public abstract class ApplicationException extends RuntimeException {

  private final HttpStatus status;
  private final String errorCode;

  /**
   * Creates exception with message, status and error code.
   */
  public ApplicationException(String message, HttpStatus status, String errorCode) {
    super(message);
    this.status = status;
    this.errorCode = errorCode;
  }

  /**
   * Creates exception with message, cause, status and error code.
   */
  public ApplicationException(
      String message, Throwable cause, HttpStatus status, String errorCode
  ) {
    super(message, cause);
    this.status = status;
    this.errorCode = errorCode;
  }

}
