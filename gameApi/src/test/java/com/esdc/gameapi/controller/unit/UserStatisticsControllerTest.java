package com.esdc.gameapi.controller.unit;

import com.esdc.gameapi.controller.UserStatisticsController;
import com.esdc.gameapi.domain.dto.StarsProgressDto;
import com.esdc.gameapi.domain.dto.UserStatisticsDto;
import com.esdc.gameapi.exception.GlobalExceptionHandler;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.service.UserStatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("User Statistics Controller Unit Tests")
class UserStatisticsControllerTest {

  private MockMvc mockMvc;

  @Mock
  private UserStatisticsService statisticsService;

  @InjectMocks
  private UserStatisticsController statisticsController;

  private ObjectMapper objectMapper;

  private UserStatisticsDto testStatistics;
  private StarsProgressDto testStarsProgress;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    // ❗Standalone setup - без Spring контекста
    mockMvc = MockMvcBuilders.standaloneSetup(statisticsController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    testStatistics = UserStatisticsDto.builder()
        .totalLevelsCompleted(10)
        .totalTimePlayed("01:00:00")
        .totalKilledEnemies(150)
        .totalSolvedPuzzles(45)
        .totalStars(120)
        .build();

    testStarsProgress = StarsProgressDto.builder()
        .currentStars(120)
        .maxPossibleStars(300)
        .progressPercentage(40.0)
        .build();
  }

  // ========== GET Statistics Tests ==========

  @Nested
  @DisplayName("GET /api/statistics/{userId}")
  class GetStatisticsTests {

