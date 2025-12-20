package com.esdc.gameapi.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for duplicate resource conflicts.
 */
public class DuplicateResourceException extends ApplicationException {

  /**
   * Creates exception with resource, field and value details.
   */
  public DuplicateResourceException(String resource, String field, Object value) {
    super(String.format("%s already exists with %s: '%s'", resource, field, value),
        HttpStatus.CONFLICT,
        "DUPLICATE_RESOURCE");
  }

  /**
   * Creates exception with custom message.
   */
  public DuplicateResourceException(String message) {
    super(message, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
  }
}
