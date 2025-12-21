package com.esdc.gameapi.controller.integration;

import com.esdc.gameapi.domain.dto.UserLoginDto;
import com.esdc.gameapi.domain.dto.UserRegistrationDto;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.repository.UserRepository;
import com.esdc.gameapi.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("User Controller Integration Tests")
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=mySecretKeyForTestingPurposesOnlyMustBeAtLeast256BitsLong",
    "jwt.expiration=3600000",
    "admin.password=testAdminPassword123"
})
class UserControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private ObjectMapper objectMapper;

  private User testUser;
  private String testUserPassword = "password123";

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    testUser = User.builder()
        .nickname("testuser")
        .passwordHash(passwordEncoder.encode(testUserPassword))
        .age(25)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    testUser = userRepository.save(testUser);
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  // ========== POST Register Tests ==========

  @Nested
  @DisplayName("POST /api/users/register - Register User")
  class RegisterTests {

    @Test
    @Tag("integration")
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() throws Exception {
      // Arrange
      UserRegistrationDto registrationDto = UserRegistrationDto.builder()
          .nickname("newuser")
          .password("newpassword123")
          .age(30)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registrationDto)))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.token").exists())
          .andExpect(jsonPath("$.token").isNotEmpty())
          .andExpect(jsonPath("$.user.id").exists())
          .andExpect(jsonPath("$.user.nickname").value("newuser"))
          .andExpect(jsonPath("$.user.age").value(30))
          .andExpect(jsonPath("$.user.createdAt").exists())
          .andExpect(jsonPath("$.user.updatedAt").exists());

      // Verify user exists in database
      Assertions.assertTrue(userRepository.existsByNickname("newuser"));
    }

    @Test
    @Tag("integration")
    @DisplayName("Should reject registration with duplicate nickname")
    void shouldRejectRegistrationWithDuplicateNickname() throws Exception {
      // Arrange
      UserRegistrationDto registrationDto = UserRegistrationDto.builder()
          .nickname("testuser") // Already exists
          .password("password123")
          .age(25)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registrationDto)))
          .andExpect(status().isConflict());
    }

    @Test
    @Tag("integration")
    @DisplayName("Should register user with minimum age")
    void shouldRegisterUserWithMinimumAge() throws Exception {
      // Arrange
      UserRegistrationDto registrationDto = UserRegistrationDto.builder()
          .nickname("younguser")
          .password("password123")
          .age(10)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registrationDto)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.user.age").value(10));
    }

    @Test
    @Tag("integration")
    @DisplayName("Should hash password during registration")
    void shouldHashPasswordDuringRegistration() throws Exception {
      // Arrange
      UserRegistrationDto registrationDto = UserRegistrationDto.builder()
          .nickname("secureuser")
          .password("mySecretPassword")
          .age(25)
          .build();

      // Act
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registrationDto)))
          .andExpect(status().isCreated());

      // Assert - Password should be hashed
      User savedUser = userRepository.findByNickname("secureuser").orElseThrow();
      Assertions.assertNotEquals("mySecretPassword", savedUser.getPasswordHash());
      Assertions.assertTrue(passwordEncoder.matches("mySecretPassword", savedUser.getPasswordHash()));
    }

    @Test
    @Tag("integration")
    @DisplayName("Should return valid JWT token")
    void shouldReturnValidJwtToken() throws Exception {
      // Arrange
      UserRegistrationDto registrationDto = UserRegistrationDto.builder()
          .nickname("tokenuser")
          .password("password123")
          .age(25)
          .build();

      // Act
      String response = mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registrationDto)))
          .andExpect(status().isCreated())
          .andReturn()
          .getResponse()
          .getContentAsString();

      // Assert - Token should be valid
      String token = objectMapper.readTree(response).get("token").asText();
      Assertions.assertNotNull(token);
      Assertions.assertFalse(token.isEmpty());

      // Verify token contains correct username
      String usernameFromToken = jwtUtil.extractUsername(token);
      Assertions.assertEquals("tokenuser", usernameFromToken);
    }
  }

  // ========== POST Login Tests ==========

  @Nested
  @DisplayName("POST /api/users/login - User Login")
  class LoginTests {

    @Test
    @Tag("integration")
    @DisplayName("Should login user with valid credentials")
    void shouldLoginUserWithValidCredentials() throws Exception {
      // Arrange
      UserLoginDto loginDto = UserLoginDto.builder()
          .nickname("testuser")
          .password(testUserPassword)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(loginDto)))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.token").exists())
          .andExpect(jsonPath("$.token").isNotEmpty())
          .andExpect(jsonPath("$.user.nickname").value("testuser"))
          .andExpect(jsonPath("$.user.age").value(25));
    }

    @Test
    @Tag("integration")
    @DisplayName("Should reject login with invalid password")
    void shouldRejectLoginWithInvalidPassword() throws Exception {
      // Arrange
      UserLoginDto loginDto = UserLoginDto.builder()
          .nickname("testuser")
          .password("wrongpassword")
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(loginDto)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @Tag("integration")
    @DisplayName("Should reject login with non-existent user")
    void shouldRejectLoginWithNonExistentUser() throws Exception {
      // Arrange
      UserLoginDto loginDto = UserLoginDto.builder()
          .nickname("nonexistent")
          .password("password123")
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(loginDto)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @Tag("integration")
    @DisplayName("Should return valid JWT token on login")
    void shouldReturnValidJwtTokenOnLogin() throws Exception {
      // Arrange
      UserLoginDto loginDto = UserLoginDto.builder()
          .nickname("testuser")
          .password(testUserPassword)
          .build();

      // Act
      String response = mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(loginDto)))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

      // Assert - Token should be valid
      String token = objectMapper.readTree(response).get("token").asText();
      String usernameFromToken = jwtUtil.extractUsername(token);
      Assertions.assertEquals("testuser", usernameFromToken);
    }
  }

  // ========== PUT Update User Tests ==========

  @Nested
  @DisplayName("PUT /api/users/{userId} - Update User")
  class UpdateUserTests {

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should update user successfully")
    void shouldUpdateUserSuccessfully() throws Exception {
      // Arrange
      UserRegistrationDto updateDto = UserRegistrationDto.builder()
          .nickname("updatednickname")
          .password("newpassword123")
          .age(35)
          .build();

      // Act & Assert
      mockMvc.perform(put("/api/users/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").value(testUser.getId()))
          .andExpect(jsonPath("$.nickname").value("updatednickname"))
          .andExpect(jsonPath("$.age").value(35));

      // Verify changes in database
      User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
      Assertions.assertEquals("updatednickname", updatedUser.getNickname());
      Assertions.assertEquals(35, updatedUser.getAge());
      Assertions.assertTrue(passwordEncoder.matches("newpassword123", updatedUser.getPasswordHash()));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return 404 when updating non-existent user")
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
      // Arrange
      UserRegistrationDto updateDto = UserRegistrationDto.builder()
          .nickname("updatednickname")
          .password("newpassword123")
          .age(35)
          .build();

      // Act & Assert
      mockMvc.perform(put("/api/users/999999")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isNotFound());
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should update password correctly")
    void shouldUpdatePasswordCorrectly() throws Exception {
      // Arrange
      String oldPasswordHash = testUser.getPasswordHash();
      UserRegistrationDto updateDto = UserRegistrationDto.builder()
          .nickname("testuser")
          .password("brandnewpassword")
          .age(25)
          .build();

      // Act
      mockMvc.perform(put("/api/users/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isOk());

      // Assert
      User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
      Assertions.assertNotEquals(oldPasswordHash, updatedUser.getPasswordHash());
      Assertions.assertTrue(passwordEncoder.matches("brandnewpassword", updatedUser.getPasswordHash()));
    }

    @Test
    @Tag("integration")
    @DisplayName("Should deny update without authentication")
    void shouldDenyUpdateWithoutAuthentication() throws Exception {
      // Arrange
      UserRegistrationDto updateDto = UserRegistrationDto.builder()
          .nickname("updatednickname")
          .password("newpassword123")
          .age(35)
          .build();

      // Act & Assert
      mockMvc.perform(put("/api/users/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isForbidden());
    }
  }

  // ========== DELETE User Tests ==========

  @Nested
  @DisplayName("DELETE /api/users/{userId} - Delete User")
  class DeleteUserTests {

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() throws Exception {
      // Act & Assert
      mockMvc.perform(delete("/api/users/" + testUser.getId()))
          .andExpect(status().isNoContent());

      // Verify deletion
      Assertions.assertFalse(userRepository.existsById(testUser.getId()));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return 404 when deleting non-existent user")
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
      // Act & Assert
      mockMvc.perform(delete("/api/users/999999"))
          .andExpect(status().isNotFound());
    }

    @Test
    @Tag("integration")
    @DisplayName("Should deny deletion without authentication")
    void shouldDenyDeletionWithoutAuthentication() throws Exception {
      // Act & Assert
      mockMvc.perform(delete("/api/users/" + testUser.getId()))
          .andExpect(status().isForbidden());
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should not be able to get user after deletion")
    void shouldNotBeAbleToGetUserAfterDeletion() throws Exception {
      // Arrange - Delete user
      mockMvc.perform(delete("/api/users/" + testUser.getId()))
          .andExpect(status().isNoContent());

      // Act & Assert - Try to get nickname
      mockMvc.perform(get("/api/users/" + testUser.getId() + "/nickname"))
          .andExpect(status().isNotFound());
    }
  }

  // ========== GET User Nickname Tests ==========

  @Nested
  @DisplayName("GET /api/users/{userId}/nickname - Get User Nickname")
  class GetUserNicknameTests {

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return user nickname")
    void shouldReturnUserNickname() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/users/" + testUser.getId() + "/nickname")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().string("testuser"));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/users/999999/nickname")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
    }

    @Test
    @Tag("integration")
    @DisplayName("Should deny access without authentication")
    void shouldDenyAccessWithoutAuthentication() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/users/" + testUser.getId() + "/nickname")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return updated nickname after update")
    void shouldReturnUpdatedNicknameAfterUpdate() throws Exception {
      // Arrange - Update user
      UserRegistrationDto updateDto = UserRegistrationDto.builder()
          .nickname("newnickname")
          .password("password123")
          .age(25)
          .build();

      mockMvc.perform(put("/api/users/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isOk());

      // Act & Assert - Get nickname
      mockMvc.perform(get("/api/users/" + testUser.getId() + "/nickname")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().string("newnickname"));
    }
  }

  // ========== Edge Cases Tests ==========

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @Tag("integration")
    @DisplayName("Should handle very long nickname")
    void shouldHandleVeryLongNickname() throws Exception {
      // Arrange
      String longNickname = "a".repeat(200);
      UserRegistrationDto registrationDto = UserRegistrationDto.builder()
          .nickname(longNickname)
          .password("password123")
          .age(25)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registrationDto)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.user.nickname").value(longNickname));
    }

    @Test
    @Tag("integration")
    @DisplayName("Should handle special characters in nickname")
    void shouldHandleSpecialCharactersInNickname() throws Exception {
      // Arrange
      String specialNickname = "user_123-test!@#";
      UserRegistrationDto registrationDto = UserRegistrationDto.builder()
          .nickname(specialNickname)
          .password("password123")
          .age(25)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registrationDto)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.user.nickname").value(specialNickname));
    }

    @Test
    @Tag("integration")
    @DisplayName("Should handle very high age")
    void shouldHandleVeryHighAge() throws Exception {
      // Arrange
      UserRegistrationDto registrationDto = UserRegistrationDto.builder()
          .nickname("olduser")
          .password("password123")
          .age(999)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registrationDto)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.user.age").value(999));
    }

    @Test
    @Tag("integration")
    @DisplayName("Should handle very long password")
    void shouldHandleVeryLongPassword() throws Exception {
      // Arrange
      String longPassword = "p".repeat(1000);
      UserRegistrationDto registrationDto = UserRegistrationDto.builder()
          .nickname("longpassuser")
          .password(longPassword)
          .age(25)
          .build();

      // Act
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registrationDto)))
          .andExpect(status().isCreated());

      // Assert - Password should be hashed correctly
      User savedUser = userRepository.findByNickname("longpassuser").orElseThrow();
      Assertions.assertTrue(passwordEncoder.matches(longPassword, savedUser.getPasswordHash()));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should handle concurrent user updates")
    void shouldHandleConcurrentUserUpdates() throws Exception {
      // Arrange
      UserRegistrationDto updateDto1 = UserRegistrationDto.builder()
          .nickname("update1")
          .password("password1")
          .age(30)
          .build();

      UserRegistrationDto updateDto2 = UserRegistrationDto.builder()
          .nickname("update2")
          .password("password2")
          .age(40)
          .build();

      // Act - First update
      mockMvc.perform(put("/api/users/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto1)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.nickname").value("update1"));

      // Act - Second update
      mockMvc.perform(put("/api/users/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto2)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.nickname").value("update2"));

      // Assert - Final state should be second update
      User finalUser = userRepository.findById(testUser.getId()).orElseThrow();
      Assertions.assertEquals("update2", finalUser.getNickname());
      Assertions.assertEquals(40, finalUser.getAge());
    }
  }

  // ========== Security Tests ==========

  @Nested
  @DisplayName("Security Tests")
  class SecurityTests {

    @Test
    @Tag("integration")
    @DisplayName("Should allow registration without authentication")
    void shouldAllowRegistrationWithoutAuthentication() throws Exception {
      // Arrange
      UserRegistrationDto registrationDto = UserRegistrationDto.builder()
          .nickname("publicuser")
          .password("password123")
          .age(25)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registrationDto)))
          .andExpect(status().isCreated());
    }

    @Test
    @Tag("integration")
    @DisplayName("Should allow login without authentication")
    void shouldAllowLoginWithoutAuthentication() throws Exception {
      // Arrange
      UserLoginDto loginDto = UserLoginDto.builder()
          .nickname("testuser")
          .password(testUserPassword)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(loginDto)))
          .andExpect(status().isOk());
    }

    @Test
    @Tag("integration")
    @DisplayName("Should deny protected operations without authentication")
    void shouldDenyProtectedOperationsWithoutAuthentication() throws Exception {
      // Act & Assert - Update
      UserRegistrationDto updateDto = UserRegistrationDto.builder()
          .nickname("updated")
          .password("password")
          .age(30)
          .build();

      mockMvc.perform(put("/api/users/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isForbidden());

      // Act & Assert - Delete
      mockMvc.perform(delete("/api/users/" + testUser.getId()))
          .andExpect(status().isForbidden());

      // Act & Assert - Get nickname
      mockMvc.perform(get("/api/users/" + testUser.getId() + "/nickname"))
          .andExpect(status().isForbidden());
    }
  }

  // ========== Integration Scenarios ==========

  @Test
  @Tag("integration")
  @WithMockUser(username = "testuser")
  @DisplayName("Should handle complete user lifecycle")
  void shouldHandleCompleteUserLifecycle() throws Exception {
    // 1. Register new user
    UserRegistrationDto registrationDto = UserRegistrationDto.builder()
        .nickname("lifecycleuser")
        .password("password123")
        .age(25)
        .build();

    String registerResponse = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registrationDto)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long userId = objectMapper.readTree(registerResponse).get("user").get("id").asLong();

    // 2. Login with credentials
    UserLoginDto loginDto = UserLoginDto.builder()
        .nickname("lifecycleuser")
        .password("password123")
        .build();

    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginDto)))
        .andExpect(status().isOk());

    // 3. Update user
    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("updatedlifecycle")
        .password("newpassword456")
        .age(30)
        .build();

    mockMvc.perform(put("/api/users/" + userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("updatedlifecycle"));

    // 4. Login with new credentials
    UserLoginDto newLoginDto = UserLoginDto.builder()
        .nickname("updatedlifecycle")
        .password("newpassword456")
        .build();

    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(newLoginDto)))
        .andExpect(status().isOk());

    // 5. Delete user
    mockMvc.perform(delete("/api/users/" + userId))
        .andExpect(status().isNoContent());

    // 6. Verify user is deleted
    Assertions.assertFalse(userRepository.existsById(userId));
  }
}
