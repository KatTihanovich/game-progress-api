package com.esdc.gameapi.service.integration;

import com.esdc.gameapi.domain.dto.AchievementDto;
import com.esdc.gameapi.domain.dto.UserAchievementDto;
import com.esdc.gameapi.domain.entity.*;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.*;
import com.esdc.gameapi.service.AchievementService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
@DisplayName("Achievement Service Integration Tests")
class AchievementServiceIntegrationTest {

  @Autowired
  private AchievementService achievementService;

  @Autowired
  private AchievementRepository achievementRepository;

  @Autowired
  private UserAchievementRepository userAchievementRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserStatisticsRepository userStatisticsRepository;

  @Autowired
  private LevelRepository levelRepository;

  @Autowired
  private ProgressRepository progressRepository;

  @PersistenceContext
  private EntityManager entityManager;

  private User testUser;
  private UserStatistics testStats;
  private Level testLevel;
  private Progress testProgress;

  @BeforeEach
  void setUp() {
    // Очистка всех таблиц
    userAchievementRepository.deleteAll();
    progressRepository.deleteAll();
    userStatisticsRepository.deleteAll();
    achievementRepository.deleteAll();
    levelRepository.deleteAll();
    userRepository.deleteAll();

    // Создание тестовых данных
    testUser = userRepository.save(User.builder()
        .nickname("TestPlayer")
        .passwordHash("hash")
        .age(25)
        .build());

    testLevel = levelRepository.save(Level.builder()
        .levelName("Test Level")
        .starsOnLevel(3)
        .bossOnLevel(true)
        .build());

    testStats = userStatisticsRepository.save(UserStatistics.builder()
        .user(testUser)
        .totalLevelsCompleted(5)
        .totalKilledEnemies(50)
        .totalSolvedPuzzles(30)
        .totalTimePlayed("02:30:45")
        .totalStars(15)
        .build());

    testProgress = new Progress();
    testProgress.setUser(testUser);
    testProgress.setLevel(testLevel);
    testProgress.setKilledEnemiesNumber(10);
    testProgress.setSolvedPuzzlesNumber(5);
    testProgress.setTimeSpent("00:15:30");
    testProgress.setStars(3);
    testProgress.setCreatedAt(LocalDateTime.now());
    testProgress = progressRepository.save(testProgress);
  }

  @AfterEach
  void tearDown() {
    userAchievementRepository.deleteAll();
    progressRepository.deleteAll();
    userStatisticsRepository.deleteAll();
    achievementRepository.deleteAll();
    levelRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ========== CRUD Operations Tests ==========

  @Test
  @Tag("integration")
  @DisplayName("Should get all achievements from database")
  void shouldGetAllAchievementsFromDatabase() {
    // Arrange
    Achievement achievement1 = achievementRepository.save(Achievement.builder()
        .achievementName("First Victory")
        .achievementDescription("Complete 1 levels")
        .build());

    Achievement achievement2 = achievementRepository.save(Achievement.builder()
        .achievementName("Enemy Hunter")
        .achievementDescription("Kill 100 enemies")
        .build());

    // Act
    List<AchievementDto> result = achievementService.getAllAchievements();

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result).extracting(AchievementDto::getAchievementName)
        .containsExactlyInAnyOrder("First Victory", "Enemy Hunter");
  }

