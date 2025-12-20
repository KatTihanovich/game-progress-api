package com.esdc.gameapi.service;

import com.esdc.gameapi.domain.dto.ProgressDto;
import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.domain.entity.Progress;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.LevelRepository;
import com.esdc.gameapi.repository.ProgressRepository;
import com.esdc.gameapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

  // Date/Time format constants
  private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
  private static final String TIME_VALIDATION_REGEX = "^([0-9]{2}):([0-5][0-9]):([0-5][0-9])$";

  // Validation constants
  private static final int MIN_STARS = 0;

  private final ProgressRepository progressRepository;
  private final UserRepository userRepository;
  private final LevelRepository levelRepository;
  private final UserStatisticsService userStatisticsService;
  private final AchievementService achievementService;

  @Transactional
  public ProgressDto createProgress(Long userId, ProgressDto request) {
    log.info("Creating progress for user: {}, level: {}", userId, request.getLevelId());

    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("User not found: {}", userId);
          return new ResourceNotFoundException("User", "id", userId);
        });

    Level level = levelRepository.findById(request.getLevelId())
        .orElseThrow(() -> {
          log.warn("Level not found: {}", request.getLevelId());
          return new ResourceNotFoundException("Level", "id", request.getLevelId());
        });

    try {
      validateTimeFormat(request.getTimeSpent());
      validateStars(request.getStars(), level.getStarsOnLevel());
    } catch (IllegalArgumentException e) {
      log.warn("Validation failed for user {}: {}", userId, e.getMessage());
      throw e;
    }

    Progress progress = new Progress();
    progress.setUser(user);
    progress.setLevel(level);
    progress.setKilledEnemiesNumber(request.getKilledEnemiesNumber());
    progress.setSolvedPuzzlesNumber(request.getSolvedPuzzlesNumber());
    progress.setTimeSpent(request.getTimeSpent());
    progress.setStars(request.getStars());

    Progress savedProgress = progressRepository.save(progress);
    log.info("Progress saved for user: {}, level: {}, stars: {}, enemies: {}, puzzles: {}",
        userId, level.getId(), savedProgress.getStars(),
        savedProgress.getKilledEnemiesNumber(), savedProgress.getSolvedPuzzlesNumber());

    userStatisticsService.recalculateUserStatistics(user.getId());
    achievementService.checkAndUnlockAchievements(user.getId(), level.getId(), savedProgress);

    return toDto(savedProgress);
  }

  @Transactional(readOnly = true)
  public ProgressDto getLatestProgressByUserAndLevel(Long userId, Long levelId) {
    log.debug("Fetching latest progress for user: {}, level: {}", userId, levelId);

    userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("User not found: {}", userId);
          return new ResourceNotFoundException("User", "id", userId);
        });

    Level level = levelRepository.findById(levelId)
        .orElseThrow(() -> {
          log.warn("Level not found: {}", levelId);
          return new ResourceNotFoundException("Level", "id", levelId);
        });

    List<Progress> progressList = progressRepository.findByUserIdAndLevelId(userId, levelId);

    Progress latestProgress = progressList.stream()
        .max((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()))
        .orElseThrow(() -> {
          log.warn("No progress found for user {} on level {}", userId, levelId);
          return new ResourceNotFoundException("Progress", "userId and levelId", userId + ", " + levelId);
        });

    log.debug("Latest progress found for user {} on level {}", userId, levelId);
    return toDto(latestProgress, level.getLevelName());
  }

  @Transactional(readOnly = true)
  public List<ProgressDto> getProgressByUserId(Long userId) {
    log.debug("Fetching all progress for user: {}", userId);
    List<ProgressDto> progressList = progressRepository.findByUserId(userId).stream()
        .map(progress -> toDto(progress, progress.getLevel().getLevelName()))
        .toList();
    log.debug("Found {} progress records for user: {}", progressList.size(), userId);
    return progressList;
  }

  @Transactional(readOnly = true)
  public Integer getTotalStarsByUserAndLevel(Long userId, Long levelId) {
    log.debug("Calculating total stars for user: {}, level: {}", userId, levelId);
    Integer totalStars = progressRepository.getTotalStarsByUserIdAndLevelId(userId, levelId);
    log.debug("Total stars for user {} on level {}: {}", userId, levelId, totalStars);
    return totalStars;
  }

  private ProgressDto toDto(Progress progress) {
    return toDto(progress, null);
  }

  private ProgressDto toDto(Progress progress, String levelName) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    return ProgressDto.builder()
        .levelId(progress.getLevel().getId())
        .killedEnemiesNumber(progress.getKilledEnemiesNumber())
        .solvedPuzzlesNumber(progress.getSolvedPuzzlesNumber())
        .timeSpent(progress.getTimeSpent())
        .stars(progress.getStars())
        .build();
  }

  private void validateStars(Integer stars, Integer maxStars) {
    if (stars == null) {
      throw new IllegalArgumentException("Stars cannot be null");
    }
    if (stars < MIN_STARS) {
      throw new IllegalArgumentException("Stars cannot be negative");
    }
    if (stars > maxStars) {
      log.warn("Stars validation failed: {} exceeds max {}", stars, maxStars);
      throw new IllegalArgumentException(
          String.format("Stars (%d) cannot exceed maximum stars on level (%d)", stars, maxStars));
    }
  }

  private void validateTimeFormat(String timeSpent) {
    if (!timeSpent.matches(TIME_VALIDATION_REGEX)) {
      log.warn("Invalid time format: {}", timeSpent);
      throw new IllegalArgumentException("Invalid time format. Expected HH:MM:SS");
    }
  }
}
