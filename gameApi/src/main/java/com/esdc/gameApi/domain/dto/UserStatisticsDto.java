package com.esdc.gameApi.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDto {
    private Integer totalLevelsCompleted;
    private String totalTimePlayed;
    private Integer totalKilledEnemies;
    private Integer totalSolvedPuzzles;
}