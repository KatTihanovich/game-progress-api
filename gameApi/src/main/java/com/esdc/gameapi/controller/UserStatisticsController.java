package com.esdc.gameapi.controller;

import com.esdc.gameapi.domain.dto.StarsProgressDto;
import com.esdc.gameapi.domain.dto.UserStatisticsDto;
import com.esdc.gameapi.service.UserStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user statistics operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Statistics", description = "User statistics management")
public class UserStatisticsController {

  private final UserStatisticsService statisticsService;

  /**
   * Gets user statistics by ID.
   */
  @GetMapping("/{userId}")
  @Operation(summary = "Get user statistics",
      description = "Returns overall user statistics: "
          + "completed levels, play time, enemies, puzzles, stars")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics"),
      @ApiResponse(responseCode = "404", description = "Statistics not found")
  })
  public ResponseEntity<UserStatisticsDto> getUserStatistics(
      @Parameter(description = "User ID")
      @PathVariable Long userId) {
    log.debug("Request to get statistics for user: {}", userId);
    return statisticsService.getStatisticsByUserId(userId)
        .map(stats -> {
          log.debug("Statistics found for user: {}", userId);
          return ResponseEntity.ok(stats);
        })
        .orElseGet(() -> {
          log.warn("Statistics not found for user: {}", userId);
          return ResponseEntity.notFound().build();
        });
  }

  /**
   * Recalculates user statistics from progress.
   */
  @PostMapping("/{userId}/recalculate")
  @Operation(summary = "Recalculate user statistics",
      description = "Recalculates all statistics based on progress. "
          + "Automatically called when creating new progress")
  @ApiResponse(responseCode = "200", description = "Statistics recalculated")
  public ResponseEntity<UserStatisticsDto> recalculateStatistics(
      @Parameter(description = "User ID")
      @PathVariable Long userId) {
    log.info("Request to recalculate statistics for user: {}", userId);
    UserStatisticsDto statistics = statisticsService.recalculateUserStatistics(userId);
    log.info("Statistics recalculated successfully for user: {}", userId);
    return ResponseEntity.ok(statistics);
  }

  /**
   * Gets maximum possible stars across all levels.
   */

  @GetMapping("/max-stars")
  @Operation(summary = "Get maximum possible stars",
      description = "Returns the sum of all stars_on_level for all levels")
  @ApiResponse(responseCode = "200", description = "Max stars retrieved")
  public ResponseEntity<Map<String, Integer>> getMaxPossibleStars() {
    log.debug("Request to get maximum possible stars");
    int maxStars = statisticsService.getMaxPossibleStars();
    Map<String, Integer> response = new HashMap<>();
    response.put("maxPossibleStars", maxStars);
    log.debug("Maximum possible stars: {}", maxStars);
    return ResponseEntity.ok(response);
  }

  /**
   * Gets user stars progress with percentage.
   */
  @GetMapping("/{userId}/stars-progress")
  @Operation(summary = "Get stars progress",
      description = "Returns current stars, maximum, and progress percentage")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Progress retrieved"),
      @ApiResponse(responseCode = "404", description = "Statistics not found")
  })
  public ResponseEntity<StarsProgressDto> getStarsProgress(
      @Parameter(description = "User ID")
      @PathVariable Long userId) {
    log.debug("Request to get stars progress for user: {}", userId);

    try {
      StarsProgressDto progress = statisticsService.getStarsProgress(userId);
      return ResponseEntity.ok(progress);
    } catch (Exception e) {
      log.warn("Statistics not found for user: {}", userId);
      return ResponseEntity.notFound().build();
    }
  }
}
