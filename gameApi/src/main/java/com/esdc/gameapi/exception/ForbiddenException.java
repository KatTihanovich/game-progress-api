package com.esdc.gameapi.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApplicationException {

  public ForbiddenException(String message) {
    super(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
  }

  public ForbiddenException() {
    super("Access forbidden", HttpStatus.FORBIDDEN, "FORBIDDEN");
  }
}
