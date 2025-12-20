package com.esdc.gameapi.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
  private Long id;
  private String nickname;
  private Integer age;
  private String createdAt;
  private String updatedAt;
}