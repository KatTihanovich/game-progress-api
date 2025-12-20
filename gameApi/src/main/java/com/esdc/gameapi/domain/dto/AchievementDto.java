package com.esdc.gameapi.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for achievements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDto {
  private Long id;
  private String achievementName;
  private String achievementDescription;
}
