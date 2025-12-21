package com.esdc.gameapi.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for unauthorized access attempts.
 */
public class UnauthorizedException extends ApplicationException {

  public UnauthorizedException(String message) {
    super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
  }

  public UnauthorizedException() {
    super("Unauthorized access", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
  }
}
