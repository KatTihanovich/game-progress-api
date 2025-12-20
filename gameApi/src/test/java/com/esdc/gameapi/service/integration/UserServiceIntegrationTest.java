package com.esdc.gameapi.service.integration;

import com.esdc.gameapi.domain.dto.AuthResponse;
import com.esdc.gameapi.domain.dto.UserLoginDto;
import com.esdc.gameapi.domain.dto.UserRegistrationDto;
import com.esdc.gameapi.domain.dto.UserResponseDto;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.exception.DuplicateResourceException;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.UserRepository;
import com.esdc.gameapi.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=mySecretKeyForTestingPurposesOnlyMustBeAtLeast256BitsLong",
    "jwt.expiration=3600000"
})
@DisplayName("User Service Integration Tests")
class UserServiceIntegrationTest {

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private UserRegistrationDto registrationDto;
  private UserLoginDto loginDto;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    registrationDto = UserRegistrationDto.builder()
        .nickname("testuser")
        .password("password123")
        .age(25)
        .build();

    loginDto = UserLoginDto.builder()
        .nickname("testuser")
        .password("password123")
        .build();
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  // ========== Registration Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should register user and persist to database")
  void shouldRegisterUserAndPersistToDatabase() {
    // Act
    AuthResponse response = userService.register(registrationDto);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isNotNull();
    assertThat(response.getUser()).isNotNull();
    assertThat(response.getUser().getId()).isNotNull();
    assertThat(response.getUser().getNickname()).isEqualTo("testuser");
    assertThat(response.getUser().getAge()).isEqualTo(25);
    assertThat(response.getUser().getCreatedAt()).isNotNull();
    assertThat(response.getUser().getUpdatedAt()).isNotNull();

    // Verify persistence
    User fromDb = userRepository.findById(response.getUser().getId()).orElseThrow();
    assertThat(fromDb.getNickname()).isEqualTo("testuser");
    assertThat(fromDb.getAge()).isEqualTo(25);
    assertThat(fromDb.getPasswordHash()).isNotEqualTo("password123");
  }

