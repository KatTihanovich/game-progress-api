package com.esdc.gameapi.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for stars progress.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StarsProgressDto {
  private int currentStars;
  private int maxPossibleStars;
  private double progressPercentage;
}
