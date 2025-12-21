package com.esdc.gameapi.service.integration;

import com.esdc.gameapi.domain.dto.ProgressDto;
import com.esdc.gameapi.domain.dto.UserStatisticsDto;
import com.esdc.gameapi.domain.entity.Achievement;
import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.domain.entity.Progress;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.*;
import com.esdc.gameapi.service.ProgressService;
import com.esdc.gameapi.service.UserStatisticsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=mySecretKeyForTestingPurposesOnlyMustBeAtLeast256BitsLong",
    "jwt.expiration=3600000",
    "admin.password=testAdminPassword123"
})
@DisplayName("Progress Service Integration Tests")
class ProgressServiceIntegrationTest {

  @Autowired
  private ProgressService progressService;

  @Autowired
  private ProgressRepository progressRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private LevelRepository levelRepository;

  @Autowired
  private UserStatisticsRepository statisticsRepository;

  @Autowired
  private UserStatisticsService statisticsService;

  @Autowired
  private AchievementRepository achievementRepository;

  @Autowired
  private UserAchievementRepository userAchievementRepository;

  private User testUser;
  private Level testLevel1;
  private Level testLevel2;

  @BeforeEach
  void setUp() {
    // Очистка всех таблиц в правильном порядке
    userAchievementRepository.deleteAll();
    progressRepository.deleteAll();
    statisticsRepository.deleteAll();
    achievementRepository.deleteAll();
    levelRepository.deleteAll();
    userRepository.deleteAll();

    // Создание тестовых данных
    testUser = userRepository.save(User.builder()
        .nickname("testuser")
        .passwordHash("hash")
        .age(25)
        .build());

    testLevel1 = levelRepository.save(Level.builder()
        .levelName("Level 1")
        .starsOnLevel(3)
        .bossOnLevel(false)
        .build());

    testLevel2 = levelRepository.save(Level.builder()
        .levelName("Level 2")
        .starsOnLevel(5)
        .bossOnLevel(true)
        .build());

    // Создание тестового достижения
    achievementRepository.save(Achievement.builder()
        .achievementName("First Steps")
        .achievementDescription("Complete level 1")
        .build());
  }

