package com.esdc.gameapi.controller;

import com.esdc.gameapi.domain.dto.ProgressDto;
import com.esdc.gameapi.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user progress operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Progress", description = "User progress management")
public class ProgressController {

  private final ProgressService progressService;

  /**
   * Gets all user progress across levels.
   */
  @GetMapping("/{userId}")
  @Operation(summary = "Get user progress",
      description = "Returns all user progress across all levels")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved progress")
  public ResponseEntity<List<ProgressDto>> getProgressByUser(
      @Parameter(description = "User ID")
      @PathVariable Long userId) {
    log.debug("Request to get progress for user: {}", userId);
    return ResponseEntity.ok(progressService.getProgressByUserId(userId));
  }

  /**
   * Gets latest progress for user on specific level.
   */
  @GetMapping("/{userId}/level/{levelId}/latest")
  @Operation(summary = "Get latest progress for user on specific level",
      description = "Returns the most recent progress record for a user on a given level")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved latest progress"),
      @ApiResponse(responseCode = "404", description = "User, level, or progress not found")
  })
  public ResponseEntity<ProgressDto> getLatestProgressByUserAndLevel(
      @Parameter(description = "User ID", required = true)
      @PathVariable Long userId,

      @Parameter(description = "Level ID", required = true)
      @PathVariable Long levelId) {

    log.debug("Request to get latest progress for user: {}, level: {}", userId, levelId);
    ProgressDto progress = progressService.getLatestProgressByUserAndLevel(userId, levelId);
    return ResponseEntity.ok(progress);
  }

  /**
   * Gets total stars for user on specific level.
   */
  @GetMapping("/{userId}/level/{levelId}/total-stars")
  @Operation(summary = "Get total stars for user on specific level",
      description =
          "Calculates and returns the total number of stars earned by a user on a specific level")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully calculated total stars"),
      @ApiResponse(responseCode = "404", description = "User or level not found")
  })
  public ResponseEntity<Integer> getTotalStarsByUserAndLevel(
      @Parameter(description = "User ID", required = true)
      @PathVariable Long userId,

      @Parameter(description = "Level ID", required = true)
      @PathVariable Long levelId) {

    log.debug("Request to get total stars for user: {}, level: {}", userId, levelId);
    Integer totalStars = progressService.getTotalStarsByUserAndLevel(userId, levelId);
    return ResponseEntity.ok(totalStars);
  }

  /**
   * Creates new progress record for user on level.
   */
  @PostMapping
  @Operation(summary = "Create new progress",
      description = "Creates a progress record for a user on a level")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Progress successfully created"),
      @ApiResponse(responseCode = "400", description = "Invalid data or stars limit exceeded"),
      @ApiResponse(responseCode = "404", description = "User or level not found")
  })
  public ResponseEntity<ProgressDto> createProgress(
      @Parameter(description = "User ID", required = true)
      @RequestParam Long userId,

      @Parameter(description = "Progress data", required = true)
      @RequestBody @Valid ProgressDto request) {

    log.info("Request to create progress for user: {}, level: {}", userId, request.getLevelId());
    ProgressDto created = progressService.createProgress(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
