package com.esdc.gameapi.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelDto {
  private Long id;
  private String levelName;
  private Boolean bossOnLevel;
  private Integer starsOnLevel;
  private String createdAt;
  private String updatedAt;
}
