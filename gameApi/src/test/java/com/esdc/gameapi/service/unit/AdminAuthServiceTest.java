package com.esdc.gameapi.service.unit;

import com.esdc.gameapi.exception.UnauthorizedException;
import com.esdc.gameapi.service.AdminAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("AdminAuthService Unit Tests")
class AdminAuthServiceTest {

  private static final String TEST_ADMIN_PASSWORD = "test-admin-password";

  private AdminAuthService adminAuthService;

  @BeforeEach
  void setUp() {
    adminAuthService = new AdminAuthService();
    ReflectionTestUtils.setField(adminAuthService, "adminPassword", TEST_ADMIN_PASSWORD);
  }

  @Test
  @DisplayName("Should pass validation for correct admin password")
  void shouldPassValidationForCorrectPassword() {
    assertThatCode(() -> adminAuthService.validateAdminPassword(TEST_ADMIN_PASSWORD))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should throw UnauthorizedException for wrong admin password")
  void shouldThrowUnauthorizedForWrongPassword() {
    assertThatThrownBy(() -> adminAuthService.validateAdminPassword("wrong-password"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessage("Invalid admin password");
  }

  @Test
  @DisplayName("Should throw UnauthorizedException for null password")
  void shouldThrowUnauthorizedForNullPassword() {
    assertThatThrownBy(() -> adminAuthService.validateAdminPassword(null))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessage("Invalid admin password");
  }

  @Test
  @DisplayName("Should throw UnauthorizedException for empty password")
  void shouldThrowUnauthorizedForEmptyPassword() {
    assertThatThrownBy(() -> adminAuthService.validateAdminPassword(""))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessage("Invalid admin password");
  }
}
