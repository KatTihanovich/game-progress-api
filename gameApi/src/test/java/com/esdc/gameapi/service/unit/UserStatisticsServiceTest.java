package com.esdc.gameapi.service.unit;

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
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("User Statistics Service Unit Tests")
class UserStatisticsServiceTest {

  @Mock
  private UserStatisticsRepository statisticsRepository;

  @Mock
  private ProgressRepository progressRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private LevelRepository levelRepository;

  @InjectMocks
  private UserStatisticsService statisticsService;

  private User testUser;
  private UserStatistics testStatistics;
  private Level testLevel1;
  private Level testLevel2;
  private Progress testProgress1;
  private Progress testProgress2;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .nickname("testuser")
        .passwordHash("hash")
        .age(25)
        .build();

    testLevel1 = Level.builder()
        .id(1L)
        .levelName("Level 1")
        .starsOnLevel(3)
        .bossOnLevel(false)
        .build();

    testLevel2 = Level.builder()
        .id(2L)
        .levelName("Level 2")
        .starsOnLevel(3)
        .bossOnLevel(true)
        .build();

    testStatistics = UserStatistics.builder()
        .id(1L)
        .user(testUser)
        .totalLevelsCompleted(2)
        .totalKilledEnemies(50)
        .totalSolvedPuzzles(30)
        .totalTimePlayed("02:30:45")
        .totalStars(5)
        .build();

    testProgress1 = new Progress();
    testProgress1.setId(1L);
    testProgress1.setUser(testUser);
    testProgress1.setLevel(testLevel1);
    testProgress1.setKilledEnemiesNumber(10);
    testProgress1.setSolvedPuzzlesNumber(5);
    testProgress1.setTimeSpent("00:30:15");
    testProgress1.setStars(2);
    testProgress1.setCreatedAt(LocalDateTime.now());