  @AfterEach
  void tearDown() {
    userAchievementRepository.deleteAll();
    progressRepository.deleteAll();
    statisticsRepository.deleteAll();
    achievementRepository.deleteAll();
    levelRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ========== Create Progress Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should create progress and persist to database")
  void shouldCreateProgressAndPersistToDatabase() {
    // Arrange
    ProgressDto progressDto = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .killedEnemiesNumber(10)
        .solvedPuzzlesNumber(5)
        .timeSpent("00:30:15")
        .stars(2)
        .build();

    // Act
    ProgressDto result = progressService.createProgress(testUser.getId(), progressDto);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getLevelId()).isEqualTo(testLevel1.getId());
    assertThat(result.getStars()).isEqualTo(2);
    assertThat(result.getKilledEnemiesNumber()).isEqualTo(10);
    assertThat(result.getSolvedPuzzlesNumber()).isEqualTo(5);
    assertThat(result.getTimeSpent()).isEqualTo("00:30:15");

    // Verify persistence
    List<Progress> progressList = progressRepository.findByUserId(testUser.getId());
    assertThat(progressList).hasSize(1);
    assertThat(progressList.get(0).getStars()).isEqualTo(2);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should create multiple progress entries for same level")
  void shouldCreateMultipleProgressEntriesForSameLevel() {
    // Arrange
    ProgressDto progress1 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(1)
        .timeSpent("00:40:00")
        .killedEnemiesNumber(5)
        .solvedPuzzlesNumber(2)
        .build();

    ProgressDto progress2 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(3)
        .timeSpent("00:25:00")
        .killedEnemiesNumber(10)
        .solvedPuzzlesNumber(5)
        .build();

    // Act
    progressService.createProgress(testUser.getId(), progress1);
    progressService.createProgress(testUser.getId(), progress2);

    // Assert
    List<Progress> progressList = progressRepository.findByUserIdAndLevelId(
        testUser.getId(), testLevel1.getId());
    assertThat(progressList).hasSize(2);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when user not found")
  void shouldThrowExceptionWhenUserNotFound() {
    // Arrange
    ProgressDto progressDto = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(2)
        .timeSpent("00:30:00")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(999L, progressDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("999");

    assertThat(progressRepository.count()).isEqualTo(0);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when level not found")
  void shouldThrowExceptionWhenLevelNotFound() {
    // Arrange
    ProgressDto progressDto = ProgressDto.builder()
        .levelId(999L)
        .stars(2)
        .timeSpent("00:30:00")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(testUser.getId(), progressDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Level")
        .hasMessageContaining("999");

    assertThat(progressRepository.count()).isEqualTo(0);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should validate invalid time format")
  void shouldValidateInvalidTimeFormat() {
    // Arrange
    ProgressDto progressDto = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(2)
        .timeSpent("invalid-time")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(testUser.getId(), progressDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid time format");

    assertThat(progressRepository.count()).isEqualTo(0);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should validate stars exceed level maximum")
  void shouldValidateStarsExceedLevelMaximum() {
    // Arrange
    ProgressDto progressDto = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(10)  // Level max is 3
        .timeSpent("00:30:00")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(testUser.getId(), progressDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot exceed maximum stars");

    assertThat(progressRepository.count()).isEqualTo(0);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should validate negative stars")
  void shouldValidateNegativeStars() {
    // Arrange
    ProgressDto progressDto = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(-1)
        .timeSpent("00:30:00")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(testUser.getId(), progressDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Stars cannot be negative");

    assertThat(progressRepository.count()).isEqualTo(0);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should validate null stars")
  void shouldValidateNullStars() {
    // Arrange
    ProgressDto progressDto = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(null)
        .timeSpent("00:30:00")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(testUser.getId(), progressDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Stars cannot be null");

    assertThat(progressRepository.count()).isEqualTo(0);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should accept zero stars")
  void shouldAcceptZeroStars() {
    // Arrange
    ProgressDto progressDto = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(0)
        .timeSpent("00:30:00")
        .killedEnemiesNumber(0)
        .solvedPuzzlesNumber(0)
        .build();

    // Act
    ProgressDto result = progressService.createProgress(testUser.getId(), progressDto);

    // Assert
    assertThat(result.getStars()).isEqualTo(0);
    assertThat(progressRepository.count()).isEqualTo(1);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should accept maximum stars for level")
  void shouldAcceptMaximumStarsForLevel() {
    // Arrange
    ProgressDto progressDto = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(3)  // Maximum for level1
        .timeSpent("00:20:00")
        .killedEnemiesNumber(15)
        .solvedPuzzlesNumber(10)
        .build();

    // Act
    ProgressDto result = progressService.createProgress(testUser.getId(), progressDto);

    // Assert
    assertThat(result.getStars()).isEqualTo(3);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should trigger statistics recalculation after creating progress")
  void shouldTriggerStatisticsRecalculationAfterCreatingProgress() {
    // Arrange
    ProgressDto progressDto = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(2)
        .timeSpent("00:30:00")
        .killedEnemiesNumber(10)
        .solvedPuzzlesNumber(5)
        .build();

    // Act
    progressService.createProgress(testUser.getId(), progressDto);

    // Assert - Statistics should be created/updated
    Optional<UserStatisticsDto> stats = statisticsService.getStatisticsByUserId(testUser.getId());
    assertThat(stats).isPresent();
    assertThat(stats.get().getTotalLevelsCompleted()).isEqualTo(1);
    assertThat(stats.get().getTotalStars()).isEqualTo(2);
    assertThat(stats.get().getTotalKilledEnemies()).isEqualTo(10);
    assertThat(stats.get().getTotalSolvedPuzzles()).isEqualTo(5);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should update statistics when multiple progress entries added")
  void shouldUpdateStatisticsWhenMultipleProgressEntriesAdded() {
    // Arrange
    ProgressDto progress1 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(2)
        .timeSpent("00:30:00")
        .killedEnemiesNumber(10)
        .solvedPuzzlesNumber(5)
        .build();

    ProgressDto progress2 = ProgressDto.builder()
        .levelId(testLevel2.getId())
        .stars(4)
        .timeSpent("01:00:00")
        .killedEnemiesNumber(20)
        .solvedPuzzlesNumber(10)
        .build();

    // Act
    progressService.createProgress(testUser.getId(), progress1);
    progressService.createProgress(testUser.getId(), progress2);

    // Assert
    Optional<UserStatisticsDto> stats = statisticsService.getStatisticsByUserId(testUser.getId());
    assertThat(stats).isPresent();
    assertThat(stats.get().getTotalLevelsCompleted()).isEqualTo(2);
    assertThat(stats.get().getTotalStars()).isEqualTo(6); // 2 + 4
    assertThat(stats.get().getTotalKilledEnemies()).isEqualTo(30); // 10 + 20
    assertThat(stats.get().getTotalSolvedPuzzles()).isEqualTo(15); // 5 + 10
  }

  // ========== Get Latest Progress Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should get latest progress by user and level from database")
  void shouldGetLatestProgressByUserAndLevelFromDatabase() throws InterruptedException {
    // Arrange
    ProgressDto progress1 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(1)
        .timeSpent("00:40:00")
        .killedEnemiesNumber(5)
        .solvedPuzzlesNumber(2)
        .build();

    progressService.createProgress(testUser.getId(), progress1);
    Thread.sleep(100); // Ensure different timestamps

    ProgressDto progress2 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(3)
        .timeSpent("00:25:00")
        .killedEnemiesNumber(12)
        .solvedPuzzlesNumber(6)
        .build();

    progressService.createProgress(testUser.getId(), progress2);

    // Act
    ProgressDto result = progressService.getLatestProgressByUserAndLevel(
        testUser.getId(), testLevel1.getId());

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getStars()).isEqualTo(3);
    assertThat(result.getKilledEnemiesNumber()).isEqualTo(12);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when no progress found for user and level")
  void shouldThrowExceptionWhenNoProgressFoundForUserAndLevel() {
    // Act & Assert
    assertThatThrownBy(() -> progressService.getLatestProgressByUserAndLevel(
        testUser.getId(), testLevel1.getId()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Progress");
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when user not found for latest progress")
  void shouldThrowExceptionWhenUserNotFoundForLatestProgress() {
    // Act & Assert
    assertThatThrownBy(() -> progressService.getLatestProgressByUserAndLevel(999L, testLevel1.getId()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User");
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when level not found for latest progress")
  void shouldThrowExceptionWhenLevelNotFoundForLatestProgress() {
    // Act & Assert
    assertThatThrownBy(() -> progressService.getLatestProgressByUserAndLevel(testUser.getId(), 999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Level");
  }

  // ========== Get Progress By User Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should get all progress by user ID from database")
  void shouldGetAllProgressByUserIdFromDatabase() {
    // Arrange
    ProgressDto progress1 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(2)
        .timeSpent("00:30:00")
        .killedEnemiesNumber(10)
        .solvedPuzzlesNumber(5)
        .build();

    ProgressDto progress2 = ProgressDto.builder()
        .levelId(testLevel2.getId())
        .stars(4)
        .timeSpent("01:00:00")
        .killedEnemiesNumber(20)
        .solvedPuzzlesNumber(10)
        .build();

    progressService.createProgress(testUser.getId(), progress1);
    progressService.createProgress(testUser.getId(), progress2);

    // Act
    List<ProgressDto> result = progressService.getProgressByUserId(testUser.getId());

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result).extracting(ProgressDto::getStars)
        .containsExactlyInAnyOrder(2, 4);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should return empty list when user has no progress")
  void shouldReturnEmptyListWhenUserHasNoProgress() {
    // Act
    List<ProgressDto> result = progressService.getProgressByUserId(testUser.getId());

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle multiple attempts on same level")
  void shouldHandleMultipleAttemptsOnSameLevel() {
    // Arrange
    ProgressDto progress1 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(1)
        .timeSpent("00:40:00")
        .killedEnemiesNumber(5)
        .solvedPuzzlesNumber(2)
        .build();

    ProgressDto progress2 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(3)
        .timeSpent("00:25:00")
        .killedEnemiesNumber(10)
        .solvedPuzzlesNumber(5)
        .build();

    progressService.createProgress(testUser.getId(), progress1);
    progressService.createProgress(testUser.getId(), progress2);

    // Act
    List<ProgressDto> result = progressService.getProgressByUserId(testUser.getId());

    // Assert
    assertThat(result).hasSize(2);
  }

  // ========== Get Total Stars Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should get total stars by user and level from database")
  void shouldGetTotalStarsByUserAndLevelFromDatabase() {
    // Arrange
    ProgressDto progress1 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(2)
        .timeSpent("00:30:00")
        .killedEnemiesNumber(10)
        .solvedPuzzlesNumber(5)
        .build();

    ProgressDto progress2 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(3)
        .timeSpent("00:25:00")
        .killedEnemiesNumber(12)
        .solvedPuzzlesNumber(6)
        .build();

    progressService.createProgress(testUser.getId(), progress1);
    progressService.createProgress(testUser.getId(), progress2);

    // Act
    Integer result = progressService.getTotalStarsByUserAndLevel(
        testUser.getId(), testLevel1.getId());

    // Assert
    assertThat(result).isEqualTo(5); // 2 + 3
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should return null when no progress exists for total stars")
  void shouldReturnNullWhenNoProgressExistsForTotalStars() {
    // Act
    Integer result = progressService.getTotalStarsByUserAndLevel(
        testUser.getId(), testLevel1.getId());

    // Assert
    assertThat(result).isEqualTo(0);
  }

  // ========== Complex Integration Scenarios ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle complete game session workflow")
  void shouldHandleCompleteGameSessionWorkflow() {
    // Arrange - Simulate player completing multiple levels
    ProgressDto level1Progress = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(3)
        .timeSpent("00:20:00")
        .killedEnemiesNumber(15)
        .solvedPuzzlesNumber(8)
        .build();

    ProgressDto level2Progress = ProgressDto.builder()
        .levelId(testLevel2.getId())
        .stars(5)
        .timeSpent("01:30:00")
        .killedEnemiesNumber(30)
        .solvedPuzzlesNumber(12)
        .build();

    // Act
    progressService.createProgress(testUser.getId(), level1Progress);
    progressService.createProgress(testUser.getId(), level2Progress);

    // Assert - Check progress
    List<ProgressDto> allProgress = progressService.getProgressByUserId(testUser.getId());
    assertThat(allProgress).hasSize(2);

    // Assert - Check statistics
    Optional<UserStatisticsDto> stats = statisticsService.getStatisticsByUserId(testUser.getId());
    assertThat(stats).isPresent();
    assertThat(stats.get().getTotalLevelsCompleted()).isEqualTo(2);
    assertThat(stats.get().getTotalStars()).isEqualTo(8);
    assertThat(stats.get().getTotalKilledEnemies()).isEqualTo(45);
    assertThat(stats.get().getTotalSolvedPuzzles()).isEqualTo(20);

    // Verify database persistence
    assertThat(progressRepository.count()).isEqualTo(2);
    assertThat(statisticsRepository.count()).isEqualTo(1);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle player improving score on same level")
  void shouldHandlePlayerImprovingScoreOnSameLevel() {
    // Arrange - First attempt (poor performance)
    ProgressDto firstAttempt = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(1)
        .timeSpent("00:50:00")
        .killedEnemiesNumber(5)
        .solvedPuzzlesNumber(2)
        .build();

    progressService.createProgress(testUser.getId(), firstAttempt);

    Optional<UserStatisticsDto> statsAfterFirst = statisticsService.getStatisticsByUserId(testUser.getId());
    assertThat(statsAfterFirst.get().getTotalStars()).isEqualTo(1);

    // Act - Second attempt (better performance)
    ProgressDto secondAttempt = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(3)
        .timeSpent("00:25:00")
        .killedEnemiesNumber(15)
        .solvedPuzzlesNumber(8)
        .build();

    progressService.createProgress(testUser.getId(), secondAttempt);

    // Assert - Both attempts are saved
    List<Progress> allAttempts = progressRepository.findByUserIdAndLevelId(
        testUser.getId(), testLevel1.getId());
    assertThat(allAttempts).hasSize(2);

    // Latest progress should be the second attempt
    ProgressDto latest = progressService.getLatestProgressByUserAndLevel(
        testUser.getId(), testLevel1.getId());
    assertThat(latest.getStars()).isEqualTo(3);

    // Statistics should reflect best attempt
    Optional<UserStatisticsDto> finalStats = statisticsService.getStatisticsByUserId(testUser.getId());
    assertThat(finalStats.get().getTotalStars()).isEqualTo(3); // Best score from same level
    assertThat(finalStats.get().getTotalKilledEnemies()).isEqualTo(20); // Sum of both attempts
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle multiple users independently")
  void shouldHandleMultipleUsersIndependently() {
    // Arrange
    User user2 = userRepository.save(User.builder()
        .nickname("user2")
        .passwordHash("hash")
        .age(30)
        .build());

    ProgressDto progress1 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(2)
        .timeSpent("00:30:00")
        .killedEnemiesNumber(10)
        .solvedPuzzlesNumber(5)
        .build();

    ProgressDto progress2 = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(3)
        .timeSpent("00:20:00")
        .killedEnemiesNumber(15)
        .solvedPuzzlesNumber(8)
        .build();

    // Act
    progressService.createProgress(testUser.getId(), progress1);
    progressService.createProgress(user2.getId(), progress2);

    // Assert
    List<ProgressDto> user1Progress = progressService.getProgressByUserId(testUser.getId());
    List<ProgressDto> user2Progress = progressService.getProgressByUserId(user2.getId());

    assertThat(user1Progress).hasSize(1);
    assertThat(user2Progress).hasSize(1);

    assertThat(user1Progress.get(0).getStars()).isEqualTo(2);
    assertThat(user2Progress.get(0).getStars()).isEqualTo(3);

    // Verify separate statistics
    Optional<UserStatisticsDto> stats1 = statisticsService.getStatisticsByUserId(testUser.getId());
    Optional<UserStatisticsDto> stats2 = statisticsService.getStatisticsByUserId(user2.getId());

    assertThat(stats1.get().getTotalStars()).isEqualTo(2);
    assertThat(stats2.get().getTotalStars()).isEqualTo(3);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle valid time format edge cases")
  void shouldHandleValidTimeFormatEdgeCases() {
    // Arrange & Act & Assert
    List<String> validTimes = List.of(
        "00:00:00",
        "23:59:59",
        "99:59:59"  // Large hours are valid for total time
    );

    for (String time : validTimes) {
      ProgressDto dto = ProgressDto.builder()
          .levelId(testLevel1.getId())
          .stars(1)
          .timeSpent(time)
          .killedEnemiesNumber(5)
          .solvedPuzzlesNumber(2)
          .build();

      // Should not throw
      ProgressDto result = progressService.createProgress(testUser.getId(), dto);
      assertThat(result.getTimeSpent()).isEqualTo(time);
    }
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should maintain data integrity with concurrent level completions")
  void shouldMaintainDataIntegrityWithConcurrentLevelCompletions() {
    // Arrange - Simulate rapid level completions
    ProgressDto[] progressArray = {
        ProgressDto.builder()
            .levelId(testLevel1.getId())
            .stars(2)
            .timeSpent("00:30:00")
            .killedEnemiesNumber(10)
            .solvedPuzzlesNumber(5)
            .build(),
        ProgressDto.builder()
            .levelId(testLevel2.getId())
            .stars(3)
            .timeSpent("00:45:00")
            .killedEnemiesNumber(15)
            .solvedPuzzlesNumber(8)
            .build(),
        ProgressDto.builder()
            .levelId(testLevel1.getId())
            .stars(3)
            .timeSpent("00:20:00")
            .killedEnemiesNumber(12)
            .solvedPuzzlesNumber(6)
            .build()
    };

    // Act
    for (ProgressDto dto : progressArray) {
      progressService.createProgress(testUser.getId(), dto);
    }

    // Assert
    assertThat(progressRepository.count()).isEqualTo(3);

    List<ProgressDto> allProgress = progressService.getProgressByUserId(testUser.getId());
    assertThat(allProgress).hasSize(3);

    // Statistics should be consistent
    Optional<UserStatisticsDto> stats = statisticsService.getStatisticsByUserId(testUser.getId());
    assertThat(stats).isPresent();
    assertThat(stats.get().getTotalLevelsCompleted()).isEqualTo(2); // 2 unique levels
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should verify progress created_at timestamp is set")
  void shouldVerifyProgressCreatedAtTimestampIsSet() {
    // Arrange
    ProgressDto progressDto = ProgressDto.builder()
        .levelId(testLevel1.getId())
        .stars(2)
        .timeSpent("00:30:00")
        .killedEnemiesNumber(10)
        .solvedPuzzlesNumber(5)
        .build();

    // Act
    ProgressDto result = progressService.createProgress(testUser.getId(), progressDto);

    // Verify in database
    Progress fromDb = progressRepository.findByUserId(testUser.getId()).get(0);
    assertThat(fromDb.getCreatedAt()).isNotNull();
    assertThat(fromDb.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
  }
}
