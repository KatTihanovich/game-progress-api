package com.esdc.gameapi.service;

import com.esdc.gameapi.domain.dto.AchievementDto;
import com.esdc.gameapi.domain.dto.UserAchievementDto;
import com.esdc.gameapi.domain.entity.Achievement;
import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.domain.entity.Progress;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.domain.entity.UserAchievement;
import com.esdc.gameapi.domain.entity.UserStatistics;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.AchievementRepository;
import com.esdc.gameapi.repository.LevelRepository;
import com.esdc.gameapi.repository.UserAchievementRepository;
import com.esdc.gameapi.repository.UserRepository;
import com.esdc.gameapi.repository.UserStatisticsRepository;
import com.esdc.gameapi.util.AchievementConditionParser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing game achievements and unlock conditions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

  private static final int SECONDS_PER_MINUTE = 60;
  private static final int MINUTES_PER_HOUR = 60;
  private static final int SECONDS_PER_HOUR = 3600;

  private static final int TIME_HOURS_INDEX = 0;
  private static final int TIME_MINUTES_INDEX = 1;
  private static final int TIME_SECONDS_INDEX = 2;

  private static final int MIN_SECONDS_FOR_MINUTE_ROUND_UP = 1;

  private static final String TIME_DELIMITER = ":";

  private final AchievementRepository achievementRepository;
  private final UserAchievementRepository userAchievementRepository;
  private final UserRepository userRepository;
  private final UserStatisticsRepository userStatisticsRepository;
  private final LevelRepository levelRepository;

  /**
   * Gets all achievements.
   */
  @Transactional(readOnly = true)
  public List<AchievementDto> getAllAchievements() {
    log.debug("Fetching all achievements");
    List<AchievementDto> achievements = achievementRepository.findAll()
        .stream()
        .map(this::toDto)
        .collect(Collectors.toList());
    log.debug("Found {} achievements", achievements.size());
    return achievements;
  }

  /**
   * Gets user achievements by ID.
   */
  @Transactional(readOnly = true)
  public List<UserAchievementDto> getAchievementsByUserId(Long userId) {
    log.debug("Fetching achievements for user: {}", userId);
    List<UserAchievementDto> userAchievements = userAchievementRepository.findByUserId(userId)
        .stream()
        .map(this::toUserAchievementDto)
        .collect(Collectors.toList());
    log.debug("User {} has {} achievements", userId, userAchievements.size());
    return userAchievements;
  }

  /**
   * Checks and unlocks achievements after progress.
   */
  @Transactional
  public List<UserAchievementDto> checkAndUnlockAchievements(
      Long userId,
      Long levelId,
      Progress currentProgress
  ) {
    log.info("Checking achievements for userId: {}, levelId: {}", userId, levelId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    log.debug("User found: {}", user.getId());

    UserStatistics stats = userStatisticsRepository.findByUserId(userId).orElse(null);
    if (stats != null) {
      log.debug("User {} statistics - Levels: {}, Enemies: {}, Puzzles: {}, Time: {}",
          userId, stats.getTotalLevelsCompleted(), stats.getTotalKilledEnemies(),
          stats.getTotalSolvedPuzzles(), stats.getTotalTimePlayed());
    } else {
      log.debug("No statistics found for user: {}", userId);
    }

    Progress latestProgress = currentProgress;
    if (latestProgress != null) {
      log.debug("Current progress - Enemies: {}, Puzzles: {}, Time: {}, Stars: {}",
          latestProgress.getKilledEnemiesNumber(), latestProgress.getSolvedPuzzlesNumber(),
          latestProgress.getTimeSpent(), latestProgress.getStars());
    }

    List<Achievement> allAchievements = achievementRepository.findAll();
    log.debug("Total achievements in DB: {}", allAchievements.size());

    Set<Long> unlockedIds = userAchievementRepository.findByUserId(userId).stream()
        .map(ua -> ua.getAchievement().getId())
        .collect(Collectors.toSet());
    log.debug("Already unlocked achievements: {}", unlockedIds.size());

    List<UserAchievementDto> newlyUnlocked = new ArrayList<>();

    for (Achievement achievement : allAchievements) {
      log.debug("Checking achievement: {}", achievement.getAchievementName());

      if (unlockedIds.contains(achievement.getId())) {
        log.debug("Skipped - already unlocked");
        continue;
      }

      AchievementConditionParser condition = AchievementConditionParser.parse(
          achievement.getAchievementDescription());

      if (condition == null) {
        log.warn("Could not parse condition for achievement: {} - Description: {}",
            achievement.getAchievementName(), achievement.getAchievementDescription());
        continue;
      }

      log.debug("Parsed condition - Type: {}, Required value: {}",
          condition.getType(), condition.getRequiredValue());

      boolean conditionMet = checkCondition(condition, stats, latestProgress, levelId);
      log.debug("Condition met: {}", conditionMet);

      if (conditionMet) {
        UserAchievement userAchievement = new UserAchievement(user, achievement);
        userAchievementRepository.save(userAchievement);
        log.info("Achievement unlocked for user {}: {}", userId, achievement.getAchievementName());
        newlyUnlocked.add(toUserAchievementDto(userAchievement));
      }
    }

    log.info("Newly unlocked achievements for user {}: {}", userId, newlyUnlocked.size());
    return newlyUnlocked;
  }

  /**
   * Creates new achievement.
   */
  @Transactional
  public AchievementDto createAchievement(AchievementDto dto) {
    log.info("Creating achievement: {}", dto.getAchievementName());

    Achievement achievement = Achievement.builder()
        .achievementName(dto.getAchievementName())
        .achievementDescription(dto.getAchievementDescription())
        .build();

    Achievement saved = achievementRepository.save(achievement);
    log.info("Achievement created: {}", saved.getId());
    return toDto(saved);
  }

  /**
   * Updates existing achievement.
   */
  @Transactional
  public AchievementDto updateAchievement(Long id, AchievementDto dto) {
    log.info("Updating achievement: {}", id);

    Achievement achievement = achievementRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Achievement", "id", id));

    achievement.setAchievementName(dto.getAchievementName());
    achievement.setAchievementDescription(dto.getAchievementDescription());

    Achievement updated = achievementRepository.save(achievement);
    log.info("Achievement updated: {}", updated.getId());
    return toDto(updated);
  }

  /**
   * Deletes achievement by ID.
   */
  @Transactional
  public void deleteAchievement(Long id) {
    log.info("Deleting achievement: {}", id);

    Achievement achievement = achievementRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Achievement", "id", id));

    achievementRepository.delete(achievement);
    log.info("Achievement deleted: {}", id);
  }

  private boolean checkCondition(AchievementConditionParser condition, UserStatistics stats,
                                 Progress latestProgress, Long levelId) {
    switch (condition.getType()) {
      case TOTAL_LEVELS:
        return stats != null && stats.getTotalLevelsCompleted() >= condition.getRequiredValue();

      case TOTAL_ENEMIES:
        return stats != null && stats.getTotalKilledEnemies() >= condition.getRequiredValue();

      case TOTAL_PUZZLES:
        return stats != null && stats.getTotalSolvedPuzzles() >= condition.getRequiredValue();

      case TOTAL_TIME:
        if (stats == null) {
          return false;
        }
        int totalMinutes = convertTimeToMinutes(stats.getTotalTimePlayed());
        return totalMinutes >= condition.getRequiredValue();

      case LEVEL_ENEMIES:
        return latestProgress != null
            && latestProgress.getKilledEnemiesNumber() >= condition.getRequiredValue();

      case LEVEL_PUZZLES:
        return latestProgress != null
            && latestProgress.getSolvedPuzzlesNumber() >= condition.getRequiredValue();

      case LEVEL_TIME:
        if (latestProgress == null) {
          return false;
        }
        int seconds = convertTimeToSeconds(latestProgress.getTimeSpent());
        return seconds <= condition.getRequiredValue();

      case SPECIFIC_LEVEL:
        return levelId.equals(condition.getRequiredValue().longValue());

      case DEFEAT_BOSS:
        log.debug("Checking DEFEAT_BOSS condition for levelId: {}", levelId);
        if (levelId == null || latestProgress == null) {
          log.debug("DEFEAT_BOSS check failed: levelId or latestProgress is null");
          return false;
        }

        Level level = levelRepository.findById(levelId).orElse(null);
        if (level == null) {
          log.warn("Level not found: {}", levelId);
          return false;
        }

        boolean hasBoss = level.getBossOnLevel() != null && level.getBossOnLevel();
        log.debug("Level {} has boss: {}", levelId, hasBoss);
        return hasBoss;

      case TOTAL_STARS:
        log.debug("Checking TOTAL_STARS condition");
        return stats != null && stats.getTotalStars() >= condition.getRequiredValue();

      case LEVEL_STARS:
        log.debug("Checking LEVEL_STARS condition");
        return latestProgress != null && latestProgress.getStars() >= condition.getRequiredValue();

      default:
        log.warn("Unknown condition type: {}", condition.getType());
        return false;
    }
  }

  private int convertTimeToMinutes(String time) {
    String[] parts = time.split(TIME_DELIMITER);
    int hours = Integer.parseInt(parts[TIME_HOURS_INDEX]);
    int minutes = Integer.parseInt(parts[TIME_MINUTES_INDEX]);
    int seconds = Integer.parseInt(parts[TIME_SECONDS_INDEX]);
    return hours * MINUTES_PER_HOUR + minutes + (seconds > MIN_SECONDS_FOR_MINUTE_ROUND_UP ? 1 : 0);
  }

  private int convertTimeToSeconds(String time) {
    String[] parts = time.split(TIME_DELIMITER);
    int hours = Integer.parseInt(parts[TIME_HOURS_INDEX]);
    int minutes = Integer.parseInt(parts[TIME_MINUTES_INDEX]);
    int seconds = Integer.parseInt(parts[TIME_SECONDS_INDEX]);
    return hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds;
  }

  private AchievementDto toDto(Achievement achievement) {
    return AchievementDto.builder()
        .id(achievement.getId())
        .achievementName(achievement.getAchievementName())
        .achievementDescription(achievement.getAchievementDescription())
        .build();
  }

  private UserAchievementDto toUserAchievementDto(UserAchievement ua) {
    return UserAchievementDto.builder()
        .achievementId(ua.getAchievement().getId())
        .achievementName(ua.getAchievement().getAchievementName())
        .achievementDescription(ua.getAchievement().getAchievementDescription())
        .createdAt(
            ua.getCreatedAt() != null
                ? ua.getCreatedAt().toString()
                : LocalDateTime.now().toString()
        )
        .build();
  }
}
