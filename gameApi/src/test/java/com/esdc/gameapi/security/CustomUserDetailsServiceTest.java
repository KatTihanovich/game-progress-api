package com.esdc.gameapi.security;

import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Custom User Details Service Tests")
class CustomUserDetailsServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private CustomUserDetailsService userDetailsService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .nickname("testUser")
        .passwordHash("hashedPassword")
        .build();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should load user by username successfully")
  void shouldLoadUserByUsername() {
    // Arrange
    when(userRepository.findByNickname("testUser")).thenReturn(Optional.of(testUser));

    // Act
    UserDetails userDetails = userDetailsService.loadUserByUsername("testUser");

    // Assert
    assertThat(userDetails).isNotNull();
    assertThat(userDetails.getUsername()).isEqualTo("testUser");
    assertThat(userDetails.getPassword()).isEqualTo("hashedPassword");
    verify(userRepository, times(1)).findByNickname("testUser");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when user not found")
  void shouldThrowExceptionWhenUserNotFound() {
    // Arrange
    when(userRepository.findByNickname("nonExistent")).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonExistent"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("User not found: nonExistent");

    verify(userRepository, times(1)).findByNickname("nonExistent");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return CustomUserDetails instance")
  void shouldReturnCustomUserDetails() {
    // Arrange
    when(userRepository.findByNickname("testUser")).thenReturn(Optional.of(testUser));

    // Act
    UserDetails userDetails = userDetailsService.loadUserByUsername("testUser");

    // Assert
    assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
    assertThat(((CustomUserDetails) userDetails).getUserId()).isEqualTo(1L);
  }
}
