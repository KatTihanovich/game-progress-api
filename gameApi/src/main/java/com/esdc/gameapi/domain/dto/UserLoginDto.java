package com.esdc.gameapi.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginDto {
  @NotBlank(message = "Password must not be blank")
  @Size(min = 4, message = "Password must be at least 4 characters")
  private String nickname;
  @NotBlank(message = "Password must not be blank")
  @Size(min = 4, message = "Password must be at least 4 characters")
  private String password;
}
