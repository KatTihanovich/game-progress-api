package com.esdc.gameApi.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDto {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Level ID is required")
    private Long levelId;

    @NotNull(message = "Killed enemies number is required")
    @PositiveOrZero(message = "Killed enemies must be zero or positive")
    private Integer killedEnemiesNumber;

    @NotNull(message = "Solved puzzles number is required")
    @PositiveOrZero(message = "Solved puzzles must be zero or positive")
    private Integer solvedPuzzlesNumber;

    @NotNull(message = "Time spent is required")
    @Pattern(regexp = "^([0-9]{2}):([0-5][0-9]):([0-5][0-9])$",
            message = "Time must be in HH:MM:SS format")
    private String timeSpent;
}
