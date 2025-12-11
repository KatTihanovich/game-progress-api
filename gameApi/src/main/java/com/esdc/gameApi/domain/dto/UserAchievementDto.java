package com.esdc.gameApi.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
