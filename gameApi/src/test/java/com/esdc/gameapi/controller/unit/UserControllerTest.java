package com.esdc.gameapi.controller.unit;

import com.esdc.gameapi.controller.UserController;
import com.esdc.gameapi.domain.dto.AuthResponse;
import com.esdc.gameapi.domain.dto.UserLoginDto;
import com.esdc.gameapi.domain.dto.UserRegistrationDto;
import com.esdc.gameapi.domain.dto.UserResponseDto;
import com.esdc.gameapi.exception.DuplicateResourceException;
import com.esdc.gameapi.exception.GlobalExceptionHandler;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("User Controller Unit Tests")
class UserControllerTest {

  private MockMvc mockMvc;

  @Mock
  private UserService userService;

  @InjectMocks
  private UserController userController;

  private ObjectMapper objectMapper;

  private UserRegistrationDto testRegistrationDto;
  private UserLoginDto testLoginDto;
  private UserResponseDto testUserResponse;
  private AuthResponse testAuthResponse;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    mockMvc = MockMvcBuilders.standaloneSetup(userController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    testRegistrationDto = UserRegistrationDto.builder()
        .nickname("testuser")
        .password("password123")
        .age(25)
        .build();

    testLoginDto = UserLoginDto.builder()
        .nickname("testuser")
        .password("password123")
        .build();

    testUserResponse = UserResponseDto.builder()
        .id(1L)
        .nickname("testuser")
        .age(25)
        .createdAt("2025-12-17T14:00:00")
        .updatedAt("2025-12-17T14:00:00")
        .build();

    testAuthResponse = AuthResponse.builder()
        .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        .user(testUserResponse)
        .build();
  }

  // ========== POST Register Tests ==========

  @Nested
  @DisplayName("POST /api/users/register")
  class RegisterTests {

    @Test
    @Tag("unit")
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() throws Exception {
      // Arrange
      when(userService.register(any(UserRegistrationDto.class))).thenReturn(testAuthResponse);

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(testRegistrationDto)))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
          .andExpect(jsonPath("$.user.id").value(1))
          .andExpect(jsonPath("$.user.nickname").value("testuser"))
          .andExpect(jsonPath("$.user.age").value(25));

