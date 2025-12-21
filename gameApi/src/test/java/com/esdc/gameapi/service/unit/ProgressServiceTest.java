package com.esdc.gameapi.service.unit;

import com.esdc.gameapi.domain.dto.ProgressDto;
import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.domain.entity.Progress;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.LevelRepository;
import com.esdc.gameapi.repository.ProgressRepository;
import com.esdc.gameapi.repository.UserRepository;
import com.esdc.gameapi.service.AchievementService;
import com.esdc.gameapi.service.ProgressService;
import com.esdc.gameapi.service.UserStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Progress Service Unit Tests")
class ProgressServiceTest {

  @Mock
  private ProgressRepository progressRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private LevelRepository levelRepository;

  @Mock
  private UserStatisticsService userStatisticsService;

  @Mock
  private AchievementService achievementService;

  @InjectMocks
  private ProgressService progressService;

  private User testUser;
  private Level testLevel;
  private Progress testProgress;
  private ProgressDto testProgressDto;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .nickname("testuser")
        .passwordHash("hash")
        .age(25)
        .build();

    testLevel = Level.builder()
        .id(1L)
        .levelName("Level 1")
        .starsOnLevel(3)
        .bossOnLevel(false)
        .build();

    testProgress = new Progress();
    testProgress.setId(1L);
    testProgress.setUser(testUser);
    testProgress.setLevel(testLevel);
    testProgress.setKilledEnemiesNumber(10);
    testProgress.setSolvedPuzzlesNumber(5);
    testProgress.setTimeSpent("00:30:15");
    testProgress.setStars(2);
    testProgress.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0, 0));

    testProgressDto = ProgressDto.builder()
        .levelId(1L)
        .killedEnemiesNumber(10)
        .solvedPuzzlesNumber(5)
        .timeSpent("00:30:15")
        .stars(2)
        .build();
  }

  // ========== Create Progress Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should create progress successfully")
  void shouldCreateProgressSuccessfully() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    when(progressRepository.save(any(Progress.class))).thenReturn(testProgress);
    when(achievementService.checkAndUnlockAchievements(eq(1L), eq(1L), any(Progress.class)))
        .thenReturn(Collections.emptyList());

    // Act
    ProgressDto result = progressService.createProgress(1L, testProgressDto);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getLevelId()).isEqualTo(1L);
    assertThat(result.getStars()).isEqualTo(2);
    assertThat(result.getKilledEnemiesNumber()).isEqualTo(10);
    assertThat(result.getSolvedPuzzlesNumber()).isEqualTo(5);
    assertThat(result.getTimeSpent()).isEqualTo("00:30:15");

    verify(userRepository, times(1)).findById(1L);
    verify(levelRepository, times(1)).findById(1L);
    verify(progressRepository, times(1)).save(any(Progress.class));
    verify(userStatisticsService, times(1)).recalculateUserStatistics(1L);
    verify(achievementService, times(1)).checkAndUnlockAchievements(eq(1L), eq(1L), any(Progress.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when user not found")
  void shouldThrowExceptionWhenUserNotFound() {
    // Arrange
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(999L, testProgressDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("999");

    verify(userRepository, times(1)).findById(999L);
    verify(levelRepository, never()).findById(any());
    verify(progressRepository, never()).save(any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when level not found")
  void shouldThrowExceptionWhenLevelNotFound() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(999L)).thenReturn(Optional.empty());

    ProgressDto invalidDto = ProgressDto.builder()
        .levelId(999L)
        .stars(2)
        .timeSpent("00:30:00")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(1L, invalidDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Level")
        .hasMessageContaining("999");

    verify(levelRepository, times(1)).findById(999L);
    verify(progressRepository, never()).save(any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should validate time format correctly")
  void shouldValidateTimeFormatCorrectly() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));

    ProgressDto invalidTimeDto = ProgressDto.builder()
        .levelId(1L)
        .stars(2)
        .timeSpent("invalid")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(1L, invalidTimeDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid time format");

    verify(progressRepository, never()).save(any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should reject invalid time format variations")
  void shouldRejectInvalidTimeFormatVariations() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));

    List<String> invalidTimes = Arrays.asList(
        "1:30:15",      // Missing leading zero in hours
        "01:60:00",     // Invalid minutes
        "01:30:60",     // Invalid seconds
        "25:00:00",     // Hours can be >= 24 (valid for total time)
        "00-30-00",     // Wrong delimiter
        "00:30"         // Missing seconds
    );

    for (String invalidTime : invalidTimes) {
      ProgressDto dto = ProgressDto.builder()
          .levelId(1L)
          .stars(2)
          .timeSpent(invalidTime)
          .build();

      // Act & Assert
      if (!invalidTime.equals("25:00:00")) { // This one might be valid
        assertThatThrownBy(() -> progressService.createProgress(1L, dto))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Test
  @Tag("unit")
  @DisplayName("Should accept valid time formats")
  void shouldAcceptValidTimeFormats() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    when(progressRepository.save(any(Progress.class))).thenReturn(testProgress);
    when(achievementService.checkAndUnlockAchievements(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    List<String> validTimes = Arrays.asList(
        "00:00:00",
        "01:30:45",
        "23:59:59",
        "99:59:59"  // Large hours are valid
    );

    for (String validTime : validTimes) {
      ProgressDto dto = ProgressDto.builder()
          .levelId(1L)
          .stars(2)
          .timeSpent(validTime)
          .build();

      // Act
      progressService.createProgress(1L, dto);

      // Assert - should not throw
      verify(progressRepository, atLeastOnce()).save(any(Progress.class));
    }
  }

  @Test
  @Tag("unit")
  @DisplayName("Should validate stars are not null")
  void shouldValidateStarsAreNotNull() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));

    ProgressDto nullStarsDto = ProgressDto.builder()
        .levelId(1L)
        .stars(null)
        .timeSpent("00:30:00")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(1L, nullStarsDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Stars cannot be null");

    verify(progressRepository, never()).save(any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should validate stars are not negative")
  void shouldValidateStarsAreNotNegative() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));

    ProgressDto negativeStarsDto = ProgressDto.builder()
        .levelId(1L)
        .stars(-1)
        .timeSpent("00:30:00")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(1L, negativeStarsDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Stars cannot be negative");

    verify(progressRepository, never()).save(any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should validate stars do not exceed level maximum")
  void shouldValidateStarsDoNotExceedLevelMaximum() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));

    ProgressDto tooManyStarsDto = ProgressDto.builder()
        .levelId(1L)
        .stars(5)  // Level max is 3
        .timeSpent("00:30:00")
        .build();

    // Act & Assert
    assertThatThrownBy(() -> progressService.createProgress(1L, tooManyStarsDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot exceed maximum stars");

    verify(progressRepository, never()).save(any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should accept zero stars")
  void shouldAcceptZeroStars() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    when(progressRepository.save(any(Progress.class))).thenReturn(testProgress);
    when(achievementService.checkAndUnlockAchievements(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    ProgressDto zeroStarsDto = ProgressDto.builder()
        .levelId(1L)
        .stars(0)
        .timeSpent("00:30:00")
        .build();

    // Act
    ProgressDto result = progressService.createProgress(1L, zeroStarsDto);

    // Assert
    assertThat(result).isNotNull();
    verify(progressRepository, times(1)).save(any(Progress.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should accept maximum stars for level")
  void shouldAcceptMaximumStarsForLevel() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    when(progressRepository.save(any(Progress.class))).thenReturn(testProgress);
    when(achievementService.checkAndUnlockAchievements(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    ProgressDto maxStarsDto = ProgressDto.builder()
        .levelId(1L)
        .stars(3)  // Level max is 3
        .timeSpent("00:30:00")
        .build();

    // Act
    ProgressDto result = progressService.createProgress(1L, maxStarsDto);

    // Assert
    assertThat(result).isNotNull();
    verify(progressRepository, times(1)).save(any(Progress.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should trigger statistics recalculation after saving")
  void shouldTriggerStatisticsRecalculationAfterSaving() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    when(progressRepository.save(any(Progress.class))).thenReturn(testProgress);
    when(achievementService.checkAndUnlockAchievements(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    // Act
    progressService.createProgress(1L, testProgressDto);

    // Assert
    verify(userStatisticsService, times(1)).recalculateUserStatistics(1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should trigger achievement check after saving")
  void shouldTriggerAchievementCheckAfterSaving() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    when(progressRepository.save(any(Progress.class))).thenReturn(testProgress);
    when(achievementService.checkAndUnlockAchievements(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    // Act
    progressService.createProgress(1L, testProgressDto);

    // Assert
    verify(achievementService, times(1)).checkAndUnlockAchievements(eq(1L), eq(1L), any(Progress.class));
  }

  // ========== Get Latest Progress Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should get latest progress by user and level")
  void shouldGetLatestProgressByUserAndLevel() {
    // Arrange
    Progress oldProgress = new Progress();
    oldProgress.setId(1L);
    oldProgress.setUser(testUser);
    oldProgress.setLevel(testLevel);
    oldProgress.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
    oldProgress.setStars(1);
    oldProgress.setTimeSpent("00:30:00");

    Progress newProgress = new Progress();
    newProgress.setId(2L);
    newProgress.setUser(testUser);
    newProgress.setLevel(testLevel);
    newProgress.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0, 0));
    newProgress.setStars(3);
    newProgress.setTimeSpent("00:25:00");

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    when(progressRepository.findByUserIdAndLevelId(1L, 1L))
        .thenReturn(Arrays.asList(oldProgress, newProgress));

    // Act
    ProgressDto result = progressService.getLatestProgressByUserAndLevel(1L, 1L);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getStars()).isEqualTo(3);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when no progress found")
  void shouldThrowExceptionWhenNoProgressFound() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    when(progressRepository.findByUserIdAndLevelId(1L, 1L))
        .thenReturn(Collections.emptyList());

    // Act & Assert
    assertThatThrownBy(() -> progressService.getLatestProgressByUserAndLevel(1L, 1L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Progress");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when user not found for latest progress")
  void shouldThrowExceptionWhenUserNotFoundForLatestProgress() {
    // Arrange
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> progressService.getLatestProgressByUserAndLevel(999L, 1L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User");

    verify(progressRepository, never()).findByUserIdAndLevelId(any(), any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when level not found for latest progress")
  void shouldThrowExceptionWhenLevelNotFoundForLatestProgress() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> progressService.getLatestProgressByUserAndLevel(1L, 999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Level");
  }

  // ========== Get Progress By User Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should get all progress by user ID")
  void shouldGetAllProgressByUserId() {
    // Arrange
    Progress progress2 = new Progress();
    progress2.setId(2L);
    progress2.setUser(testUser);
    progress2.setLevel(testLevel);
    progress2.setCreatedAt(LocalDateTime.now());
    progress2.setStars(3);
    progress2.setTimeSpent("00:20:00");

    when(progressRepository.findByUserId(1L))
        .thenReturn(Arrays.asList(testProgress, progress2));

    // Act
    List<ProgressDto> result = progressService.getProgressByUserId(1L);

    // Assert
    assertThat(result).hasSize(2);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return empty list when user has no progress")
  void shouldReturnEmptyListWhenUserHasNoProgress() {
    // Arrange
    when(progressRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

    // Act
    List<ProgressDto> result = progressService.getProgressByUserId(1L);

    // Assert
    assertThat(result).isEmpty();
  }

  // ========== Get Total Stars Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should get total stars by user and level")
  void shouldGetTotalStarsByUserAndLevel() {
    // Arrange
    when(progressRepository.getTotalStarsByUserIdAndLevelId(1L, 1L)).thenReturn(10);

    // Act
    Integer result = progressService.getTotalStarsByUserAndLevel(1L, 1L);

    // Assert
    assertThat(result).isEqualTo(10);
    verify(progressRepository, times(1)).getTotalStarsByUserIdAndLevelId(1L, 1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return null when no stars recorded")
  void shouldReturnNullWhenNoStarsRecorded() {
    // Arrange
    when(progressRepository.getTotalStarsByUserIdAndLevelId(1L, 1L)).thenReturn(null);

    // Act
    Integer result = progressService.getTotalStarsByUserAndLevel(1L, 1L);

    // Assert
    assertThat(result).isNull();
  }

  // ========== DTO Conversion Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should convert progress to DTO with level name")
  void shouldConvertProgressToDtoWithLevelName() {
    // Arrange
    when(progressRepository.findByUserId(1L)).thenReturn(List.of(testProgress));

    // Act
    List<ProgressDto> result = progressService.getProgressByUserId(1L);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLevelId()).isEqualTo(1L);
    assertThat(result.get(0).getStars()).isEqualTo(2);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle progress with zero values")
  void shouldHandleProgressWithZeroValues() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    when(progressRepository.save(any(Progress.class))).thenReturn(testProgress);
    when(achievementService.checkAndUnlockAchievements(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    ProgressDto zeroDto = ProgressDto.builder()
        .levelId(1L)
        .killedEnemiesNumber(0)
        .solvedPuzzlesNumber(0)
        .stars(0)
        .timeSpent("00:00:00")
        .build();

    // Act
    ProgressDto result = progressService.createProgress(1L, zeroDto);

    // Assert
    assertThat(result).isNotNull();
    verify(progressRepository, times(1)).save(any(Progress.class));
  }
}
