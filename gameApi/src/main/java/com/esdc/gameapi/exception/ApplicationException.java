package com.esdc.gameapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApplicationException extends RuntimeException {

  private final HttpStatus status;
  private final String errorCode;

  public ApplicationException(String message, HttpStatus status, String errorCode) {
    super(message);
    this.status = status;
    this.errorCode = errorCode;
  }

  public ApplicationException(String message, Throwable cause, HttpStatus status, String errorCode) {
    super(message, cause);
    this.status = status;
    this.errorCode = errorCode;
  }

}
