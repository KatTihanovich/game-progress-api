package com.esdc.gameapi.service.unit;

import com.esdc.gameapi.domain.dto.AchievementDto;
import com.esdc.gameapi.domain.dto.UserAchievementDto;
import com.esdc.gameapi.domain.entity.*;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.*;
import com.esdc.gameapi.service.AchievementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Achievement Service Unit Tests")
class AchievementServiceTest {

  @Mock
  private AchievementRepository achievementRepository;

  @Mock
  private UserAchievementRepository userAchievementRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserStatisticsRepository userStatisticsRepository;

  @Mock
  private LevelRepository levelRepository;

  @InjectMocks
  private AchievementService achievementService;

  private User testUser;
  private Achievement testAchievement;
  private UserStatistics testStats;
  private Progress testProgress;
  private Level testLevel;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .nickname("TestPlayer")
        .passwordHash("hash")
        .age(25)
        .build();

    testAchievement = Achievement.builder()
        .id(1L)
        .achievementName("First Victory")
        .achievementDescription("Complete 1 levels in total")
        .build();

    testStats = UserStatistics.builder()
        .id(1L)
        .user(testUser)
        .totalLevelsCompleted(5)
        .totalKilledEnemies(50)
        .totalSolvedPuzzles(30)
        .totalTimePlayed("02:30:45")
        .totalStars(15)
        .build();

    testLevel = Level.builder()
        .id(1L)
        .levelName("Test Level")
        .starsOnLevel(3)
        .bossOnLevel(true)
        .build();

