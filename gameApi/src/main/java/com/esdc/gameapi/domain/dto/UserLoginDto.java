package com.esdc.gameapi.domain.dto;

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
  private String nickname;
  private String password;
}
