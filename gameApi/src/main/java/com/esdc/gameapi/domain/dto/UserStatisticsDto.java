package com.esdc.gameapi.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user game statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDto {
  private Integer totalLevelsCompleted;
  private String totalTimePlayed;
  private Integer totalKilledEnemies;
  private Integer totalSolvedPuzzles;
  private Integer totalStars;
}
