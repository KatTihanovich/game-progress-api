package com.esdc.gameapi.service.unit;

import com.esdc.gameapi.domain.dto.AuthResponse;
import com.esdc.gameapi.domain.dto.UserLoginDto;
import com.esdc.gameapi.domain.dto.UserRegistrationDto;
import com.esdc.gameapi.domain.dto.UserResponseDto;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.exception.DuplicateResourceException;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.UserRepository;
import com.esdc.gameapi.security.JwtUtil;
import com.esdc.gameapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Unit Tests")
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtUtil jwtUtil;

  @Mock
  private AuthenticationManager authenticationManager;

  @InjectMocks
  private UserService userService;

  private User testUser;
  private UserRegistrationDto registrationDto;
  private UserLoginDto loginDto;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .nickname("testuser")
        .passwordHash("encodedPassword")
        .age(25)
        .build();
    testUser.setCreatedAt(LocalDateTime.now());
    testUser.setUpdatedAt(LocalDateTime.now());

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

  // ========== Registration Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should register user successfully")
  void shouldRegisterUserSuccessfully() {
    // Arrange
    when(userRepository.existsByNickname("testuser")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(jwtUtil.generateToken("testuser", 1L)).thenReturn("jwt-token");

    // Act
    AuthResponse result = userService.register(registrationDto);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getToken()).isEqualTo("jwt-token");
    assertThat(result.getUser()).isNotNull();
    assertThat(result.getUser().getNickname()).isEqualTo("testuser");
    assertThat(result.getUser().getAge()).isEqualTo(25);

    verify(userRepository, times(1)).existsByNickname("testuser");
    verify(passwordEncoder, times(1)).encode("password123");
    verify(userRepository, times(1)).save(any(User.class));
    verify(jwtUtil, times(1)).generateToken("testuser", 1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when registering duplicate nickname")
  void shouldThrowExceptionWhenRegisteringDuplicateNickname() {
    // Arrange
    when(userRepository.existsByNickname("testuser")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> userService.register(registrationDto))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("nickname")
        .hasMessageContaining("testuser");

    verify(userRepository, times(1)).existsByNickname("testuser");
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(jwtUtil, never()).generateToken(anyString(), any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should encode password when registering")
  void shouldEncodePasswordWhenRegistering() {
    // Arrange
    when(userRepository.existsByNickname("testuser")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token");

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

    // Act
    userService.register(registrationDto);

    // Assert
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getPasswordHash()).isEqualTo("encodedPassword");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should register user with minimum age")
  void shouldRegisterUserWithMinimumAge() {
    // Arrange
    UserRegistrationDto minAgeDto = UserRegistrationDto.builder()
        .nickname("younguser")
        .password("password123")
        .age(1)
        .build();

    User youngUser = User.builder()
        .id(2L)
        .nickname("younguser")
        .passwordHash("encodedPassword")
        .age(1)
        .build();

    when(userRepository.existsByNickname("younguser")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(youngUser);
    when(jwtUtil.generateToken("younguser", 2L)).thenReturn("jwt-token");

    // Act
    AuthResponse result = userService.register(minAgeDto);

    // Assert
    assertThat(result.getUser().getAge()).isEqualTo(1);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should register user with maximum age")
  void shouldRegisterUserWithMaximumAge() {
    // Arrange
    UserRegistrationDto maxAgeDto = UserRegistrationDto.builder()
        .nickname("olduser")
        .password("password123")
        .age(150)
        .build();

    User oldUser = User.builder()
        .id(3L)
        .nickname("olduser")
        .passwordHash("encodedPassword")
        .age(150)
        .build();

    when(userRepository.existsByNickname("olduser")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(oldUser);
    when(jwtUtil.generateToken("olduser", 3L)).thenReturn("jwt-token");

    // Act
    AuthResponse result = userService.register(maxAgeDto);

    // Assert
    assertThat(result.getUser().getAge()).isEqualTo(150);
  }

  // ========== Login Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should login user successfully")
  void shouldLoginUserSuccessfully() {
    // Arrange
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(null);
    when(userRepository.findByNickname("testuser")).thenReturn(Optional.of(testUser));
    when(jwtUtil.generateToken("testuser", 1L)).thenReturn("jwt-token");

    // Act
    AuthResponse result = userService.login(loginDto);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getToken()).isEqualTo("jwt-token");
    assertThat(result.getUser()).isNotNull();
    assertThat(result.getUser().getNickname()).isEqualTo("testuser");

    verify(authenticationManager, times(1))
        .authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(userRepository, times(1)).findByNickname("testuser");
    verify(jwtUtil, times(1)).generateToken("testuser", 1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when login with wrong credentials")
  void shouldThrowExceptionWhenLoginWithWrongCredentials() {
    // Arrange
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    // Act & Assert
    assertThatThrownBy(() -> userService.login(loginDto))
        .isInstanceOf(BadCredentialsException.class);

    verify(authenticationManager, times(1))
        .authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(userRepository, never()).findByNickname(anyString());
    verify(jwtUtil, never()).generateToken(anyString(), any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when user not found after authentication")
  void shouldThrowExceptionWhenUserNotFoundAfterAuthentication() {
    // Arrange
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(null);
    when(userRepository.findByNickname("testuser")).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> userService.login(loginDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("nickname")
        .hasMessageContaining("testuser");

    verify(userRepository, times(1)).findByNickname("testuser");
    verify(jwtUtil, never()).generateToken(anyString(), any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should authenticate with correct credentials")
  void shouldAuthenticateWithCorrectCredentials() {
    // Arrange
    ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
        ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(null);
    when(userRepository.findByNickname("testuser")).thenReturn(Optional.of(testUser));
    when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token");

    // Act
    userService.login(loginDto);

    // Assert
    verify(authenticationManager).authenticate(authCaptor.capture());
    UsernamePasswordAuthenticationToken token = authCaptor.getValue();
    assertThat(token.getPrincipal()).isEqualTo("testuser");
    assertThat(token.getCredentials()).isEqualTo("password123");
  }

  // ========== Update User Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should update user successfully")
  void shouldUpdateUserSuccessfully() {
    // Arrange
    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("updateduser")
        .password("newpassword")
        .age(30)
        .build();

    User updatedUser = User.builder()
        .id(1L)
        .nickname("updateduser")
        .passwordHash("encodedNewPassword")
        .age(30)
        .build();
    updatedUser.setCreatedAt(testUser.getCreatedAt());
    updatedUser.setUpdatedAt(LocalDateTime.now());

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.encode("newpassword")).thenReturn("encodedNewPassword");
    when(userRepository.save(any(User.class))).thenReturn(updatedUser);

    // Act
    UserResponseDto result = userService.updateUser(1L, updateDto);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getNickname()).isEqualTo("updateduser");
    assertThat(result.getAge()).isEqualTo(30);

    verify(userRepository, times(1)).findById(1L);
    verify(passwordEncoder, times(1)).encode("newpassword");
    verify(userRepository, times(1)).save(any(User.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when updating non-existent user")
  void shouldThrowExceptionWhenUpdatingNonExistentUser() {
    // Arrange
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> userService.updateUser(999L, registrationDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("999");

    verify(userRepository, times(1)).findById(999L);
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should encode new password when updating")
  void shouldEncodeNewPasswordWhenUpdating() {
    // Arrange
    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("testuser")
        .password("newpassword123")
        .age(25)
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.encode("newpassword123")).thenReturn("encodedNewPassword");
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

    // Act
    userService.updateUser(1L, updateDto);

    // Assert
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getPasswordHash()).isEqualTo("encodedNewPassword");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should update all user fields")
  void shouldUpdateAllUserFields() {
    // Arrange
    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("completelynewuser")
        .password("brandnewpassword")
        .age(40)
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.encode("brandnewpassword")).thenReturn("encodedBrandNewPassword");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    userService.updateUser(1L, updateDto);

    // Assert
    verify(userRepository).save(argThat(user ->
        user.getNickname().equals("completelynewuser") &&
            user.getPasswordHash().equals("encodedBrandNewPassword") &&
            user.getAge().equals(40)
    ));
  }

  // ========== Delete User Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should delete user successfully")
  void shouldDeleteUserSuccessfully() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    doNothing().when(userRepository).delete(testUser);

    // Act
    userService.deleteUser(1L);

    // Assert
    verify(userRepository, times(1)).findById(1L);
    verify(userRepository, times(1)).delete(testUser);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when deleting non-existent user")
  void shouldThrowExceptionWhenDeletingNonExistentUser() {
    // Arrange
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> userService.deleteUser(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("999");

    verify(userRepository, times(1)).findById(999L);
    verify(userRepository, never()).delete(any(User.class));
  }

  // ========== Get User Nickname Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should get user nickname successfully")
  void shouldGetUserNicknameSuccessfully() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act
    String nickname = userService.getUserNickname(1L);

    // Assert
    assertThat(nickname).isEqualTo("testuser");
    verify(userRepository, times(1)).findById(1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when getting nickname of non-existent user")
  void shouldThrowExceptionWhenGettingNicknameOfNonExistentUser() {
    // Arrange
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> userService.getUserNickname(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("999");

    verify(userRepository, times(1)).findById(999L);
  }

  // ========== Edge Cases ==========

  @Test
  @Tag("unit")
  @DisplayName("Should handle special characters in nickname")
  void shouldHandleSpecialCharactersInNickname() {
    // Arrange
    UserRegistrationDto specialDto = UserRegistrationDto.builder()
        .nickname("user_123-test")
        .password("password123")
        .age(25)
        .build();

    User specialUser = User.builder()
        .id(4L)
        .nickname("user_123-test")
        .passwordHash("encodedPassword")
        .age(25)
        .build();

    when(userRepository.existsByNickname("user_123-test")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(specialUser);
    when(jwtUtil.generateToken("user_123-test", 4L)).thenReturn("jwt-token");

    // Act
    AuthResponse result = userService.register(specialDto);

    // Assert
    assertThat(result.getUser().getNickname()).isEqualTo("user_123-test");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle very long password")
  void shouldHandleVeryLongPassword() {
    // Arrange
    String longPassword = "a".repeat(1000);
    UserRegistrationDto longPassDto = UserRegistrationDto.builder()
        .nickname("longpassuser")
        .password(longPassword)
        .age(25)
        .build();

    when(userRepository.existsByNickname("longpassuser")).thenReturn(false);
    when(passwordEncoder.encode(longPassword)).thenReturn("encodedLongPassword");
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token");

    // Act
    userService.register(longPassDto);

    // Assert
    verify(passwordEncoder).encode(longPassword);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should preserve user ID when updating")
  void shouldPreserveUserIdWhenUpdating() {
    // Arrange
    UserRegistrationDto updateDto = UserRegistrationDto.builder()
        .nickname("newname")
        .password("newpass")
        .age(30)
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    UserResponseDto result = userService.updateUser(1L, updateDto);

    // Assert
    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return user with timestamps in response")
  void shouldReturnUserWithTimestampsInResponse() {
    // Arrange
    when(userRepository.existsByNickname("testuser")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token");

    // Act
    AuthResponse result = userService.register(registrationDto);

    // Assert
    assertThat(result.getUser().getCreatedAt()).isNotNull();
    assertThat(result.getUser().getUpdatedAt()).isNotNull();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should not expose password in response")
  void shouldNotExposePasswordInResponse() {
    // Arrange
    when(userRepository.existsByNickname("testuser")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token");

    // Act
    AuthResponse result = userService.register(registrationDto);

    // Assert
    assertThat(result.getUser().toString()).doesNotContain("password");
    assertThat(result.getUser().toString()).doesNotContain("encodedPassword");
  }
}
