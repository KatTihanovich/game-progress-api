package com.esdc.gameapi.service;

import com.esdc.gameapi.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *Service to validate Admin Passowrd.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthService {

  @Value("${admin.password}")
  private String adminPassword;

  /**
   *Method to validate admin password.
   */
  public void validateAdminPassword(String password) {
    if (!adminPassword.equals(password)) {
      log.warn("Invalid admin password attempt");
      throw new UnauthorizedException("Invalid admin password");
    }
  }
}