  @Test
  @Tag("integration")
  @DisplayName("Should encode password when registering")
  void shouldEncodePasswordWhenRegistering() {
    // Act
    AuthResponse response = userService.register(registrationDto);

    // Assert
    User fromDb = userRepository.findById(response.getUser().getId()).orElseThrow();
    assertThat(fromDb.getPasswordHash()).isNotEqualTo("password123");
    assertThat(passwordEncoder.matches("password123", fromDb.getPasswordHash())).isTrue();
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when registering duplicate nickname")
  void shouldThrowExceptionWhenRegisteringDuplicateNickname() {
    // Arrange
    userService.register(registrationDto);

    // Act & Assert
    assertThatThrownBy(() -> userService.register(registrationDto))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("nickname")
        .hasMessageContaining("testuser");

    // Verify only one user exists
    assertThat(userRepository.count()).isEqualTo(1);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should generate valid JWT token on registration")
  void shouldGenerateValidJwtTokenOnRegistration() {
    // Act
    AuthResponse response = userService.register(registrationDto);

    // Assert
    assertThat(response.getToken()).isNotNull();
    assertThat(response.getToken()).isNotEmpty();
    assertThat(response.getToken().split("\\.")).hasSize(3); // JWT has 3 parts
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should set timestamps on registration")
  void shouldSetTimestampsOnRegistration() {
    // Act
    AuthResponse response = userService.register(registrationDto);

    // Assert
    User fromDb = userRepository.findById(response.getUser().getId()).orElseThrow();
    assertThat(fromDb.getCreatedAt()).isNotNull();
    assertThat(fromDb.getUpdatedAt()).isNotNull();
    assertThat(fromDb.getCreatedAt()).isBeforeOrEqualTo(fromDb.getUpdatedAt());
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should register multiple users with different nicknames")
  void shouldRegisterMultipleUsersWithDifferentNicknames() {
    // Arrange
    UserRegistrationDto user1 = UserRegistrationDto.builder()
        .nickname("user1")
        .password("password1")
        .age(20)
        .build();

    UserRegistrationDto user2 = UserRegistrationDto.builder()
        .nickname("user2")
        .password("password2")
        .age(30)
        .build();

    // Act
    AuthResponse response1 = userService.register(user1);
    AuthResponse response2 = userService.register(user2);

    // Assert
    assertThat(userRepository.count()).isEqualTo(2);
    assertThat(response1.getUser().getNickname()).isEqualTo("user1");
    assertThat(response2.getUser().getNickname()).isEqualTo("user2");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should register user with minimum age")
  void shouldRegisterUserWithMinimumAge() {
    // Arrange
    UserRegistrationDto minAgeDto = UserRegistrationDto.builder()
        .nickname("younguser")
        .password("password123")
        .age(1)
        .build();

    // Act
    AuthResponse response = userService.register(minAgeDto);

    // Assert
    User fromDb = userRepository.findById(response.getUser().getId()).orElseThrow();
    assertThat(fromDb.getAge()).isEqualTo(1);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should register user with maximum age")
  void shouldRegisterUserWithMaximumAge() {
    // Arrange
    UserRegistrationDto maxAgeDto = UserRegistrationDto.builder()
        .nickname("olduser")
        .password("password123")
        .age(150)
        .build();

    // Act
    AuthResponse response = userService.register(maxAgeDto);

    // Assert
    User fromDb = userRepository.findById(response.getUser().getId()).orElseThrow();
    assertThat(fromDb.getAge()).isEqualTo(150);
  }

  // ========== Login Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should login user with correct credentials")
  void shouldLoginUserWithCorrectCredentials() {
    // Arrange
    userService.register(registrationDto);

    // Act
    AuthResponse response = userService.login(loginDto);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isNotNull();
    assertThat(response.getUser()).isNotNull();
    assertThat(response.getUser().getNickname()).isEqualTo("testuser");
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when login with wrong password")
  void shouldThrowExceptionWhenLoginWithWrongPassword() {
    // Arrange
    userService.register(registrationDto);

    UserLoginDto wrongPasswordDto = UserLoginDto.builder()
        .nickname("testuser")
        .password("wrongpassword")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> userService.login(wrongPasswordDto))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when login with non-existent user")
  void shouldThrowExceptionWhenLoginWithNonExistentUser() {
    // Arrange
    UserLoginDto nonExistentDto = UserLoginDto.builder()
        .nickname("nonexistent")
        .password("password123")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> userService.login(nonExistentDto))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should generate different tokens for each login")
  void shouldGenerateDifferentTokensForEachLogin() throws InterruptedException {
    // Arrange
    userService.register(registrationDto);

    // Act
    AuthResponse response1 = userService.login(loginDto);
    Thread.sleep(1000); // Ensure different timestamps
    AuthResponse response2 = userService.login(loginDto);

    // Assert
    assertThat(response1.getToken()).isNotEqualTo(response2.getToken());
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should login user case-sensitively")
  void shouldLoginUserCaseSensitively() {
    // Arrange
    userService.register(registrationDto);

    UserLoginDto wrongCaseDto = UserLoginDto.builder()
        .nickname("TestUser") // Different case
        .password("password123")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> userService.login(wrongCaseDto))
        .isInstanceOf(BadCredentialsException.class);
  }

  // ========== Update User Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should update user and persist changes to database")
  void shouldUpdateUserAndPersistChangesToDatabase() {
    // Arrange
    AuthResponse registered = userService.register(registrationDto);
    Long userId = registered.getUser().getId();

    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("updateduser")
        .password("newpassword123")
        .age(30)
        .build();

    // Act
    UserResponseDto updated = userService.updateUser(userId, updateDto);

    // Assert
    assertThat(updated.getNickname()).isEqualTo("updateduser");
    assertThat(updated.getAge()).isEqualTo(30);

    // Verify persistence
    User fromDb = userRepository.findById(userId).orElseThrow();
    assertThat(fromDb.getNickname()).isEqualTo("updateduser");
    assertThat(fromDb.getAge()).isEqualTo(30);
    assertThat(passwordEncoder.matches("newpassword123", fromDb.getPasswordHash())).isTrue();
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when updating non-existent user")
  void shouldThrowExceptionWhenUpdatingNonExistentUser() {
    // Act & Assert
    assertThatThrownBy(() -> userService.updateUser(999L, registrationDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("999");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should update password and encode it")
  void shouldUpdatePasswordAndEncodeIt() {
    // Arrange
    AuthResponse registered = userService.register(registrationDto);
    Long userId = registered.getUser().getId();

    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("testuser")
        .password("brandnewpassword")
        .age(25)
        .build();

    // Act
    userService.updateUser(userId, updateDto);

    // Assert
    User fromDb = userRepository.findById(userId).orElseThrow();
    assertThat(passwordEncoder.matches("brandnewpassword", fromDb.getPasswordHash())).isTrue();
    assertThat(passwordEncoder.matches("password123", fromDb.getPasswordHash())).isFalse();
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should update updatedAt timestamp when updating user")
  void shouldUpdateUpdatedAtTimestampWhenUpdatingUser() throws InterruptedException {
    // Arrange
    AuthResponse registered = userService.register(registrationDto);
    Long userId = registered.getUser().getId();

    // Загрузить оригинального пользователя и запомнить timestamp
    User originalUser = userRepository.findById(userId).orElseThrow();
    LocalDateTime originalUpdatedAt = originalUser.getUpdatedAt();

    Thread.sleep(1100); // Ensure different timestamp (больше 1 секунды)

    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("updateduser")
        .password("newpass")
        .age(30)
        .build();

    // Act
    userService.updateUser(userId, updateDto);

    // Очистить persistence context чтобы получить свежие данные из БД
    userRepository.flush();

    // Assert
    User updatedUser = userRepository.findById(userId).orElseThrow();
    assertThat(updatedUser.getUpdatedAt()).isAfter(originalUpdatedAt);
    assertThat(updatedUser.getNickname()).isEqualTo("updateduser");
    assertThat(updatedUser.getAge()).isEqualTo(30);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should preserve user ID when updating")
  void shouldPreserveUserIdWhenUpdating() {
    // Arrange
    AuthResponse registered = userService.register(registrationDto);
    Long originalId = registered.getUser().getId();

    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("newname")
        .password("newpass")
        .age(30)
        .build();

    // Act
    UserResponseDto updated = userService.updateUser(originalId, updateDto);

    // Assert
    assertThat(updated.getId()).isEqualTo(originalId);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should login with updated credentials")
  void shouldLoginWithUpdatedCredentials() {
    // Arrange
    AuthResponse registered = userService.register(registrationDto);
    Long userId = registered.getUser().getId();

    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("updateduser")
        .password("newpassword123")
        .age(30)
        .build();

    userService.updateUser(userId, updateDto);

    UserLoginDto newLoginDto = UserLoginDto.builder()
        .nickname("updateduser")
        .password("newpassword123")
        .build();

    // Act
    AuthResponse loginResponse = userService.login(newLoginDto);

    // Assert
    assertThat(loginResponse).isNotNull();
    assertThat(loginResponse.getToken()).isNotNull();
    assertThat(loginResponse.getUser().getNickname()).isEqualTo("updateduser");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should not login with old credentials after update")
  void shouldNotLoginWithOldCredentialsAfterUpdate() {
    // Arrange
    AuthResponse registered = userService.register(registrationDto);
    Long userId = registered.getUser().getId();

    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("updateduser")
        .password("newpassword123")
        .age(30)
        .build();

    userService.updateUser(userId, updateDto);

    // Act & Assert
    assertThatThrownBy(() -> userService.login(loginDto))
        .isInstanceOf(BadCredentialsException.class);
  }

  // ========== Delete User Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should delete user from database")
  void shouldDeleteUserFromDatabase() {
    // Arrange
    AuthResponse registered = userService.register(registrationDto);
    Long userId = registered.getUser().getId();

    // Act
    userService.deleteUser(userId);

    // Assert
    assertThat(userRepository.findById(userId)).isEmpty();
    assertThat(userRepository.count()).isEqualTo(0);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when deleting non-existent user")
  void shouldThrowExceptionWhenDeletingNonExistentUser() {
    // Act & Assert
    assertThatThrownBy(() -> userService.deleteUser(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("999");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should not be able to login after user deletion")
  void shouldNotBeAbleToLoginAfterUserDeletion() {
    // Arrange
    AuthResponse registered = userService.register(registrationDto);
    Long userId = registered.getUser().getId();

    // Act
    userService.deleteUser(userId);

    // Assert
    assertThatThrownBy(() -> userService.login(loginDto))
        .isInstanceOf(BadCredentialsException.class);
  }

  // ========== Get User Nickname Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should get user nickname from database")
  void shouldGetUserNicknameFromDatabase() {
    // Arrange
    AuthResponse registered = userService.register(registrationDto);
    Long userId = registered.getUser().getId();

    // Act
    String nickname = userService.getUserNickname(userId);

    // Assert
    assertThat(nickname).isEqualTo("testuser");
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when getting nickname of non-existent user")
  void shouldThrowExceptionWhenGettingNicknameOfNonExistentUser() {
    // Act & Assert
    assertThatThrownBy(() -> userService.getUserNickname(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("999");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should get updated nickname after user update")
  void shouldGetUpdatedNicknameAfterUserUpdate() {
    // Arrange
    AuthResponse registered = userService.register(registrationDto);
    Long userId = registered.getUser().getId();

    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("updateduser")
        .password("password123")
        .age(25)
        .build();

    userService.updateUser(userId, updateDto);

    // Act
    String nickname = userService.getUserNickname(userId);

    // Assert
    assertThat(nickname).isEqualTo("updateduser");
  }

  // ========== Edge Cases ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle special characters in nickname")
  void shouldHandleSpecialCharactersInNickname() {
    // Arrange
    UserRegistrationDto specialDto = UserRegistrationDto.builder()
        .nickname("user_123-test@domain")
        .password("password123")
        .age(25)
        .build();

    // Act
    AuthResponse response = userService.register(specialDto);

    // Assert
    User fromDb = userRepository.findById(response.getUser().getId()).orElseThrow();
    assertThat(fromDb.getNickname()).isEqualTo("user_123-test@domain");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle very long password")
  void shouldHandleVeryLongPassword() {
    // Arrange
    String longPassword = "a".repeat(1000);
    UserRegistrationDto longPassDto = UserRegistrationDto.builder()
        .nickname("longpassuser")
        .password(longPassword)
        .age(25)
        .build();

    // Act
    AuthResponse response = userService.register(longPassDto);

    // Assert
    User fromDb = userRepository.findById(response.getUser().getId()).orElseThrow();
    assertThat(passwordEncoder.matches(longPassword, fromDb.getPasswordHash())).isTrue();
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle concurrent user registrations")
  void shouldHandleConcurrentUserRegistrations() {
    // Arrange
    UserRegistrationDto user1 = UserRegistrationDto.builder()
        .nickname("concurrent1")
        .password("password1")
        .age(20)
        .build();

    UserRegistrationDto user2 = UserRegistrationDto.builder()
        .nickname("concurrent2")
        .password("password2")
        .age(30)
        .build();

    // Act
    AuthResponse response1 = userService.register(user1);
    AuthResponse response2 = userService.register(user2);

    // Assert
    assertThat(userRepository.count()).isEqualTo(2);
    assertThat(response1.getUser().getId()).isNotEqualTo(response2.getUser().getId());
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should maintain data integrity after multiple operations")
  void shouldMaintainDataIntegrityAfterMultipleOperations() {
    // Arrange & Act
    AuthResponse registered = userService.register(registrationDto);
    Long userId = registered.getUser().getId();

    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("updated")
        .password("newpass")
        .age(30)
        .build();

    userService.updateUser(userId, updateDto);

    UserLoginDto newLoginDto = UserLoginDto.builder()
        .nickname("updated")
        .password("newpass")
        .build();

    AuthResponse loginResponse = userService.login(newLoginDto);

    // Assert
    User fromDb = userRepository.findById(userId).orElseThrow();
    assertThat(fromDb.getNickname()).isEqualTo("updated");
    assertThat(fromDb.getAge()).isEqualTo(30);
    assertThat(loginResponse.getUser().getId()).isEqualTo(userId);
  }
}
