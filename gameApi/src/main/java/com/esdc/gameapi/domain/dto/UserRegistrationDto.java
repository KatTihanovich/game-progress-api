package com.esdc.gameapi.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDto {

  @NotBlank(message = "Nickname must not be blank")
  @Size(max = 255, message = "Nickname must be at most 255 characters")
  private String nickname;

  @NotBlank(message = "Password must not be blank")
  @Size(min = 4, message = "Password must be at least 4 characters")
  private String password;

  @NotNull(message = "Age is required")
  @Min(value = 10, message = "Age must be at least 10")
  private Integer age;
}
