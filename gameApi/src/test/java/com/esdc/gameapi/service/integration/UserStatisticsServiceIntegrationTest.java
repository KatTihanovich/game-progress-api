package com.esdc.gameapi.service.integration;

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
import com.esdc.gameapi.service.UserStatisticsService;
import jakarta.persistence.EntityManager;
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
    "jwt.expiration=3600000"
})
@DisplayName("User Statistics Service Integration Tests")
class UserStatisticsServiceIntegrationTest {

  @Autowired
  private UserStatisticsService statisticsService;

  @Autowired
  private UserStatisticsRepository statisticsRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private LevelRepository levelRepository;

  @Autowired
  private ProgressRepository progressRepository;

  @Autowired
  private EntityManager entityManager;

  private User testUser;
  private Level testLevel1;
  private Level testLevel2;
  private Level testLevel3;

  @BeforeEach
  void setUp() {
    // Очистка всех таблиц
    progressRepository.deleteAll();
    statisticsRepository.deleteAll();
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
        .starsOnLevel(3)
        .bossOnLevel(true)
        .build());

    testLevel3 = levelRepository.save(Level.builder()
        .levelName("Level 3")
        .starsOnLevel(5)
        .bossOnLevel(false)
        .build());
  }

  @AfterEach
  void tearDown() {
    progressRepository.deleteAll();
    statisticsRepository.deleteAll();
    levelRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ========== Get Statistics Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should get statistics by user ID from database")
  void shouldGetStatisticsByUserIdFromDatabase() {
    // Arrange
    UserStatistics statistics = new UserStatistics(testUser);
    statistics.setTotalLevelsCompleted(5);
    statistics.setTotalKilledEnemies(100);
    statistics.setTotalSolvedPuzzles(50);
    statistics.setTotalTimePlayed("05:30:45");
    statistics.setTotalStars(12);
    statisticsRepository.save(statistics);

    // Act
    Optional<UserStatisticsDto> result = statisticsService.getStatisticsByUserId(testUser.getId());

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().getTotalLevelsCompleted()).isEqualTo(5);
    assertThat(result.get().getTotalKilledEnemies()).isEqualTo(100);
    assertThat(result.get().getTotalSolvedPuzzles()).isEqualTo(50);
    assertThat(result.get().getTotalTimePlayed()).isEqualTo("05:30:45");
    assertThat(result.get().getTotalStars()).isEqualTo(12);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should return empty when statistics not found in database")
  void shouldReturnEmptyWhenStatisticsNotFoundInDatabase() {
    // Act
    Optional<UserStatisticsDto> result = statisticsService.getStatisticsByUserId(testUser.getId());

    // Assert
    assertThat(result).isEmpty();
  }

  // ========== Recalculate Statistics Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should recalculate and persist statistics for single level")
  void shouldRecalculateAndPersistStatisticsForSingleLevel() {
    // Arrange
    Progress progress = createProgress(testUser, testLevel1, 10, 5, "00:30:15", 2);
    progressRepository.save(progress);

    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert
    assertThat(result.getTotalLevelsCompleted()).isEqualTo(1);
    assertThat(result.getTotalKilledEnemies()).isEqualTo(10);
    assertThat(result.getTotalSolvedPuzzles()).isEqualTo(5);
    assertThat(result.getTotalTimePlayed()).isEqualTo("00:30:15");
    assertThat(result.getTotalStars()).isEqualTo(2);

    // Verify persistence
    UserStatistics fromDb = statisticsRepository.findByUserId(testUser.getId()).orElseThrow();
    assertThat(fromDb.getTotalLevelsCompleted()).isEqualTo(1);
    assertThat(fromDb.getTotalStars()).isEqualTo(2);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should recalculate statistics for multiple levels")
  void shouldRecalculateStatisticsForMultipleLevels() {
    // Arrange
    progressRepository.save(createProgress(testUser, testLevel1, 10, 5, "00:30:00", 2));
    progressRepository.save(createProgress(testUser, testLevel2, 15, 10, "01:15:30", 3));
    progressRepository.save(createProgress(testUser, testLevel3, 20, 8, "00:45:00", 4));

    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert
    assertThat(result.getTotalLevelsCompleted()).isEqualTo(3);
    assertThat(result.getTotalKilledEnemies()).isEqualTo(45); // 10+15+20
    assertThat(result.getTotalSolvedPuzzles()).isEqualTo(23); // 5+10+8
    assertThat(result.getTotalTimePlayed()).isEqualTo("02:30:30"); // Sum of times
    assertThat(result.getTotalStars()).isEqualTo(9); // 2+3+4
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should take best stars from multiple attempts on same level")
  void shouldTakeBestStarsFromMultipleAttemptsOnSameLevel() {
    // Arrange
    progressRepository.save(createProgress(testUser, testLevel1, 5, 2, "00:10:00", 1));
    progressRepository.save(createProgress(testUser, testLevel1, 8, 4, "00:15:00", 2));
    progressRepository.save(createProgress(testUser, testLevel1, 12, 5, "00:20:00", 3));
    progressRepository.save(createProgress(testUser, testLevel2, 10, 5, "00:30:00", 2));

    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert
    assertThat(result.getTotalLevelsCompleted()).isEqualTo(2); // Only 2 unique levels
    assertThat(result.getTotalKilledEnemies()).isEqualTo(35); // Sum all attempts
    assertThat(result.getTotalStars()).isEqualTo(5); // Best from level1 (3) + level2 (2)
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should cap stars to level maximum")
  void shouldCapStarsToLevelMaximum() {
    // Arrange
    Progress progress = createProgress(testUser, testLevel1, 10, 5, "00:30:00", 10);
    progressRepository.save(progress);

    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert
    assertThat(result.getTotalStars()).isEqualTo(3); // Capped to level max (3)

    UserStatistics fromDb = statisticsRepository.findByUserId(testUser.getId()).orElseThrow();
    assertThat(fromDb.getTotalStars()).isEqualTo(3);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should create new statistics if not exists")
  void shouldCreateNewStatisticsIfNotExists() {
    // Arrange
    progressRepository.save(createProgress(testUser, testLevel1, 10, 5, "00:30:00", 2));

    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert
    assertThat(result).isNotNull();

    // Verify created in database
    UserStatistics fromDb = statisticsRepository.findByUserId(testUser.getId()).orElseThrow();
    assertThat(fromDb.getId()).isNotNull();
    assertThat(fromDb.getUser().getId()).isEqualTo(testUser.getId());
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should update existing statistics")
  void shouldUpdateExistingStatistics() {
    // Arrange
    UserStatistics existing = new UserStatistics(testUser);
    existing.setTotalLevelsCompleted(1);
    existing.setTotalKilledEnemies(10);
    existing.setTotalSolvedPuzzles(5);
    existing.setTotalTimePlayed("00:30:00");
    existing.setTotalStars(2);
    statisticsRepository.save(existing);

    progressRepository.save(createProgress(testUser, testLevel1, 10, 5, "00:30:00", 2));
    progressRepository.save(createProgress(testUser, testLevel2, 15, 8, "01:00:00", 3));

    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert
    assertThat(result.getTotalLevelsCompleted()).isEqualTo(2);
    assertThat(result.getTotalKilledEnemies()).isEqualTo(25);

    // Verify only one statistics record exists
    assertThat(statisticsRepository.count()).isEqualTo(1);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when user not found")
  void shouldThrowExceptionWhenUserNotFound() {
    // Act & Assert
    assertThatThrownBy(() -> statisticsService.recalculateUserStatistics(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("999");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle empty progress list")
  void shouldHandleEmptyProgressList() {
    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert
    assertThat(result.getTotalLevelsCompleted()).isEqualTo(0);
    assertThat(result.getTotalKilledEnemies()).isEqualTo(0);
    assertThat(result.getTotalSolvedPuzzles()).isEqualTo(0);
    assertThat(result.getTotalTimePlayed()).isEqualTo("00:00:00");
    assertThat(result.getTotalStars()).isEqualTo(0);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle invalid time format gracefully")
  void shouldHandleInvalidTimeFormatGracefully() {
    // Arrange
    Progress progress1 = createProgress(testUser, testLevel1, 10, 5, "invalid", 2);
    Progress progress2 = createProgress(testUser, testLevel2, 15, 8, "01:00:00", 3);
    progressRepository.save(progress1);
    progressRepository.save(progress2);

    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert (should only count valid time)
    assertThat(result.getTotalTimePlayed()).isEqualTo("01:00:00");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should calculate time with hours overflow")
  void shouldCalculateTimeWithHoursOverflow() {
    // Arrange
    progressRepository.save(createProgress(testUser, testLevel1, 10, 5, "23:45:30", 2));
    progressRepository.save(createProgress(testUser, testLevel2, 15, 8, "01:30:45", 3));

    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert
    assertThat(result.getTotalTimePlayed()).isEqualTo("25:16:15");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle large time values")
  void shouldHandleLargeTimeValues() {
    // Arrange
    progressRepository.save(createProgress(testUser, testLevel1, 10, 5, "100:00:00", 2));
    progressRepository.save(createProgress(testUser, testLevel2, 15, 8, "200:30:45", 3));

    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert
    assertThat(result.getTotalTimePlayed()).isEqualTo("300:30:45");
  }

  // ========== Max Possible Stars Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should calculate max possible stars from database")
  void shouldCalculateMaxPossibleStarsFromDatabase() {
    // Act
    int result = statisticsService.getMaxPossibleStars();

    // Assert (3 + 3 + 5 = 11)
    assertThat(result).isEqualTo(11);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should return zero when no levels in database")
  void shouldReturnZeroWhenNoLevelsInDatabase() {
    // Arrange
    levelRepository.deleteAll();

    // Act
    int result = statisticsService.getMaxPossibleStars();

    // Assert
    assertThat(result).isEqualTo(0);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should update max stars when new level added")
  void shouldUpdateMaxStarsWhenNewLevelAdded() {
    // Arrange
    int beforeAdd = statisticsService.getMaxPossibleStars();

    // Act
    levelRepository.save(Level.builder()
        .levelName("Level 4")
        .starsOnLevel(4)
        .bossOnLevel(false)
        .build());

    int afterAdd = statisticsService.getMaxPossibleStars();

    // Assert
    assertThat(afterAdd).isEqualTo(beforeAdd + 4);
  }

  // ========== Stars Progress Percentage Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should calculate stars progress percentage from database")
  void shouldCalculateStarsProgressPercentageFromDatabase() {
    // Arrange
    UserStatistics statistics = new UserStatistics(testUser);
    statistics.setTotalStars(5); // Out of 11 max
    statisticsRepository.save(statistics);

    // Act
    double result = statisticsService.getStarsProgressPercentage(testUser.getId());

    // Assert (5/11 * 100 = 45.45)
    assertThat(result).isEqualTo(45.45);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should return zero percentage when no levels exist")
  void shouldReturnZeroPercentageWhenNoLevelsExist() {
    // Arrange
    levelRepository.deleteAll();
    UserStatistics statistics = new UserStatistics(testUser);
    statistics.setTotalStars(5);
    statisticsRepository.save(statistics);

    // Act
    double result = statisticsService.getStarsProgressPercentage(testUser.getId());

    // Assert
    assertThat(result).isEqualTo(0.0);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should return 100 percent when all stars collected")
  void shouldReturn100PercentWhenAllStarsCollected() {
    // Arrange
    UserStatistics statistics = new UserStatistics(testUser);
    statistics.setTotalStars(11); // All stars
    statisticsRepository.save(statistics);

    // Act
    double result = statisticsService.getStarsProgressPercentage(testUser.getId());

    // Assert
    assertThat(result).isEqualTo(100.0);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when statistics not found for percentage")
  void shouldThrowExceptionWhenStatisticsNotFoundForPercentage() {
    // Act & Assert
    assertThatThrownBy(() -> statisticsService.getStarsProgressPercentage(testUser.getId()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Statistics")
        .hasMessageContaining(testUser.getId().toString());
  }

  // ========== Stars Progress DTO Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should get stars progress DTO from database")
  void shouldGetStarsProgressDtoFromDatabase() {
    // Arrange
    UserStatistics statistics = new UserStatistics(testUser);
    statistics.setTotalStars(6);
    statisticsRepository.save(statistics);

    // Act
    StarsProgressDto result = statisticsService.getStarsProgress(testUser.getId());

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getCurrentStars()).isEqualTo(6);
    assertThat(result.getMaxPossibleStars()).isEqualTo(11);
    assertThat(result.getProgressPercentage()).isEqualTo(54.55);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle zero stars in progress DTO")
  void shouldHandleZeroStarsInProgressDto() {
    // Arrange
    UserStatistics statistics = new UserStatistics(testUser);
    statistics.setTotalStars(0);
    statisticsRepository.save(statistics);

    // Act
    StarsProgressDto result = statisticsService.getStarsProgress(testUser.getId());

    // Assert
    assertThat(result.getCurrentStars()).isEqualTo(0);
    assertThat(result.getProgressPercentage()).isEqualTo(0.0);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when statistics not found for progress DTO")
  void shouldThrowExceptionWhenStatisticsNotFoundForProgressDto() {
    // Act & Assert
    assertThatThrownBy(() -> statisticsService.getStarsProgress(testUser.getId()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Statistics");
  }

  // ========== Complex Integration Scenarios ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle complete game progress workflow")
  void shouldHandleCompleteGameProgressWorkflow() {
    // Arrange - Player plays all levels
    progressRepository.save(createProgress(testUser, testLevel1, 10, 5, "00:30:00", 3));
    progressRepository.save(createProgress(testUser, testLevel2, 15, 8, "01:00:00", 3));
    progressRepository.save(createProgress(testUser, testLevel3, 20, 10, "02:00:00", 5));

    // Act
    UserStatisticsDto stats = statisticsService.recalculateUserStatistics(testUser.getId());
    StarsProgressDto progress = statisticsService.getStarsProgress(testUser.getId());

    // Assert
    assertThat(stats.getTotalLevelsCompleted()).isEqualTo(3);
    assertThat(stats.getTotalStars()).isEqualTo(11); // All stars collected
    assertThat(progress.getProgressPercentage()).isEqualTo(100.0);

    // Verify in database
    UserStatistics fromDb = statisticsRepository.findByUserId(testUser.getId()).orElseThrow();
    assertThat(fromDb.getTotalStars()).isEqualTo(11);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should update statistics when player improves score")
  void shouldUpdateStatisticsWhenPlayerImprovesScore() {
    // Arrange - First attempt
    progressRepository.save(createProgress(testUser, testLevel1, 5, 2, "00:20:00", 1));
    UserStatisticsDto firstStats = statisticsService.recalculateUserStatistics(testUser.getId());

    assertThat(firstStats.getTotalStars()).isEqualTo(1);

    // Act - Better attempt
    progressRepository.save(createProgress(testUser, testLevel1, 10, 5, "00:15:00", 3));
    UserStatisticsDto updatedStats = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert
    assertThat(updatedStats.getTotalStars()).isEqualTo(3); // Improved from 1 to 3
    assertThat(updatedStats.getTotalKilledEnemies()).isEqualTo(15); // Sum of both attempts
    assertThat(updatedStats.getTotalLevelsCompleted()).isEqualTo(1); // Still same level
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

    progressRepository.save(createProgress(testUser, testLevel1, 10, 5, "00:30:00", 2));
    progressRepository.save(createProgress(user2, testLevel1, 20, 10, "01:00:00", 3));

    // Act
    UserStatisticsDto stats1 = statisticsService.recalculateUserStatistics(testUser.getId());
    UserStatisticsDto stats2 = statisticsService.recalculateUserStatistics(user2.getId());

    // Assert
    assertThat(stats1.getTotalKilledEnemies()).isEqualTo(10);
    assertThat(stats1.getTotalStars()).isEqualTo(2);

    assertThat(stats2.getTotalKilledEnemies()).isEqualTo(20);
    assertThat(stats2.getTotalStars()).isEqualTo(3);

    assertThat(statisticsRepository.count()).isEqualTo(2);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should maintain data integrity after multiple recalculations")
  void shouldMaintainDataIntegrityAfterMultipleRecalculations() {
    // Arrange
    progressRepository.save(createProgress(testUser, testLevel1, 10, 5, "00:30:00", 2));

    // Act - Multiple recalculations
    UserStatisticsDto stats1 = statisticsService.recalculateUserStatistics(testUser.getId());
    UserStatisticsDto stats2 = statisticsService.recalculateUserStatistics(testUser.getId());
    UserStatisticsDto stats3 = statisticsService.recalculateUserStatistics(testUser.getId());

    // Assert - Results should be identical
    assertThat(stats1.getTotalStars()).isEqualTo(stats2.getTotalStars()).isEqualTo(stats3.getTotalStars());
    assertThat(stats1.getTotalKilledEnemies()).isEqualTo(stats2.getTotalKilledEnemies()).isEqualTo(stats3.getTotalKilledEnemies());

    // Only one statistics record should exist
    assertThat(statisticsRepository.count()).isEqualTo(1);
  }

  // ========== Helper Methods ==========

  private Progress createProgress(User user, Level level, int enemies, int puzzles, String time, int stars) {
    Progress progress = new Progress();
    progress.setUser(user);
    progress.setLevel(level);
    progress.setKilledEnemiesNumber(enemies);
    progress.setSolvedPuzzlesNumber(puzzles);
    progress.setTimeSpent(time);
    progress.setStars(stars);
    progress.setCreatedAt(LocalDateTime.now());
    return progress;
  }
}