    testProgress2 = new Progress();
    testProgress2.setId(2L);
    testProgress2.setUser(testUser);
    testProgress2.setLevel(testLevel2);
    testProgress2.setKilledEnemiesNumber(15);
    testProgress2.setSolvedPuzzlesNumber(10);
    testProgress2.setTimeSpent("01:15:30");
    testProgress2.setStars(3);
    testProgress2.setCreatedAt(LocalDateTime.now());
  }

  // ========== Get Statistics Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should get statistics by user ID")
  void shouldGetStatisticsByUserId() {
    // Arrange
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));

    // Act
    Optional<UserStatisticsDto> result = statisticsService.getStatisticsByUserId(1L);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().getTotalLevelsCompleted()).isEqualTo(2);
    assertThat(result.get().getTotalKilledEnemies()).isEqualTo(50);
    assertThat(result.get().getTotalSolvedPuzzles()).isEqualTo(30);
    assertThat(result.get().getTotalTimePlayed()).isEqualTo("02:30:45");
    assertThat(result.get().getTotalStars()).isEqualTo(5);

    verify(statisticsRepository, times(1)).findByUserId(1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return empty when statistics not found")
  void shouldReturnEmptyWhenStatisticsNotFound() {
    // Arrange
    when(statisticsRepository.findByUserId(999L)).thenReturn(Optional.empty());

    // Act
    Optional<UserStatisticsDto> result = statisticsService.getStatisticsByUserId(999L);

    // Assert
    assertThat(result).isEmpty();
    verify(statisticsRepository, times(1)).findByUserId(999L);
  }

  // ========== Recalculate Statistics Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should recalculate user statistics successfully")
  void shouldRecalculateUserStatisticsSuccessfully() {
    // Arrange
    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenReturn(testStatistics);

    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(1L);

    // Assert
    assertThat(result).isNotNull();
    verify(userRepository, times(1)).findById(1L);
    verify(progressRepository, times(1)).findByUserId(1L);
    verify(statisticsRepository, times(1)).save(any(UserStatistics.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when user not found during recalculation")
  void shouldThrowExceptionWhenUserNotFoundDuringRecalculation() {
    // Arrange
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> statisticsService.recalculateUserStatistics(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User")
        .hasMessageContaining("999");

    verify(userRepository, times(1)).findById(999L);
    verify(progressRepository, never()).findByUserId(any());
    verify(statisticsRepository, never()).save(any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should calculate total levels completed correctly")
  void shouldCalculateTotalLevelsCompletedCorrectly() {
    // Arrange
    Progress progress3 = new Progress();
    progress3.setId(3L);
    progress3.setUser(testUser);
    progress3.setLevel(testLevel1); // Same level again
    progress3.setKilledEnemiesNumber(5);
    progress3.setSolvedPuzzlesNumber(3);
    progress3.setTimeSpent("00:10:00");
    progress3.setStars(3);
    progress3.setCreatedAt(LocalDateTime.now());

    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2, progress3);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    UserStatisticsDto result = statisticsService.recalculateUserStatistics(1L);

    // Assert
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalLevelsCompleted() == 2));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should calculate total killed enemies correctly")
  void shouldCalculateTotalKilledEnemiesCorrectly() {
    // Arrange
    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert (10 + 15 = 25)
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalKilledEnemies() == 25));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should calculate total solved puzzles correctly")
  void shouldCalculateTotalSolvedPuzzlesCorrectly() {
    // Arrange
    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert (5 + 10 = 15)
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalSolvedPuzzles() == 15));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should calculate total time played correctly")
  void shouldCalculateTotalTimePlayedCorrectly() {
    // Arrange
    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert (00:30:15 + 01:15:30 = 01:45:45)
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalTimePlayed().equals("01:45:45")));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should calculate total stars correctly with best attempt per level")
  void shouldCalculateTotalStarsCorrectlyWithBestAttemptPerLevel() {
    // Arrange
    Progress progress3 = new Progress();
    progress3.setId(3L);
    progress3.setUser(testUser);
    progress3.setLevel(testLevel1); // Same level, better stars
    progress3.setKilledEnemiesNumber(5);
    progress3.setSolvedPuzzlesNumber(3);
    progress3.setTimeSpent("00:10:00");
    progress3.setStars(3); // Better than testProgress1 (2 stars)
    progress3.setCreatedAt(LocalDateTime.now());

    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2, progress3);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert (Level1: 3 stars, Level2: 3 stars = 6 total)
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalStars() == 6));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should cap stars to level maximum")
  void shouldCapStarsToLevelMaximum() {
    // Arrange
    testProgress1.setStars(10); // More than level max (3)

    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert (Level1: capped to 3, Level2: 3 = 6 total)
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalStars() == 6));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should create new statistics if not exists")
  void shouldCreateNewStatisticsIfNotExists() {
    // Arrange
    List<Progress> progressList = Arrays.asList(testProgress1);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.empty());
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert
    verify(statisticsRepository).save(argThat(stats ->
        stats.getUser().equals(testUser) && stats.getId() == null
    ));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle empty progress list")
  void shouldHandleEmptyProgressList() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert
    verify(statisticsRepository).save(argThat(stats ->
        stats.getTotalLevelsCompleted() == 0 &&
            stats.getTotalKilledEnemies() == 0 &&
            stats.getTotalSolvedPuzzles() == 0 &&
            stats.getTotalStars() == 0 &&
            stats.getTotalTimePlayed().equals("00:00:00")
    ));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle null time spent in progress")
  void shouldHandleNullTimeSpentInProgress() {
    // Arrange
    testProgress1.setTimeSpent(null);
    testProgress2.setTimeSpent("01:00:00");

    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert (should only count testProgress2)
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalTimePlayed().equals("01:00:00")));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle empty time spent in progress")
  void shouldHandleEmptyTimeSpentInProgress() {
    // Arrange
    testProgress1.setTimeSpent("");
    testProgress2.setTimeSpent("00:30:00");

    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalTimePlayed().equals("00:30:00")));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle invalid time format gracefully")
  void shouldHandleInvalidTimeFormatGracefully() {
    // Arrange
    testProgress1.setTimeSpent("invalid");
    testProgress2.setTimeSpent("01:00:00");

    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert (should only count valid time)
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalTimePlayed().equals("01:00:00")));
  }

  // ========== Max Possible Stars Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should get max possible stars")
  void shouldGetMaxPossibleStars() {
    // Arrange
    List<Level> levels = Arrays.asList(testLevel1, testLevel2);
    when(levelRepository.findAll()).thenReturn(levels);

    // Act
    int result = statisticsService.getMaxPossibleStars();

    // Assert (3 + 3 = 6)
    assertThat(result).isEqualTo(6);
    verify(levelRepository, times(1)).findAll();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return zero when no levels exist")
  void shouldReturnZeroWhenNoLevelsExist() {
    // Arrange
    when(levelRepository.findAll()).thenReturn(Collections.emptyList());

    // Act
    int result = statisticsService.getMaxPossibleStars();

    // Assert
    assertThat(result).isEqualTo(0);
  }

  // ========== Stars Progress Percentage Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should calculate stars progress percentage correctly")
  void shouldCalculateStarsProgressPercentageCorrectly() {
    // Arrange
    testStatistics.setTotalStars(3);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(levelRepository.findAll()).thenReturn(Arrays.asList(testLevel1, testLevel2));

    // Act
    double result = statisticsService.getStarsProgressPercentage(1L);

    // Assert (3/6 * 100 = 50.0)
    assertThat(result).isEqualTo(50.0);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return zero percentage when max stars is zero")
  void shouldReturnZeroPercentageWhenMaxStarsIsZero() {
    // Arrange
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(levelRepository.findAll()).thenReturn(Collections.emptyList());

    // Act
    double result = statisticsService.getStarsProgressPercentage(1L);

    // Assert
    assertThat(result).isEqualTo(0.0);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should round percentage to two decimals")
  void shouldRoundPercentageToTwoDecimals() {
    // Arrange
    testStatistics.setTotalStars(1);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(levelRepository.findAll()).thenReturn(Arrays.asList(testLevel1, testLevel2, testLevel1));

    // Act
    double result = statisticsService.getStarsProgressPercentage(1L);

    // Assert (1/9 * 100 = 11.11...)
    assertThat(result).isEqualTo(11.11);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when statistics not found for percentage")
  void shouldThrowExceptionWhenStatisticsNotFoundForPercentage() {
    // Arrange
    when(statisticsRepository.findByUserId(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> statisticsService.getStarsProgressPercentage(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Statistics")
        .hasMessageContaining("999");
  }

  // ========== Stars Progress DTO Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should get stars progress DTO")
  void shouldGetStarsProgressDto() {
    // Arrange
    testStatistics.setTotalStars(4);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(levelRepository.findAll()).thenReturn(Arrays.asList(testLevel1, testLevel2));

    // Act
    StarsProgressDto result = statisticsService.getStarsProgress(1L);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getCurrentStars()).isEqualTo(4);
    assertThat(result.getMaxPossibleStars()).isEqualTo(6);
    assertThat(result.getProgressPercentage()).isEqualTo(66.67);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when statistics not found for progress DTO")
  void shouldThrowExceptionWhenStatisticsNotFoundForProgressDto() {
    // Arrange
    when(statisticsRepository.findByUserId(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> statisticsService.getStarsProgress(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Statistics")
        .hasMessageContaining("999");
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle 100 percent completion")
  void shouldHandle100PercentCompletion() {
    // Arrange
    testStatistics.setTotalStars(6);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(levelRepository.findAll()).thenReturn(Arrays.asList(testLevel1, testLevel2));

    // Act
    StarsProgressDto result = statisticsService.getStarsProgress(1L);

    // Assert
    assertThat(result.getProgressPercentage()).isEqualTo(100.0);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle zero stars")
  void shouldHandleZeroStars() {
    // Arrange
    testStatistics.setTotalStars(0);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(levelRepository.findAll()).thenReturn(Arrays.asList(testLevel1, testLevel2));

    // Act
    StarsProgressDto result = statisticsService.getStarsProgress(1L);

    // Assert
    assertThat(result.getCurrentStars()).isEqualTo(0);
    assertThat(result.getProgressPercentage()).isEqualTo(0.0);
  }

  // ========== Time Conversion Edge Cases ==========

  @Test
  @Tag("unit")
  @DisplayName("Should handle time conversion with hours overflow")
  void shouldHandleTimeConversionWithHoursOverflow() {
    // Arrange
    testProgress1.setTimeSpent("23:45:30");
    testProgress2.setTimeSpent("01:30:45");

    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert (23:45:30 + 01:30:45 = 25:16:15)
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalTimePlayed().equals("25:16:15")));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle large time values")
  void shouldHandleLargeTimeValues() {
    // Arrange
    testProgress1.setTimeSpent("100:00:00");
    testProgress2.setTimeSpent("200:00:00");

    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalTimePlayed().equals("300:00:00")));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle zero time")
  void shouldHandleZeroTime() {
    // Arrange
    testProgress1.setTimeSpent("00:00:00");
    testProgress2.setTimeSpent("00:00:00");

    List<Progress> progressList = Arrays.asList(testProgress1, testProgress2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(progressRepository.findByUserId(1L)).thenReturn(progressList);
    when(statisticsRepository.findByUserId(1L)).thenReturn(Optional.of(testStatistics));
    when(statisticsRepository.save(any(UserStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    statisticsService.recalculateUserStatistics(1L);

    // Assert
    verify(statisticsRepository).save(argThat(stats -> stats.getTotalTimePlayed().equals("00:00:00")));
  }
}