      verify(userService, times(1)).register(any(UserRegistrationDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should reject registration with duplicate nickname")
    void shouldRejectRegistrationWithDuplicateNickname() throws Exception {
      // Arrange
      when(userService.register(any(UserRegistrationDto.class)))
          .thenThrow(new DuplicateResourceException("User", "nickname", "testuser"));

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(testRegistrationDto)))
          .andExpect(status().isConflict());

      verify(userService, times(1)).register(any(UserRegistrationDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should register user with minimum age")
    void shouldRegisterUserWithMinimumAge() throws Exception {
      // Arrange
      UserRegistrationDto youngUser = UserRegistrationDto.builder()
          .nickname("younguser")
          .password("password123")
          .age(10)
          .build();

      UserResponseDto youngUserResponse = UserResponseDto.builder()
          .id(2L)
          .nickname("younguser")
          .age(10)
          .createdAt("2025-12-17T14:00:00")
          .updatedAt("2025-12-17T14:00:00")
          .build();

      AuthResponse youngAuthResponse = AuthResponse.builder()
          .token("token123")
          .user(youngUserResponse)
          .build();

      when(userService.register(any(UserRegistrationDto.class))).thenReturn(youngAuthResponse);

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(youngUser)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.user.age").value(10));

      verify(userService, times(1)).register(any(UserRegistrationDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should register user with null age")
    void shouldRegisterUserWithNullAge() throws Exception {
      // Arrange
      UserRegistrationDto noAgeUser = UserRegistrationDto.builder()
          .nickname("noageuser")
          .password("password123")
          .age(null)
          .build();

      UserResponseDto noAgeResponse = UserResponseDto.builder()
          .id(3L)
          .nickname("noageuser")
          .age(null)
          .createdAt("2025-12-17T14:00:00")
          .updatedAt("2025-12-17T14:00:00")
          .build();

      AuthResponse noAgeAuthResponse = AuthResponse.builder()
          .token("token456")
          .user(noAgeResponse)
          .build();

      when(userService.register(any(UserRegistrationDto.class))).thenReturn(noAgeAuthResponse);

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(noAgeUser)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.user.age").isEmpty());

      verify(userService, times(1)).register(any(UserRegistrationDto.class));
    }
  }

  // ========== POST Login Tests ==========

  @Nested
  @DisplayName("POST /api/users/login")
  class LoginTests {

    @Test
    @Tag("unit")
    @DisplayName("Should login user successfully")
    void shouldLoginUserSuccessfully() throws Exception {
      // Arrange
      when(userService.login(any(UserLoginDto.class))).thenReturn(testAuthResponse);

      // Act & Assert
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(testLoginDto)))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
          .andExpect(jsonPath("$.user.nickname").value("testuser"));

      verify(userService, times(1)).login(any(UserLoginDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should reject login with invalid credentials")
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
      // Arrange
      when(userService.login(any(UserLoginDto.class)))
          .thenThrow(new BadCredentialsException("Invalid credentials"));

      // Act & Assert
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(testLoginDto)))
          .andExpect(status().isUnauthorized());

      verify(userService, times(1)).login(any(UserLoginDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should reject login with non-existent user")
    void shouldRejectLoginWithNonExistentUser() throws Exception {
      // Arrange
      UserLoginDto nonExistentUser = UserLoginDto.builder()
          .nickname("nonexistent")
          .password("password123")
          .build();

      when(userService.login(any(UserLoginDto.class)))
          .thenThrow(new ResourceNotFoundException("User", "nickname", "nonexistent"));

      // Act & Assert
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(nonExistentUser)))
          .andExpect(status().isNotFound());

      verify(userService, times(1)).login(any(UserLoginDto.class));
    }
  }

  // ========== PUT Update User Tests ==========

  @Nested
  @DisplayName("PUT /api/users/{userId}")
  class UpdateUserTests {

    @Test
    @Tag("unit")
    @DisplayName("Should update user successfully")
    void shouldUpdateUserSuccessfully() throws Exception {
      // Arrange
      UserRegistrationDto updateDto = UserRegistrationDto.builder()
          .nickname("updateduser")
          .password("newpassword123")
          .age(30)
          .build();

      UserResponseDto updatedResponse = UserResponseDto.builder()
          .id(1L)
          .nickname("updateduser")
          .age(30)
          .createdAt("2025-12-17T14:00:00")
          .updatedAt("2025-12-17T15:00:00")
          .build();

      when(userService.updateUser(eq(1L), any(UserRegistrationDto.class)))
          .thenReturn(updatedResponse);

      // Act & Assert
      mockMvc.perform(put("/api/users/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.nickname").value("updateduser"))
          .andExpect(jsonPath("$.age").value(30));

      verify(userService, times(1)).updateUser(eq(1L), any(UserRegistrationDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 404 when updating non-existent user")
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
      // Arrange
      UserRegistrationDto updateDto = UserRegistrationDto.builder()
          .nickname("updateduser")
          .password("newpassword123")
          .age(30)
          .build();

      when(userService.updateUser(eq(999L), any(UserRegistrationDto.class)))
          .thenThrow(new ResourceNotFoundException("User", "id", 999L));

      // Act & Assert
      mockMvc.perform(put("/api/users/999")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isNotFound());

      verify(userService, times(1)).updateUser(eq(999L), any(UserRegistrationDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should update user with different age")
    void shouldUpdateUserWithDifferentAge() throws Exception {
      // Arrange
      UserRegistrationDto updateDto = UserRegistrationDto.builder()
          .nickname("testuser")
          .password("password123")
          .age(50)
          .build();

      UserResponseDto updatedResponse = UserResponseDto.builder()
          .id(1L)
          .nickname("testuser")
          .age(50)
          .createdAt("2025-12-17T14:00:00")
          .updatedAt("2025-12-17T15:00:00")
          .build();

      when(userService.updateUser(eq(1L), any(UserRegistrationDto.class)))
          .thenReturn(updatedResponse);

      // Act & Assert
      mockMvc.perform(put("/api/users/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.age").value(50));

      verify(userService, times(1)).updateUser(eq(1L), any(UserRegistrationDto.class));
    }
  }

  // ========== DELETE User Tests ==========

  @Nested
  @DisplayName("DELETE /api/users/{userId}")
  class DeleteUserTests {

    @Test
    @Tag("unit")
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() throws Exception {
      // Arrange
      doNothing().when(userService).deleteUser(1L);

      // Act & Assert
      mockMvc.perform(delete("/api/users/1"))
          .andExpect(status().isNoContent());

      verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 404 when deleting non-existent user")
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
      // Arrange
      doThrow(new ResourceNotFoundException("User", "id", 999L))
          .when(userService).deleteUser(999L);

      // Act & Assert
      mockMvc.perform(delete("/api/users/999"))
          .andExpect(status().isNotFound());

      verify(userService, times(1)).deleteUser(999L);
    }
  }

  // ========== GET User Nickname Tests ==========

  @Nested
  @DisplayName("GET /api/users/{userId}/nickname")
  class GetUserNicknameTests {

    @Test
    @Tag("unit")
    @DisplayName("Should return user nickname successfully")
    void shouldReturnUserNicknameSuccessfully() throws Exception {
      // Arrange
      when(userService.getUserNickname(1L)).thenReturn("testuser");

      // Act & Assert
      mockMvc.perform(get("/api/users/1/nickname")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().string("testuser"));

      verify(userService, times(1)).getUserNickname(1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 404 when getting nickname of non-existent user")
    void shouldReturn404WhenGettingNicknameOfNonExistentUser() throws Exception {
      // Arrange
      when(userService.getUserNickname(999L))
          .thenThrow(new ResourceNotFoundException("User", "id", 999L));

      // Act & Assert
      mockMvc.perform(get("/api/users/999/nickname")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());

      verify(userService, times(1)).getUserNickname(999L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return nickname for different user")
    void shouldReturnNicknameForDifferentUser() throws Exception {
      // Arrange
      when(userService.getUserNickname(5L)).thenReturn("anotheruser");

      // Act & Assert
      mockMvc.perform(get("/api/users/5/nickname")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().string("anotheruser"));

      verify(userService, times(1)).getUserNickname(5L);
    }
  }

  // ========== Edge Cases Tests ==========

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @Tag("unit")
    @DisplayName("Should handle very long nickname")
    void shouldHandleVeryLongNickname() throws Exception {
      // Arrange
      String longNickname = "a".repeat(100);
      UserRegistrationDto longNicknameDto = UserRegistrationDto.builder()
          .nickname(longNickname)
          .password("password123")
          .age(25)
          .build();

      UserResponseDto longNicknameResponse = UserResponseDto.builder()
          .id(10L)
          .nickname(longNickname)
          .age(25)
          .createdAt("2025-12-17T14:00:00")
          .updatedAt("2025-12-17T14:00:00")
          .build();

      AuthResponse longNicknameAuthResponse = AuthResponse.builder()
          .token("token_long")
          .user(longNicknameResponse)
          .build();

      when(userService.register(any(UserRegistrationDto.class)))
          .thenReturn(longNicknameAuthResponse);

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(longNicknameDto)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.user.nickname").value(longNickname));

      verify(userService, times(1)).register(any(UserRegistrationDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should handle user with special characters in nickname")
    void shouldHandleUserWithSpecialCharactersInNickname() throws Exception {
      // Arrange
      String specialNickname = "user_123-test!@";
      UserRegistrationDto specialDto = UserRegistrationDto.builder()
          .nickname(specialNickname)
          .password("password123")
          .age(25)
          .build();

      UserResponseDto specialResponse = UserResponseDto.builder()
          .id(11L)
          .nickname(specialNickname)
          .age(25)
          .createdAt("2025-12-17T14:00:00")
          .updatedAt("2025-12-17T14:00:00")
          .build();

      AuthResponse specialAuthResponse = AuthResponse.builder()
          .token("token_special")
          .user(specialResponse)
          .build();

      when(userService.register(any(UserRegistrationDto.class)))
          .thenReturn(specialAuthResponse);

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(specialDto)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.user.nickname").value(specialNickname));

      verify(userService, times(1)).register(any(UserRegistrationDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should handle user with very high age")
    void shouldHandleUserWithVeryHighAge() throws Exception {
      // Arrange
      UserRegistrationDto oldUserDto = UserRegistrationDto.builder()
          .nickname("olduser")
          .password("password123")
          .age(150)
          .build();

      UserResponseDto oldUserResponse = UserResponseDto.builder()
          .id(12L)
          .nickname("olduser")
          .age(150)
          .createdAt("2025-12-17T14:00:00")
          .updatedAt("2025-12-17T14:00:00")
          .build();

      AuthResponse oldUserAuthResponse = AuthResponse.builder()
          .token("token_old")
          .user(oldUserResponse)
          .build();

      when(userService.register(any(UserRegistrationDto.class)))
          .thenReturn(oldUserAuthResponse);

      // Act & Assert
      mockMvc.perform(post("/api/users/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(oldUserDto)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.user.age").value(150));

      verify(userService, times(1)).register(any(UserRegistrationDto.class));
    }
  }
}
