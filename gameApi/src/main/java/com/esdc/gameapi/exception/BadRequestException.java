package com.esdc.gameapi.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for bad request errors.
 */
public class BadRequestException extends ApplicationException {
  /**
   * Creates exception with custom message.
   */
  public BadRequestException(String message) {
    super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
  }
}