    testProgress = new Progress();
    testProgress.setId(1L);
    testProgress.setUser(testUser);
    testProgress.setLevel(testLevel);
    testProgress.setKilledEnemiesNumber(10);
    testProgress.setSolvedPuzzlesNumber(5);
    testProgress.setTimeSpent("00:15:30");
    testProgress.setStars(3);
    testProgress.setCreatedAt(LocalDateTime.now());
  }

  // ========== CRUD Operations Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should get all achievements")
  void shouldGetAllAchievements() {
    // Arrange
    Achievement achievement2 = Achievement.builder()
        .id(2L)
        .achievementName("Enemy Hunter")
        .achievementDescription("Kill 100 enemies in total")
        .build();

    when(achievementRepository.findAll()).thenReturn(Arrays.asList(testAchievement, achievement2));

    // Act
    List<AchievementDto> result = achievementService.getAllAchievements();

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getAchievementName()).isEqualTo("First Victory");
    assertThat(result.get(1).getAchievementName()).isEqualTo("Enemy Hunter");
    verify(achievementRepository, times(1)).findAll();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return empty list when no achievements exist")
  void shouldReturnEmptyListWhenNoAchievements() {
    // Arrange
    when(achievementRepository.findAll()).thenReturn(Collections.emptyList());

    // Act
    List<AchievementDto> result = achievementService.getAllAchievements();

    // Assert
    assertThat(result).isEmpty();
    verify(achievementRepository, times(1)).findAll();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should get achievements by user id")
  void shouldGetAchievementsByUserId() {
    // Arrange
    UserAchievement userAchievement = new UserAchievement(testUser, testAchievement);
    userAchievement.setCreatedAt(LocalDateTime.now());

    when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of(userAchievement));

    // Act
    List<UserAchievementDto> result = achievementService.getAchievementsByUserId(1L);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("First Victory");
    assertThat(result.getFirst().getAchievementId()).isEqualTo(1L);
    verify(userAchievementRepository, times(1)).findByUserId(1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return empty list when user has no achievements")
  void shouldReturnEmptyListWhenUserHasNoAchievements() {
    // Arrange
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

    // Act
    List<UserAchievementDto> result = achievementService.getAchievementsByUserId(1L);

    // Assert
    assertThat(result).isEmpty();
    verify(userAchievementRepository, times(1)).findByUserId(1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should create achievement successfully")
  void shouldCreateAchievement() {
    // Arrange
    AchievementDto dto = AchievementDto.builder()
        .achievementName("Speed Runner")
        .achievementDescription("Complete a level in under 60 seconds")
        .build();

    Achievement saved = Achievement.builder()
        .id(2L)
        .achievementName("Speed Runner")
        .achievementDescription("Complete a level in under 60 seconds")
        .build();

    when(achievementRepository.save(any(Achievement.class))).thenReturn(saved);

    // Act
    AchievementDto result = achievementService.createAchievement(dto);

    // Assert
    assertThat(result.getId()).isEqualTo(2L);
    assertThat(result.getAchievementName()).isEqualTo("Speed Runner");
    verify(achievementRepository, times(1)).save(any(Achievement.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should update achievement successfully")
  void shouldUpdateAchievement() {
    // Arrange
    AchievementDto updateDto = AchievementDto.builder()
        .achievementName("Updated Achievement")
        .achievementDescription("Updated description")
        .build();

    Achievement updated = Achievement.builder()
        .id(1L)
        .achievementName("Updated Achievement")
        .achievementDescription("Updated description")
        .build();

    when(achievementRepository.findById(1L)).thenReturn(Optional.of(testAchievement));
    when(achievementRepository.save(any(Achievement.class))).thenReturn(updated);

    // Act
    AchievementDto result = achievementService.updateAchievement(1L, updateDto);

    // Assert
    assertThat(result.getAchievementName()).isEqualTo("Updated Achievement");
    assertThat(result.getAchievementDescription()).isEqualTo("Updated description");
    verify(achievementRepository, times(1)).findById(1L);
    verify(achievementRepository, times(1)).save(any(Achievement.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when updating non-existent achievement")
  void shouldThrowExceptionWhenUpdatingNonExistentAchievement() {
    // Arrange
    AchievementDto updateDto = AchievementDto.builder()
        .achievementName("Ghost Achievement")
        .achievementDescription("Description")
        .build();

    when(achievementRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> achievementService.updateAchievement(999L, updateDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Achievement");

    verify(achievementRepository, times(1)).findById(999L);
    verify(achievementRepository, never()).save(any(Achievement.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should delete achievement successfully")
  void shouldDeleteAchievement() {
    // Arrange
    when(achievementRepository.findById(1L)).thenReturn(Optional.of(testAchievement));
    doNothing().when(achievementRepository).delete(testAchievement);

    // Act
    achievementService.deleteAchievement(1L);

    // Assert
    verify(achievementRepository, times(1)).findById(1L);
    verify(achievementRepository, times(1)).delete(testAchievement);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when deleting non-existent achievement")
  void shouldThrowExceptionWhenDeletingNonExistentAchievement() {
    // Arrange
    when(achievementRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> achievementService.deleteAchievement(999L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(achievementRepository, times(1)).findById(999L);
    verify(achievementRepository, never()).delete(any(Achievement.class));
  }

  // ========== Achievement Unlocking Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should unlock achievement for TOTAL_LEVELS condition")
  void shouldUnlockAchievementForTotalLevels() {
    // Arrange
    Achievement levelAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Explorer")
        .achievementDescription("Complete 5 levels in total")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(levelAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("Explorer");
    verify(userAchievementRepository, times(1)).save(any(UserAchievement.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should unlock achievement for TOTAL_ENEMIES condition")
  void shouldUnlockAchievementForTotalEnemies() {
    // Arrange
    Achievement enemyAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Warrior")
        .achievementDescription("Kill 50 enemies in total")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(enemyAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("Warrior");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should unlock achievement for TOTAL_PUZZLES condition")
  void shouldUnlockAchievementForTotalPuzzles() {
    // Arrange
    Achievement puzzleAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Puzzle Master")
        .achievementDescription("Solve 30 puzzles in total")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(puzzleAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("Puzzle Master");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should unlock achievement for LEVEL_ENEMIES condition")
  void shouldUnlockAchievementForLevelEnemies() {
    // Arrange
    Achievement levelEnemyAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Slayer")
        .achievementDescription("Kill 10 enemies in one level")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(levelEnemyAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("Slayer");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should unlock achievement for DEFEAT_BOSS condition")
  void shouldUnlockAchievementForDefeatBoss() {
    // Arrange
    Achievement bossAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Boss Slayer")
        .achievementDescription("Defeat boss")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(bossAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("Boss Slayer");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should unlock achievement for TOTAL_STARS condition")
  void shouldUnlockAchievementForTotalStars() {
    // Arrange
    Achievement starAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Star Collector")
        .achievementDescription("Collect 15 stars in total")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(starAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("Star Collector");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should unlock achievement for LEVEL_STARS condition")
  void shouldUnlockAchievementForLevelStars() {
    // Arrange
    Achievement levelStarAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Perfect Level")
        .achievementDescription("collect 3 stars in one level")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(levelStarAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("Perfect Level");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should not unlock achievement when condition not met")
  void shouldNotUnlockAchievementWhenConditionNotMet() {
    // Arrange
    Achievement highRequirement = Achievement.builder()
        .id(2L)
        .achievementName("Master")
        .achievementDescription("Complete 100 levels in total")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(highRequirement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).isEmpty();
    verify(userAchievementRepository, never()).save(any(UserAchievement.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should not unlock already unlocked achievement")
  void shouldNotUnlockAlreadyUnlockedAchievement() {
    // Arrange
    UserAchievement existingAchievement = new UserAchievement(testUser, testAchievement);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(testAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of(existingAchievement));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).isEmpty();
    verify(userAchievementRepository, never()).save(any(UserAchievement.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when user not found")
  void shouldThrowExceptionWhenUserNotFound() {
    // Arrange
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> achievementService.checkAndUnlockAchievements(999L, 1L, testProgress))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User");

    verify(userRepository, times(1)).findById(999L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle null statistics gracefully")
  void shouldHandleNullStatistics() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.empty());
    when(achievementRepository.findAll()).thenReturn(List.of(testAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle null progress gracefully")
  void shouldHandleNullProgress() {
    // Arrange
    Achievement levelEnemyAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Slayer")
        .achievementDescription("Kill 10 enemies in one level")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(levelEnemyAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, null);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should skip achievement with unparseable description")
  void shouldSkipAchievementWithUnparseableDescription() {
    // Arrange
    Achievement invalidAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Invalid")
        .achievementDescription("This is not a valid description format")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(invalidAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).isEmpty();
    verify(userAchievementRepository, never()).save(any(UserAchievement.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should unlock multiple achievements in one check")
  void shouldUnlockMultipleAchievementsInOneCheck() {
    // Arrange
    Achievement achievement1 = Achievement.builder()
        .id(2L)
        .achievementName("Explorer")
        .achievementDescription("Complete 5 levels in total")
        .build();

    Achievement achievement2 = Achievement.builder()
        .id(3L)
        .achievementName("Warrior")
        .achievementDescription("Kill 50 enemies in total")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(Arrays.asList(achievement1, achievement2));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(2);
    verify(userAchievementRepository, times(2)).save(any(UserAchievement.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle DEFEAT_BOSS when level has no boss")
  void shouldHandleDefeatBossWhenLevelHasNoBoss() {
    // Arrange
    Achievement bossAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Boss Slayer")
        .achievementDescription("Defeat a boss on any level")
        .build();

    Level noBossLevel = Level.builder()
        .id(2L)
        .levelName("Easy Level")
        .starsOnLevel(3)
        .bossOnLevel(false)
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(bossAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 2L, testProgress);

    // Assert
    assertThat(result).isEmpty();
    verify(userAchievementRepository, never()).save(any(UserAchievement.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle TOTAL_TIME condition")
  void shouldHandleTotalTimeCondition() {
    // Arrange
    Achievement timeAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Time Traveler")
        .achievementDescription("Play for 150 minutes in total")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(timeAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("Time Traveler");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle LEVEL_TIME condition")
  void shouldHandleLevelTimeCondition() {
    // Arrange
    Achievement speedAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Speed Runner")
        .achievementDescription("complete level under 1000 seconds")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(speedAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("Speed Runner");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle LEVEL_PUZZLES condition")
  void shouldHandleLevelPuzzlesCondition() {
    // Arrange
    Achievement puzzleAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Puzzle Expert")
        .achievementDescription("Solve 5 puzzles in one level")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(puzzleAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("Puzzle Expert");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle SPECIFIC_LEVEL condition")
  void shouldHandleSpecificLevelCondition() {
    // Arrange
    Achievement specificLevelAchievement = Achievement.builder()
        .id(2L)
        .achievementName("Level Master")
        .achievementDescription("Complete level 1")
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userStatisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStats));
    when(achievementRepository.findAll()).thenReturn(List.of(specificLevelAchievement));
    when(userAchievementRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(userAchievementRepository.save(any(UserAchievement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<UserAchievementDto> result = achievementService.checkAndUnlockAchievements(1L, 1L, testProgress);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getAchievementName()).isEqualTo("Level Master");
  }
}
