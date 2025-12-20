package com.esdc.gameapi.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for progress.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDto {
  private Long userId;
  private Long levelId;

  @Min(value = 0, message = "Killed enemies must be non-negative")
  private Integer killedEnemiesNumber;

  @Min(value = 0, message = "Solved puzzles must be non-negative")
  private Integer solvedPuzzlesNumber;

  @Pattern(regexp = "^([0-9]{2}):([0-5][0-9]):([0-5][0-9])$")
  private String timeSpent;

  @Min(0)
  private Integer stars;
}
