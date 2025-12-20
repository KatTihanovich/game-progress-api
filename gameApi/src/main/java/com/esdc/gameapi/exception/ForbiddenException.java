package com.esdc.gameapi.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for forbidden access attempts.
 */
public class ForbiddenException extends ApplicationException {

  /**
   * Creates exception with custom message.
   */
  public ForbiddenException(String message) {
    super(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
  }

  /**
   * Creates exception with default message.
   */
  public ForbiddenException() {
    super("Access forbidden", HttpStatus.FORBIDDEN, "FORBIDDEN");
  }
}
