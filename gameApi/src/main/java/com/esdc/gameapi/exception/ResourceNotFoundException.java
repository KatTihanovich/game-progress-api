package com.esdc.gameapi.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for missing resources.
 */
public class ResourceNotFoundException extends ApplicationException {

  /**
   * Creates exception with resource, field and value details.
   */
  public ResourceNotFoundException(String resource, String field, Object value) {
    super(String.format("%s not found with %s: '%s'", resource, field, value),
        HttpStatus.NOT_FOUND,
        "RESOURCE_NOT_FOUND");
  }

  /**
   * Creates exception with custom message.
   */
  public ResourceNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
  }
}
