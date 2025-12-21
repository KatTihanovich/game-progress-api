package com.esdc.gameapi.service;

import com.esdc.gameapi.domain.dto.StarsProgressDto;
import com.esdc.gameapi.domain.dto.UserStatisticsDto;
import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.domain.entity.Progress;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.domain.entity.UserStatistics;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.LevelRepository;
import com.esdc.gameapi.repository.ProgressRepository;
import com.esdc.gameapi.repository.UserRepository;
import com.esdc.gameapi.repository.UserStatisticsRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user game statistics and progress calculations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatisticsService {

  private static final int SECONDS_PER_MINUTE = 60;
  private static final int SECONDS_PER_HOUR = 3600;

  private static final String TIME_DELIMITER = ":";
  private static final int TIME_HOURS_INDEX = 0;
  private static final int TIME_MINUTES_INDEX = 1;
  private static final int TIME_SECONDS_INDEX = 2;
  private static final String TIME_FORMAT = "%02d:%02d:%02d";

  private static final double PERCENTAGE_MULTIPLIER = 100.0;
  private static final double ROUNDING_PRECISION = 100.0;

  private static final int DEFAULT_STARS = 0;
  private static final int DEFAULT_TIME_SECONDS = 0;
  private static final int DEFAULT_MAX_STARS = 0;
  private static final double DEFAULT_PERCENTAGE = 0.0;

  private final UserStatisticsRepository statisticsRepository;
  private final ProgressRepository progressRepository;
  private final UserRepository userRepository;
  private final LevelRepository levelRepository;

  /**
   * Gets user statistics by ID.
   */
  @Transactional(readOnly = true)
  public Optional<UserStatisticsDto> getStatisticsByUserId(Long userId) {
    return statisticsRepository.findByUserId(userId)
        .map(this::toDto);
  }

  /**
   * Recalculates and saves user statistics.
   */
  @Transactional
  public UserStatisticsDto recalculateUserStatistics(Long userId) {
    log.info("Recalculating statistics for user {}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    List<Progress> allProgress = progressRepository.findByUserId(userId);

    int totalLevelsCompleted = (int) allProgress.stream()
        .map(progress -> progress.getLevel().getId())
        .distinct()
        .count();

    int totalKilledEnemies = allProgress.stream()
        .mapToInt(Progress::getKilledEnemiesNumber)
        .sum();

    int totalSolvedPuzzles = allProgress.stream()
        .mapToInt(Progress::getSolvedPuzzlesNumber)
        .sum();

    int totalStars = calculateTotalStars(userId, allProgress);

    String totalTimePlayed = calculateTotalTime(allProgress);

    UserStatistics statistics = statisticsRepository.findByUserId(userId)
        .orElseGet(() -> new UserStatistics(user));

    statistics.setTotalLevelsCompleted(totalLevelsCompleted);
    statistics.setTotalTimePlayed(totalTimePlayed);
    statistics.setTotalKilledEnemies(totalKilledEnemies);
    statistics.setTotalSolvedPuzzles(totalSolvedPuzzles);
    statistics.setTotalStars(totalStars);

    UserStatistics savedStatistics = statisticsRepository.save(statistics);
    log.info("Stats updated for user{}:{}levels,{}stars", userId, totalLevelsCompleted, totalStars);

    return toDto(savedStatistics);
  }

  /**
   * Calculates total stars across all levels.
   */
  private int calculateTotalStars(Long userId, List<Progress> allProgress) {
    int totalStars = DEFAULT_STARS;

    var progressByLevel = allProgress.stream()
        .collect(Collectors.groupingBy(
            progress -> progress.getLevel().getId()
        ));

    for (var entry : progressByLevel.entrySet()) {
      List<Progress> levelProgressList = entry.getValue();
      Level level = levelProgressList.getFirst().getLevel();

      int maxStarsForLevel = level.getStarsOnLevel();

      int bestAttemptStars = levelProgressList.stream()
          .mapToInt(Progress::getStars)
          .max()
          .orElse(DEFAULT_STARS);

      int cappedStars = Math.min(bestAttemptStars, maxStarsForLevel);
      totalStars += cappedStars;

      if (bestAttemptStars > maxStarsForLevel) {
        log.warn("User {} has {} stars on level {} (max: {}). Capping to max.",
            userId, bestAttemptStars, level.getId(), maxStarsForLevel);
      }
    }

    return totalStars;
  }

  /**
   * Gets maximum possible stars for all levels.
   */
  @Transactional(readOnly = true)
  public int getMaxPossibleStars() {
    List<Level> allLevels = levelRepository.findAll();
    return allLevels.stream()
        .mapToInt(Level::getStarsOnLevel)
        .sum();
  }

  /**
   * Calculates stars progress percentage.
   */
  @Transactional(readOnly = true)
  public double getStarsProgressPercentage(Long userId) {
    UserStatistics statistics = statisticsRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Statistics", "userId", userId));

    int maxPossibleStars = getMaxPossibleStars();
    if (maxPossibleStars == DEFAULT_MAX_STARS) {
      return DEFAULT_PERCENTAGE;
    }

    double percentage = (statistics.getTotalStars() * PERCENTAGE_MULTIPLIER) / maxPossibleStars;
    return roundToTwoDecimals(percentage);
  }

  /**
   * Gets detailed stars progress info.
   */
  @Transactional(readOnly = true)
  public StarsProgressDto getStarsProgress(Long userId) {
    UserStatistics statistics = statisticsRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Statistics", "userId", userId));

    int maxStars = getMaxPossibleStars();
    double percentage = getStarsProgressPercentage(userId);

    log.debug("User {} stars progress: {}/{} ({}%)",
        userId, statistics.getTotalStars(), maxStars, percentage);

    return StarsProgressDto.builder()
        .currentStars(statistics.getTotalStars())
        .maxPossibleStars(maxStars)
        .progressPercentage(percentage)
        .build();
  }

  private double roundToTwoDecimals(double value) {
    return Math.round(value * ROUNDING_PRECISION) / ROUNDING_PRECISION;
  }

  private String calculateTotalTime(List<Progress> progressList) {
    int totalSeconds = DEFAULT_TIME_SECONDS;

    for (Progress progress : progressList) {
      String timeSpent = progress.getTimeSpent();
      if (timeSpent != null && !timeSpent.isEmpty()) {
        totalSeconds += convertTimeToSeconds(timeSpent);
      }
    }

    return convertSecondsToTime(totalSeconds);
  }

  private int convertTimeToSeconds(String time) {
    try {
      String[] parts = time.split(TIME_DELIMITER);
      int hours = Integer.parseInt(parts[TIME_HOURS_INDEX]);
      int minutes = Integer.parseInt(parts[TIME_MINUTES_INDEX]);
      int seconds = Integer.parseInt(parts[TIME_SECONDS_INDEX]);
      return hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds;
    } catch (Exception e) {
      log.warn("Failed to parse time: {}", time);
      return DEFAULT_TIME_SECONDS;
    }
  }

  private String convertSecondsToTime(int totalSeconds) {
    int hours = totalSeconds / SECONDS_PER_HOUR;
    int minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
    int seconds = totalSeconds % SECONDS_PER_MINUTE;

    return String.format(TIME_FORMAT, hours, minutes, seconds);
  }

  private UserStatisticsDto toDto(UserStatistics stats) {
    return UserStatisticsDto.builder()
        .totalLevelsCompleted(stats.getTotalLevelsCompleted())
        .totalTimePlayed(stats.getTotalTimePlayed())
        .totalKilledEnemies(stats.getTotalKilledEnemies())
        .totalSolvedPuzzles(stats.getTotalSolvedPuzzles())
        .totalStars(stats.getTotalStars())
        .build();
  }
}
