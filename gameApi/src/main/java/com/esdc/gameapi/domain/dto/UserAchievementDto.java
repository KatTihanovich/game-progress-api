package com.esdc.gameapi.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user and achievement relationship.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievementDto {
  private Long achievementId;
  private String achievementName;
  private String achievementDescription;
  private String createdAt;
}