  @Test
  @Tag("integration")
  @DisplayName("Should return empty list when no achievements in database")
  void shouldReturnEmptyListWhenNoAchievements() {
    // Act
    List<AchievementDto> result = achievementService.getAllAchievements();

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should create and persist achievement to database")
  void shouldCreateAndPersistAchievement() {
    // Arrange
    AchievementDto newAchievement = AchievementDto.builder()
        .achievementName("Speed Runner")
        .achievementDescription("Complete level under 60 seconds")
        .build();

    // Act
    AchievementDto created = achievementService.createAchievement(newAchievement);

    // Assert
    assertThat(created.getId()).isNotNull();
    assertThat(created.getAchievementName()).isEqualTo("Speed Runner");

    Achievement fromDb = achievementRepository.findById(created.getId()).orElseThrow();
    assertThat(fromDb.getAchievementName()).isEqualTo("Speed Runner");
    assertThat(fromDb.getAchievementDescription()).isEqualTo("Complete level under 60 seconds");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should update existing achievement in database")
  void shouldUpdateExistingAchievementInDatabase() {
    // Arrange
    Achievement achievement = achievementRepository.save(Achievement.builder()
        .achievementName("Original Name")
        .achievementDescription("Original Description")
        .build());

    AchievementDto updateDto = AchievementDto.builder()
        .achievementName("Updated Name")
        .achievementDescription("Updated Description")
        .build();

    // Act
    AchievementDto updated = achievementService.updateAchievement(achievement.getId(), updateDto);

    // Assert
    assertThat(updated.getAchievementName()).isEqualTo("Updated Name");
    assertThat(updated.getAchievementDescription()).isEqualTo("Updated Description");

    Achievement fromDb = achievementRepository.findById(achievement.getId()).orElseThrow();
    assertThat(fromDb.getAchievementName()).isEqualTo("Updated Name");
    assertThat(fromDb.getAchievementDescription()).isEqualTo("Updated Description");
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when updating non-existent achievement")
  void shouldThrowExceptionWhenUpdatingNonExistentAchievement() {
    // Arrange
    AchievementDto updateDto = AchievementDto.builder()
        .achievementName("Ghost")
        .achievementDescription("Description")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> achievementService.updateAchievement(999L, updateDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Achievement");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should delete achievement from database")
  void shouldDeleteAchievementFromDatabase() {
    // Arrange
    Achievement achievement = achievementRepository.save(Achievement.builder()
        .achievementName("Test Achievement")
        .achievementDescription("Test Description")
        .build());
    Long achievementId = achievement.getId();

    // Act
    achievementService.deleteAchievement(achievementId);

    // Assert
    assertThat(achievementRepository.findById(achievementId)).isEmpty();
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when deleting non-existent achievement")
  void shouldThrowExceptionWhenDeletingNonExistentAchievement() {
    // Act & Assert
    assertThatThrownBy(() -> achievementService.deleteAchievement(999L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // ========== User Achievements Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should get user achievements from database")
  void shouldGetUserAchievementsFromDatabase() {
    // Arrange
    Achievement achievement = achievementRepository.save(Achievement.builder()
        .achievementName("First Victory")
        .achievementDescription("Complete 1 levels")
        .build());

    UserAchievement userAchievement = new UserAchievement(testUser, achievement);
    userAchievement.setCreatedAt(LocalDateTime.now());
    userAchievementRepository.save(userAchievement);

    // Act
    List<UserAchievementDto> result = achievementService.getAchievementsByUserId(testUser.getId());

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("First Victory");
    assertThat(result.get(0).getAchievementId()).isEqualTo(achievement.getId());
  }

  @Test
  @Tag("integration")
  @DisplayName("Should return empty list when user has no achievements")
  void shouldReturnEmptyListWhenUserHasNoAchievements() {
    // Act
    List<UserAchievementDto> result = achievementService.getAchievementsByUserId(testUser.getId());

    // Assert
    assertThat(result).isEmpty();
  }

  // ========== Achievement Unlocking Tests ==========

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock achievement for TOTAL_LEVELS and persist to database")
  void shouldUnlockAchievementForTotalLevelsAndPersist() {
    // Arrange
    Achievement levelAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Explorer")
        .achievementDescription("Complete 5 levels")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("Explorer");

    // Verify persistence
    List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(testUser.getId());
    assertThat(userAchievements).hasSize(1);
    assertThat(userAchievements.get(0).getAchievement().getAchievementName()).isEqualTo("Explorer");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock achievement for TOTAL_ENEMIES")
  void shouldUnlockAchievementForTotalEnemies() {
    // Arrange
    Achievement enemyAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Warrior")
        .achievementDescription("Kill 50 enemies")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("Warrior");

    List<UserAchievement> persisted = userAchievementRepository.findByUserId(testUser.getId());
    assertThat(persisted).hasSize(1);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock achievement for TOTAL_PUZZLES")
  void shouldUnlockAchievementForTotalPuzzles() {
    // Arrange
    Achievement puzzleAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Puzzle Master")
        .achievementDescription("Solve 30 puzzles")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("Puzzle Master");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock achievement for DEFEAT_BOSS")
  void shouldUnlockAchievementForDefeatBoss() {
    // Arrange
    Achievement bossAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Boss Slayer")
        .achievementDescription("Defeat boss")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("Boss Slayer");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock achievement for LEVEL_ENEMIES")
  void shouldUnlockAchievementForLevelEnemies() {
    // Arrange
    Achievement levelEnemyAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Slayer")
        .achievementDescription("Kill 10 enemies in one level")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("Slayer");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock achievement for LEVEL_PUZZLES")
  void shouldUnlockAchievementForLevelPuzzles() {
    // Arrange
    Achievement levelPuzzleAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Puzzle Expert")
        .achievementDescription("Solve 5 puzzles in one level")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("Puzzle Expert");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock achievement for TOTAL_STARS")
  void shouldUnlockAchievementForTotalStars() {
    // Arrange
    Achievement starAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Star Collector")
        .achievementDescription("Collect 15 stars")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("Star Collector");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock achievement for LEVEL_STARS")
  void shouldUnlockAchievementForLevelStars() {
    // Arrange
    Achievement levelStarAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Perfect Level")
        .achievementDescription("Collect 3 stars in one level")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("Perfect Level");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock achievement for LEVEL_TIME")
  void shouldUnlockAchievementForLevelTime() {
    // Arrange
    Achievement speedAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Speed Runner")
        .achievementDescription("Complete level under 1000 seconds")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("Speed Runner");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock achievement for SPECIFIC_LEVEL")
  void shouldUnlockAchievementForSpecificLevel() {
    // Arrange
    Achievement specificLevelAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Level Master")
        .achievementDescription("Complete level " + testLevel.getId())
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("Level Master");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock achievement for TOTAL_TIME")
  void shouldUnlockAchievementForTotalTime() {
    // Arrange
    Achievement timeAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Time Traveler")
        .achievementDescription("Play for 150 minutes")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAchievementName()).isEqualTo("Time Traveler");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should not unlock already unlocked achievement")
  void shouldNotUnlockAlreadyUnlockedAchievement() {
    // Arrange
    Achievement achievement = achievementRepository.save(Achievement.builder()
        .achievementName("Explorer")
        .achievementDescription("Complete 5 levels")
        .build());

    UserAchievement existing = new UserAchievement(testUser, achievement);
    existing.setCreatedAt(LocalDateTime.now());
    userAchievementRepository.save(existing);

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).isEmpty();

    // Verify still only one record
    List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(testUser.getId());
    assertThat(userAchievements).hasSize(1);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should unlock multiple achievements in one check")
  void shouldUnlockMultipleAchievements() {
    // Arrange
    Achievement achievement1 = achievementRepository.save(Achievement.builder()
        .achievementName("Explorer")
        .achievementDescription("Complete 5 levels")
        .build());

    Achievement achievement2 = achievementRepository.save(Achievement.builder()
        .achievementName("Warrior")
        .achievementDescription("Kill 50 enemies")
        .build());

    Achievement achievement3 = achievementRepository.save(Achievement.builder()
        .achievementName("Boss Slayer")
        .achievementDescription("Defeat boss")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).hasSize(3);
    assertThat(result).extracting(UserAchievementDto::getAchievementName)
        .containsExactlyInAnyOrder("Explorer", "Warrior", "Boss Slayer");

    List<UserAchievement> persisted = userAchievementRepository.findByUserId(testUser.getId());
    assertThat(persisted).hasSize(3);
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when user not found")
  void shouldThrowExceptionWhenUserNotFound() {
    // Act & Assert
    assertThatThrownBy(() -> achievementService.checkAndUnlockAchievements(999L, testLevel.getId(), testProgress))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle null statistics gracefully")
  void shouldHandleNullStatistics() {
    // Arrange
    userStatisticsRepository.deleteAll();

    Achievement achievement = achievementRepository.save(Achievement.builder()
        .achievementName("Explorer")
        .achievementDescription("Complete 5 levels")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should not unlock when condition not met")
  void shouldNotUnlockWhenConditionNotMet() {
    // Arrange
    Achievement highRequirement = achievementRepository.save(Achievement.builder()
        .achievementName("Master")
        .achievementDescription("Complete 100 levels")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).isEmpty();

    List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(testUser.getId());
    assertThat(userAchievements).isEmpty();
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle DEFEAT_BOSS when level has no boss")
  void shouldHandleDefeatBossWhenLevelHasNoBoss() {
    // Arrange
    Level noBossLevel = levelRepository.save(Level.builder()
        .levelName("Easy Level")
        .starsOnLevel(3)
        .bossOnLevel(false)
        .build());

    Achievement bossAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Boss Slayer")
        .achievementDescription("Defeat boss")
        .build());

    Progress noBossProgress = new Progress();
    noBossProgress.setUser(testUser);
    noBossProgress.setLevel(noBossLevel);
    noBossProgress.setKilledEnemiesNumber(5);
    noBossProgress.setSolvedPuzzlesNumber(3);
    noBossProgress.setTimeSpent("00:10:00");
    noBossProgress.setStars(2);
    noBossProgress.setCreatedAt(LocalDateTime.now());
    noBossProgress = progressRepository.save(noBossProgress);

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), noBossLevel.getId(), noBossProgress);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should verify achievement timestamps are set correctly")
  void shouldVerifyAchievementTimestampsAreSet() {
    // Arrange
    Achievement achievement = achievementRepository.save(Achievement.builder()
        .achievementName("Explorer")
        .achievementDescription("Complete 5 levels")
        .build());

    LocalDateTime beforeUnlock = LocalDateTime.now().minusSeconds(1);

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    LocalDateTime afterUnlock = LocalDateTime.now().plusSeconds(1);

    // Assert
    assertThat(result).hasSize(1);

    UserAchievement persisted = userAchievementRepository.findByUserId(testUser.getId()).get(0);
    assertThat(persisted.getCreatedAt()).isNotNull();
    assertThat(persisted.getCreatedAt()).isAfter(beforeUnlock);
    assertThat(persisted.getCreatedAt()).isBefore(afterUnlock);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle null progress gracefully")
  void shouldHandleNullProgressGracefully() {
    // Arrange
    Achievement levelEnemyAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Slayer")
        .achievementDescription("Kill 10 enemies in one level")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), null);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should skip achievement with invalid description")
  void shouldSkipAchievementWithInvalidDescription() {
    // Arrange
    Achievement invalidAchievement = achievementRepository.save(Achievement.builder()
        .achievementName("Invalid")
        .achievementDescription("This is totally invalid format")
        .build());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(
        testUser.getId(), testLevel.getId(), testProgress);

    // Assert
    assertThat(result).isEmpty();
  }
}
