package com.esdc.gameapi;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application entry point.
 */
@SpringBootApplication
@RequiredArgsConstructor
public class BackendApplication {

  /**
   * Starts the Game API backend application.
   */
  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }

}
