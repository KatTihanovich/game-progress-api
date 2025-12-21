package com.esdc.gameapi.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("JWT Util Tests")
class JwtUtilTest {

  private JwtUtil jwtUtil;

  private static final String TEST_SECRET = "mySecretKeyForTestingPurposesOnlyMustBeAtLeast256BitsLong";
  private static final Long TEST_EXPIRATION = 3600000L; // 1 hour

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
    ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should generate valid JWT token")
  void shouldGenerateValidToken() {
    // Arrange
    String nickname = "testUser";
    Long userId = 1L;

    // Act
    String token = jwtUtil.generateToken(nickname, userId);

    // Assert
    assertThat(token).isNotNull();
    assertThat(token).isNotEmpty();
    assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
  }

  @Test
  @Tag("unit")
  @DisplayName("Should extract username from token")
  void shouldExtractUsername() {
    // Arrange
    String nickname = "testUser";
    Long userId = 1L;
    String token = jwtUtil.generateToken(nickname, userId);

    // Act
    String extractedUsername = jwtUtil.extractUsername(token);

    // Assert
    assertThat(extractedUsername).isEqualTo(nickname);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should extract expiration date from token")
  void shouldExtractExpiration() {
    // Arrange
    String nickname = "testUser";
    Long userId = 1L;
    String token = jwtUtil.generateToken(nickname, userId);

    // Act
    Date expiration = jwtUtil.extractExpiration(token);

    // Assert
    assertThat(expiration).isNotNull();
    assertThat(expiration).isAfter(new Date());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should validate correct token")
  void shouldValidateCorrectToken() {
    // Arrange
    String nickname = "testUser";
    Long userId = 1L;
    String token = jwtUtil.generateToken(nickname, userId);

    // Act
    Boolean isValid = jwtUtil.validateToken(token, nickname);

    // Assert
    assertThat(isValid).isTrue();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should reject token with wrong username")
  void shouldRejectTokenWithWrongUsername() {
    // Arrange
    String nickname = "testUser";
    Long userId = 1L;
    String token = jwtUtil.generateToken(nickname, userId);

    // Act
    Boolean isValid = jwtUtil.validateToken(token, "wrongUser");

    // Assert
    assertThat(isValid).isFalse();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should reject invalid token format")
  void shouldRejectInvalidToken() {
    // Arrange
    String invalidToken = "invalid.token.format";

    // Act
    Boolean isValid = jwtUtil.validateToken(invalidToken, "testUser");

    // Assert
    assertThat(isValid).isFalse();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should reject expired token")
  void shouldRejectExpiredToken() throws InterruptedException {
    // Arrange
    JwtUtil shortLivedJwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(shortLivedJwtUtil, "secret", TEST_SECRET);
    ReflectionTestUtils.setField(shortLivedJwtUtil, "expiration", 100L); // 100ms

    String nickname = "testUser";
    Long userId = 1L;
    String token = shortLivedJwtUtil.generateToken(nickname, userId);

    // Act
    Thread.sleep(150); // Wait for token to expire
    Boolean isValid = shortLivedJwtUtil.validateToken(token, nickname);

    // Assert
    assertThat(isValid).isFalse();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should include userId in token claims")
  void shouldIncludeUserIdInClaims() {
    // Arrange
    String nickname = "testUser";
    Long userId = 123L;
    String token = jwtUtil.generateToken(nickname, userId);

    // Act
    Integer extractedUserId = jwtUtil.extractClaim(token, claims -> claims.get("userId", Integer.class));

    // Assert
    assertThat(extractedUserId).isEqualTo(123);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should generate different tokens for different users")
  void shouldGenerateDifferentTokens() {
    // Arrange & Act
    String token1 = jwtUtil.generateToken("user1", 1L);
    String token2 = jwtUtil.generateToken("user2", 2L);

    // Assert
    assertThat(token1).isNotEqualTo(token2);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle null token gracefully")
  void shouldHandleNullToken() {
    // Act
    Boolean isValid = jwtUtil.validateToken(null, "testUser");

    // Assert
    assertThat(isValid).isFalse();
  }
}
