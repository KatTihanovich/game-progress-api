package com.esdc.gameApi.domain.dto;

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
}