    @Test
    @Tag("unit")
    @DisplayName("Should return user statistics successfully")
    void shouldReturnUserStatisticsSuccessfully() throws Exception {
      // Arrange
      when(statisticsService.getStatisticsByUserId(1L))
          .thenReturn(Optional.of(testStatistics));

      // Act & Assert
      mockMvc.perform(get("/api/statistics/1")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.totalLevelsCompleted").value(10))
          .andExpect(jsonPath("$.totalTimePlayed").value("01:00:00"))
          .andExpect(jsonPath("$.totalKilledEnemies").value(150))
          .andExpect(jsonPath("$.totalSolvedPuzzles").value(45))
          .andExpect(jsonPath("$.totalStars").value(120));

      verify(statisticsService, times(1)).getStatisticsByUserId(1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 404 when statistics not found")
    void shouldReturn404WhenStatisticsNotFound() throws Exception {
      // Arrange
      when(statisticsService.getStatisticsByUserId(999L))
          .thenReturn(Optional.empty());

      // Act & Assert
      mockMvc.perform(get("/api/statistics/999")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());

      verify(statisticsService, times(1)).getStatisticsByUserId(999L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return statistics for different user")
    void shouldReturnStatisticsForDifferentUser() throws Exception {
      // Arrange
      UserStatisticsDto anotherUserStats = UserStatisticsDto.builder()
          .totalLevelsCompleted(20)
          .totalTimePlayed("02:00:00")
          .totalKilledEnemies(300)
          .totalSolvedPuzzles(90)
          .totalStars(240)
          .build();

      when(statisticsService.getStatisticsByUserId(5L))
          .thenReturn(Optional.of(anotherUserStats));

      // Act & Assert
      mockMvc.perform(get("/api/statistics/5")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalLevelsCompleted").value(20))
          .andExpect(jsonPath("$.totalTimePlayed").value("02:00:00"))
          .andExpect(jsonPath("$.totalStars").value(240));

      verify(statisticsService, times(1)).getStatisticsByUserId(5L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return statistics with zero values")
    void shouldReturnStatisticsWithZeroValues() throws Exception {
      // Arrange
      UserStatisticsDto zeroStats = UserStatisticsDto.builder()
          .totalLevelsCompleted(0)
          .totalTimePlayed("00:00:00")
          .totalKilledEnemies(0)
          .totalSolvedPuzzles(0)
          .totalStars(0)
          .build();

      when(statisticsService.getStatisticsByUserId(10L))
          .thenReturn(Optional.of(zeroStats));

      // Act & Assert
      mockMvc.perform(get("/api/statistics/10")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalLevelsCompleted").value(0))
          .andExpect(jsonPath("$.totalTimePlayed").value("00:00:00"))
          .andExpect(jsonPath("$.totalKilledEnemies").value(0))
          .andExpect(jsonPath("$.totalSolvedPuzzles").value(0))
          .andExpect(jsonPath("$.totalStars").value(0));

      verify(statisticsService, times(1)).getStatisticsByUserId(10L);
    }
  }

  // ========== POST Recalculate Statistics Tests ==========

  @Nested
  @DisplayName("POST /api/statistics/{userId}/recalculate")
  class RecalculateStatisticsTests {

    @Test
    @Tag("unit")
    @DisplayName("Should recalculate statistics successfully")
    void shouldRecalculateStatisticsSuccessfully() throws Exception {
      // Arrange
      UserStatisticsDto recalculatedStats = UserStatisticsDto.builder()
          .totalLevelsCompleted(15)
          .totalTimePlayed("01:30:00")
          .totalKilledEnemies(200)
          .totalSolvedPuzzles(60)
          .totalStars(180)
          .build();

      when(statisticsService.recalculateUserStatistics(1L))
          .thenReturn(recalculatedStats);

      // Act & Assert
      mockMvc.perform(post("/api/statistics/1/recalculate")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.totalLevelsCompleted").value(15))
          .andExpect(jsonPath("$.totalTimePlayed").value("01:30:00"))
          .andExpect(jsonPath("$.totalKilledEnemies").value(200))
          .andExpect(jsonPath("$.totalSolvedPuzzles").value(60))
          .andExpect(jsonPath("$.totalStars").value(180));

      verify(statisticsService, times(1)).recalculateUserStatistics(1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should recalculate statistics for user with no progress")
    void shouldRecalculateStatisticsForUserWithNoProgress() throws Exception {
      // Arrange
      UserStatisticsDto emptyStats = UserStatisticsDto.builder()
          .totalLevelsCompleted(0)
          .totalTimePlayed("00:00:00")
          .totalKilledEnemies(0)
          .totalSolvedPuzzles(0)
          .totalStars(0)
          .build();

      when(statisticsService.recalculateUserStatistics(2L))
          .thenReturn(emptyStats);

      // Act & Assert
      mockMvc.perform(post("/api/statistics/2/recalculate")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalLevelsCompleted").value(0))
          .andExpect(jsonPath("$.totalTimePlayed").value("00:00:00"))
          .andExpect(jsonPath("$.totalStars").value(0));

      verify(statisticsService, times(1)).recalculateUserStatistics(2L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should handle recalculation for non-existent user")
    void shouldHandleRecalculationForNonExistentUser() throws Exception {
      // Arrange
      when(statisticsService.recalculateUserStatistics(999L))
          .thenThrow(new ResourceNotFoundException("User", "id", 999L));

      // Act & Assert
      mockMvc.perform(post("/api/statistics/999/recalculate")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());

      verify(statisticsService, times(1)).recalculateUserStatistics(999L);
    }
  }

  // ========== GET Max Possible Stars Tests ==========

  @Nested
  @DisplayName("GET /api/statistics/max-stars")
  class GetMaxPossibleStarsTests {

    @Test
    @Tag("unit")
    @DisplayName("Should return maximum possible stars")
    void shouldReturnMaximumPossibleStars() throws Exception {
      // Arrange
      when(statisticsService.getMaxPossibleStars()).thenReturn(300);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/max-stars")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.maxPossibleStars").value(300));

      verify(statisticsService, times(1)).getMaxPossibleStars();
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return zero when no levels exist")
    void shouldReturnZeroWhenNoLevelsExist() throws Exception {
      // Arrange
      when(statisticsService.getMaxPossibleStars()).thenReturn(0);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/max-stars")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.maxPossibleStars").value(0));

      verify(statisticsService, times(1)).getMaxPossibleStars();
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return large number of maximum stars")
    void shouldReturnLargeNumberOfMaximumStars() throws Exception {
      // Arrange
      when(statisticsService.getMaxPossibleStars()).thenReturn(10000);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/max-stars")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.maxPossibleStars").value(10000));

      verify(statisticsService, times(1)).getMaxPossibleStars();
    }
  }

  // ========== GET Stars Progress Tests ==========

  @Nested
  @DisplayName("GET /api/statistics/{userId}/stars-progress")
  class GetStarsProgressTests {

    @Test
    @Tag("unit")
    @DisplayName("Should return stars progress successfully")
    void shouldReturnStarsProgressSuccessfully() throws Exception {
      // Arrange
      when(statisticsService.getStarsProgress(1L)).thenReturn(testStarsProgress);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/1/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.currentStars").value(120))
          .andExpect(jsonPath("$.maxPossibleStars").value(300))
          .andExpect(jsonPath("$.progressPercentage").value(40.0));

      verify(statisticsService, times(1)).getStarsProgress(1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 404 when stars progress not found")
    void shouldReturn404WhenStarsProgressNotFound() throws Exception {
      // Arrange
      when(statisticsService.getStarsProgress(999L))
          .thenThrow(new ResourceNotFoundException("Statistics", "userId", 999L));

      // Act & Assert
      mockMvc.perform(get("/api/statistics/999/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());

      verify(statisticsService, times(1)).getStarsProgress(999L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return zero progress for new user")
    void shouldReturnZeroProgressForNewUser() throws Exception {
      // Arrange
      StarsProgressDto zeroProgress = StarsProgressDto.builder()
          .currentStars(0)
          .maxPossibleStars(300)
          .progressPercentage(0.0)
          .build();

      when(statisticsService.getStarsProgress(2L)).thenReturn(zeroProgress);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/2/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.currentStars").value(0))
          .andExpect(jsonPath("$.maxPossibleStars").value(300))
          .andExpect(jsonPath("$.progressPercentage").value(0.0));

      verify(statisticsService, times(1)).getStarsProgress(2L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 100% progress when all stars collected")
    void shouldReturn100PercentProgressWhenAllStarsCollected() throws Exception {
      // Arrange
      StarsProgressDto fullProgress = StarsProgressDto.builder()
          .currentStars(300)
          .maxPossibleStars(300)
          .progressPercentage(100.0)
          .build();

      when(statisticsService.getStarsProgress(3L)).thenReturn(fullProgress);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/3/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.currentStars").value(300))
          .andExpect(jsonPath("$.maxPossibleStars").value(300))
          .andExpect(jsonPath("$.progressPercentage").value(100.0));

      verify(statisticsService, times(1)).getStarsProgress(3L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return partial progress")
    void shouldReturnPartialProgress() throws Exception {
      // Arrange
      StarsProgressDto partialProgress = StarsProgressDto.builder()
          .currentStars(75)
          .maxPossibleStars(300)
          .progressPercentage(25.0)
          .build();

      when(statisticsService.getStarsProgress(4L)).thenReturn(partialProgress);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/4/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.currentStars").value(75))
          .andExpect(jsonPath("$.progressPercentage").value(25.0));

      verify(statisticsService, times(1)).getStarsProgress(4L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should handle general exception")
    void shouldHandleGeneralException() throws Exception {
      // Arrange
      when(statisticsService.getStarsProgress(5L))
          .thenThrow(new RuntimeException("Database error"));

      // Act & Assert
      mockMvc.perform(get("/api/statistics/5/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());

      verify(statisticsService, times(1)).getStarsProgress(5L);
    }
  }

  // ========== Edge Cases Tests ==========

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @Tag("unit")
    @DisplayName("Should handle very large statistics values")
    void shouldHandleVeryLargeStatisticsValues() throws Exception {
      // Arrange
      UserStatisticsDto largeStats = UserStatisticsDto.builder()
          .totalLevelsCompleted(999999)
          .totalTimePlayed("999:59:59")
          .totalKilledEnemies(999999)
          .totalSolvedPuzzles(999999)
          .totalStars(999999)
          .build();

      when(statisticsService.getStatisticsByUserId(100L))
          .thenReturn(Optional.of(largeStats));

      // Act & Assert
      mockMvc.perform(get("/api/statistics/100")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalLevelsCompleted").value(999999))
          .andExpect(jsonPath("$.totalTimePlayed").value("999:59:59"))
          .andExpect(jsonPath("$.totalKilledEnemies").value(999999));

      verify(statisticsService, times(1)).getStatisticsByUserId(100L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should handle progress with decimal percentage")
    void shouldHandleProgressWithDecimalPercentage() throws Exception {
      // Arrange
      StarsProgressDto decimalProgress = StarsProgressDto.builder()
          .currentStars(123)
          .maxPossibleStars(456)
          .progressPercentage(26.97)
          .build();

      when(statisticsService.getStarsProgress(6L)).thenReturn(decimalProgress);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/6/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.progressPercentage").value(26.97));

      verify(statisticsService, times(1)).getStarsProgress(6L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should handle multiple recalculations for same user")
    void shouldHandleMultipleRecalculationsForSameUser() throws Exception {
      // Arrange
      UserStatisticsDto stats1 = UserStatisticsDto.builder()
          .totalLevelsCompleted(10)
          .totalTimePlayed("01:00:00")
          .totalKilledEnemies(100)
          .totalSolvedPuzzles(50)
          .totalStars(100)
          .build();

      UserStatisticsDto stats2 = UserStatisticsDto.builder()
          .totalLevelsCompleted(15)
          .totalTimePlayed("01:30:00")
          .totalKilledEnemies(150)
          .totalSolvedPuzzles(75)
          .totalStars(150)
          .build();

      when(statisticsService.recalculateUserStatistics(1L))
          .thenReturn(stats1)
          .thenReturn(stats2);

      // Act & Assert - First recalculation
      mockMvc.perform(post("/api/statistics/1/recalculate")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalLevelsCompleted").value(10));

      // Act & Assert - Second recalculation
      mockMvc.perform(post("/api/statistics/1/recalculate")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalLevelsCompleted").value(15));

      verify(statisticsService, times(2)).recalculateUserStatistics(1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should handle time with hours exceeding 24")
    void shouldHandleTimeWithHoursExceeding24() throws Exception {
      // Arrange
      UserStatisticsDto longPlayStats = UserStatisticsDto.builder()
          .totalLevelsCompleted(50)
          .totalTimePlayed("100:30:45")
          .totalKilledEnemies(500)
          .totalSolvedPuzzles(200)
          .totalStars(500)
          .build();

      when(statisticsService.getStatisticsByUserId(7L))
          .thenReturn(Optional.of(longPlayStats));

      // Act & Assert
      mockMvc.perform(get("/api/statistics/7")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalTimePlayed").value("100:30:45"));

      verify(statisticsService, times(1)).getStatisticsByUserId(7L);
    }
  }
}
