package com.esdc.gameapi.security;

import com.esdc.gameapi.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("Custom User Details Tests")
class CustomUserDetailsTest {

  private User testUser;
  private CustomUserDetails userDetails;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .nickname("testUser")
        .passwordHash("hashedPassword123")
        .build();

    userDetails = new CustomUserDetails(testUser);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return correct username")
  void shouldReturnCorrectUsername() {
    // Act & Assert
    assertThat(userDetails.getUsername()).isEqualTo("testUser");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return correct password")
  void shouldReturnCorrectPassword() {
    // Act & Assert
    assertThat(userDetails.getPassword()).isEqualTo("hashedPassword123");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return correct user ID")
  void shouldReturnCorrectUserId() {
    // Act & Assert
    assertThat(userDetails.getUserId()).isEqualTo(1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return empty authorities")
  void shouldReturnEmptyAuthorities() {
    // Act & Assert
    assertThat(userDetails.getAuthorities()).isEmpty();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return account non expired as true")
  void shouldReturnAccountNonExpired() {
    // Act & Assert
    assertThat(userDetails.isAccountNonExpired()).isTrue();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return account non locked as true")
  void shouldReturnAccountNonLocked() {
    // Act & Assert
    assertThat(userDetails.isAccountNonLocked()).isTrue();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return credentials non expired as true")
  void shouldReturnCredentialsNonExpired() {
    // Act & Assert
    assertThat(userDetails.isCredentialsNonExpired()).isTrue();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return enabled as true")
  void shouldReturnEnabled() {
    // Act & Assert
    assertThat(userDetails.isEnabled()).isTrue();
  }
}
