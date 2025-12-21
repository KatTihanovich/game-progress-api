package com.esdc.gameapi.controller.unit;

import com.esdc.gameapi.controller.ProgressController;
import com.esdc.gameapi.domain.dto.ProgressDto;
import com.esdc.gameapi.exception.GlobalExceptionHandler;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.service.ProgressService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Progress Controller Unit Tests")
class ProgressControllerTest {

  private MockMvc mockMvc;

  @Mock
  private ProgressService progressService;

  @InjectMocks
  private ProgressController progressController;

  private ObjectMapper objectMapper;

  private ProgressDto testProgress1;
  private ProgressDto testProgress2;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    mockMvc = MockMvcBuilders.standaloneSetup(progressController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    testProgress1 = ProgressDto.builder()
        .levelId(1L)
        .killedEnemiesNumber(10)
        .solvedPuzzlesNumber(5)
        .timeSpent("00:15:30")
        .stars(3)
        .build();

    testProgress2 = ProgressDto.builder()
        .levelId(2L)
        .killedEnemiesNumber(25)
        .solvedPuzzlesNumber(8)
        .timeSpent("00:25:45")
        .stars(5)
        .build();
  }

  // ========== GET User Progress Tests ==========

  @Nested
  @DisplayName("GET /api/progress/{userId} - Get User Progress")
  class GetUserProgressTests {

    @Test
    @Tag("unit")
    @DisplayName("Should return all progress for user")
    void shouldReturnAllProgressForUser() throws Exception {
      // Arrange
      List<ProgressDto> progressList = Arrays.asList(testProgress1, testProgress2);
      when(progressService.getProgressByUserId(1L)).thenReturn(progressList);

      // Act & Assert
      mockMvc.perform(get("/api/progress/1")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(2)))
          .andExpect(jsonPath("$[0].levelId").value(1))
          .andExpect(jsonPath("$[0].killedEnemiesNumber").value(10))
          .andExpect(jsonPath("$[0].solvedPuzzlesNumber").value(5))
          .andExpect(jsonPath("$[0].timeSpent").value("00:15:30"))
          .andExpect(jsonPath("$[0].stars").value(3))
          .andExpect(jsonPath("$[1].levelId").value(2))
          .andExpect(jsonPath("$[1].stars").value(5));

      verify(progressService, times(1)).getProgressByUserId(1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return empty list when user has no progress")
    void shouldReturnEmptyListWhenUserHasNoProgress() throws Exception {
      // Arrange
      when(progressService.getProgressByUserId(1L)).thenReturn(Collections.emptyList());

      // Act & Assert
      mockMvc.perform(get("/api/progress/1")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(0)));

      verify(progressService, times(1)).getProgressByUserId(1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should handle multiple progress records for different levels")
    void shouldHandleMultipleProgressRecordsForDifferentLevels() throws Exception {
      // Arrange
      ProgressDto progress3 = ProgressDto.builder()
          .levelId(3L)
          .killedEnemiesNumber(30)
          .solvedPuzzlesNumber(10)
          .timeSpent("00:30:00")
          .stars(4)
          .build();

      List<ProgressDto> progressList = Arrays.asList(testProgress1, testProgress2, progress3);
      when(progressService.getProgressByUserId(1L)).thenReturn(progressList);

      // Act & Assert
      mockMvc.perform(get("/api/progress/1")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(3)))
          .andExpect(jsonPath("$[0].levelId").value(1))
          .andExpect(jsonPath("$[1].levelId").value(2))
          .andExpect(jsonPath("$[2].levelId").value(3));

      verify(progressService, times(1)).getProgressByUserId(1L);
    }
  }

  // ========== GET Latest Progress Tests ==========

  @Nested
  @DisplayName("GET /api/progress/{userId}/level/{levelId}/latest - Get Latest Progress")
  class GetLatestProgressTests {

    @Test
    @Tag("unit")
    @DisplayName("Should return latest progress for user on specific level")
    void shouldReturnLatestProgressForUserOnSpecificLevel() throws Exception {
      // Arrange
      when(progressService.getLatestProgressByUserAndLevel(1L, 1L))
          .thenReturn(testProgress1);

      // Act & Assert
      mockMvc.perform(get("/api/progress/1/level/1/latest")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.levelId").value(1))
          .andExpect(jsonPath("$.killedEnemiesNumber").value(10))
          .andExpect(jsonPath("$.solvedPuzzlesNumber").value(5))
          .andExpect(jsonPath("$.timeSpent").value("00:15:30"))
          .andExpect(jsonPath("$.stars").value(3));

      verify(progressService, times(1)).getLatestProgressByUserAndLevel(1L, 1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
      // Arrange
      when(progressService.getLatestProgressByUserAndLevel(999L, 1L))
          .thenThrow(new ResourceNotFoundException("User", "id", 999L));

      // Act & Assert
      mockMvc.perform(get("/api/progress/999/level/1/latest")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());

      verify(progressService, times(1)).getLatestProgressByUserAndLevel(999L, 1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 404 when level not found")
    void shouldReturn404WhenLevelNotFound() throws Exception {
      // Arrange
      when(progressService.getLatestProgressByUserAndLevel(1L, 999L))
          .thenThrow(new ResourceNotFoundException("Level", "id", 999L));

      // Act & Assert
      mockMvc.perform(get("/api/progress/1/level/999/latest")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());

      verify(progressService, times(1)).getLatestProgressByUserAndLevel(1L, 999L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 404 when no progress exists for user on level")
    void shouldReturn404WhenNoProgressExistsForUserOnLevel() throws Exception {
      // Arrange
      when(progressService.getLatestProgressByUserAndLevel(1L, 1L))
          .thenThrow(new ResourceNotFoundException("Progress", "userId and levelId", "1, 1"));

      // Act & Assert
      mockMvc.perform(get("/api/progress/1/level/1/latest")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());

      verify(progressService, times(1)).getLatestProgressByUserAndLevel(1L, 1L);
    }
  }

  // ========== GET Total Stars Tests ==========

  @Nested
  @DisplayName("GET /api/progress/{userId}/level/{levelId}/total-stars - Get Total Stars")
  class GetTotalStarsTests {

    @Test
    @Tag("unit")
    @DisplayName("Should return total stars for user on specific level")
    void shouldReturnTotalStarsForUserOnSpecificLevel() throws Exception {
      // Arrange
      when(progressService.getTotalStarsByUserAndLevel(1L, 1L)).thenReturn(15);

      // Act & Assert
      mockMvc.perform(get("/api/progress/1/level/1/total-stars")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(content().string("15"));

      verify(progressService, times(1)).getTotalStarsByUserAndLevel(1L, 1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return zero stars when user has no progress on level")
    void shouldReturnZeroStarsWhenUserHasNoProgressOnLevel() throws Exception {
      // Arrange
      when(progressService.getTotalStarsByUserAndLevel(1L, 1L)).thenReturn(0);

      // Act & Assert
      mockMvc.perform(get("/api/progress/1/level/1/total-stars")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().string("0"));

      verify(progressService, times(1)).getTotalStarsByUserAndLevel(1L, 1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should handle large star counts")
    void shouldHandleLargeStarCounts() throws Exception {
      // Arrange
      when(progressService.getTotalStarsByUserAndLevel(1L, 1L)).thenReturn(9999);

      // Act & Assert
      mockMvc.perform(get("/api/progress/1/level/1/total-stars")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().string("9999"));

      verify(progressService, times(1)).getTotalStarsByUserAndLevel(1L, 1L);
    }
  }

  // ========== POST Create Progress Tests ==========

  @Nested
  @DisplayName("POST /api/progress - Create Progress")
  class CreateProgressTests {

    @Test
    @Tag("unit")
    @DisplayName("Should create progress successfully")
    void shouldCreateProgressSuccessfully() throws Exception {
      // Arrange
      ProgressDto createRequest = ProgressDto.builder()
          .levelId(1L)
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(3)
          .build();

      ProgressDto createdProgress = ProgressDto.builder()
          .levelId(1L)
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(3)
          .build();

      when(progressService.createProgress(eq(1L), any(ProgressDto.class)))
          .thenReturn(createdProgress);

      // Act & Assert
      mockMvc.perform(post("/api/progress")
              .param("userId", "1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(createRequest)))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.levelId").value(1))
          .andExpect(jsonPath("$.killedEnemiesNumber").value(10))
          .andExpect(jsonPath("$.solvedPuzzlesNumber").value(5))
          .andExpect(jsonPath("$.timeSpent").value("00:15:30"))
          .andExpect(jsonPath("$.stars").value(3));

      verify(progressService, times(1)).createProgress(eq(1L), any(ProgressDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should create progress with zero enemies and puzzles")
    void shouldCreateProgressWithZeroEnemiesAndPuzzles() throws Exception {
      // Arrange
      ProgressDto createRequest = ProgressDto.builder()
          .levelId(1L)
          .killedEnemiesNumber(0)
          .solvedPuzzlesNumber(0)
          .timeSpent("00:10:00")
          .stars(1)
          .build();

      ProgressDto createdProgress = ProgressDto.builder()
          .levelId(1L)
          .killedEnemiesNumber(0)
          .solvedPuzzlesNumber(0)
          .timeSpent("00:10:00")
          .stars(1)
          .build();

      when(progressService.createProgress(eq(1L), any(ProgressDto.class)))
          .thenReturn(createdProgress);

      // Act & Assert
      mockMvc.perform(post("/api/progress")
              .param("userId", "1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(createRequest)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.killedEnemiesNumber").value(0))
          .andExpect(jsonPath("$.solvedPuzzlesNumber").value(0));

      verify(progressService, times(1)).createProgress(eq(1L), any(ProgressDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
      // Arrange
      ProgressDto createRequest = ProgressDto.builder()
          .levelId(1L)
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(3)
          .build();

      when(progressService.createProgress(eq(999L), any(ProgressDto.class)))
          .thenThrow(new ResourceNotFoundException("User", "id", 999L));

      // Act & Assert
      mockMvc.perform(post("/api/progress")
              .param("userId", "999")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(createRequest)))
          .andExpect(status().isNotFound());

      verify(progressService, times(1)).createProgress(eq(999L), any(ProgressDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 404 when level not found")
    void shouldReturn404WhenLevelNotFound() throws Exception {
      // Arrange
      ProgressDto createRequest = ProgressDto.builder()
          .levelId(999L)
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(3)
          .build();

      when(progressService.createProgress(eq(1L), any(ProgressDto.class)))
          .thenThrow(new ResourceNotFoundException("Level", "id", 999L));

      // Act & Assert
      mockMvc.perform(post("/api/progress")
              .param("userId", "1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(createRequest)))
          .andExpect(status().isNotFound());

      verify(progressService, times(1)).createProgress(eq(1L), any(ProgressDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should return 400 when stars exceed maximum")
    void shouldReturn400WhenStarsExceedMaximum() throws Exception {
      // Arrange
      ProgressDto createRequest = ProgressDto.builder()
          .levelId(1L)
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(10)
          .build();

      when(progressService.createProgress(eq(1L), any(ProgressDto.class)))
          .thenThrow(new IllegalArgumentException("Stars (10) cannot exceed maximum stars on level (5)"));

      // Act & Assert
      mockMvc.perform(post("/api/progress")
              .param("userId", "1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(createRequest)))
          .andExpect(status().isBadRequest());

      verify(progressService, times(1)).createProgress(eq(1L), any(ProgressDto.class));
    }

    @Test
    @Tag("unit")
    @DisplayName("Should create progress with maximum values")
    void shouldCreateProgressWithMaximumValues() throws Exception {
      // Arrange
      ProgressDto createRequest = ProgressDto.builder()
          .levelId(1L)
          .killedEnemiesNumber(Integer.MAX_VALUE)
          .solvedPuzzlesNumber(Integer.MAX_VALUE)
          .timeSpent("99:59:59")
          .stars(5)
          .build();

      ProgressDto createdProgress = ProgressDto.builder()
          .levelId(1L)
          .killedEnemiesNumber(Integer.MAX_VALUE)
          .solvedPuzzlesNumber(Integer.MAX_VALUE)
          .timeSpent("99:59:59")
          .stars(5)
          .build();

      when(progressService.createProgress(eq(1L), any(ProgressDto.class)))
          .thenReturn(createdProgress);

      // Act & Assert
      mockMvc.perform(post("/api/progress")
              .param("userId", "1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(createRequest)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.killedEnemiesNumber").value(Integer.MAX_VALUE))
          .andExpect(jsonPath("$.solvedPuzzlesNumber").value(Integer.MAX_VALUE));

      verify(progressService, times(1)).createProgress(eq(1L), any(ProgressDto.class));
    }
  }

  // ========== Edge Cases Tests ==========

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @Tag("unit")
    @DisplayName("Should handle progress with all fields at minimum values")
    void shouldHandleProgressWithAllFieldsAtMinimumValues() throws Exception {
      // Arrange
      ProgressDto minProgress = ProgressDto.builder()
          .levelId(1L)
          .killedEnemiesNumber(0)
          .solvedPuzzlesNumber(0)
          .timeSpent("00:00:00")
          .stars(0)
          .build();

      when(progressService.getLatestProgressByUserAndLevel(1L, 1L))
          .thenReturn(minProgress);

      // Act & Assert
      mockMvc.perform(get("/api/progress/1/level/1/latest")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.killedEnemiesNumber").value(0))
          .andExpect(jsonPath("$.solvedPuzzlesNumber").value(0))
          .andExpect(jsonPath("$.stars").value(0))
          .andExpect(jsonPath("$.timeSpent").value("00:00:00"));

      verify(progressService, times(1)).getLatestProgressByUserAndLevel(1L, 1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should handle very long time spent")
    void shouldHandleVeryLongTimeSpent() throws Exception {
      // Arrange
      ProgressDto longTimeProgress = ProgressDto.builder()
          .levelId(1L)
          .killedEnemiesNumber(100)
          .solvedPuzzlesNumber(50)
          .timeSpent("99:59:59")
          .stars(3)
          .build();

      when(progressService.getLatestProgressByUserAndLevel(1L, 1L))
          .thenReturn(longTimeProgress);

      // Act & Assert
      mockMvc.perform(get("/api/progress/1/level/1/latest")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.timeSpent").value("99:59:59"));

      verify(progressService, times(1)).getLatestProgressByUserAndLevel(1L, 1L);
    }

    @Test
    @Tag("unit")
    @DisplayName("Should handle multiple progress records for same level")
    void shouldHandleMultipleProgressRecordsForSameLevel() throws Exception {
      // Arrange
      List<ProgressDto> sameLevelProgress = Arrays.asList(
          ProgressDto.builder()
              .levelId(1L)
              .killedEnemiesNumber(5)
              .solvedPuzzlesNumber(2)
              .timeSpent("00:10:00")
              .stars(1)
              .build(),
          ProgressDto.builder()
              .levelId(1L)
              .killedEnemiesNumber(10)
              .solvedPuzzlesNumber(5)
              .timeSpent("00:15:30")
              .stars(3)
              .build()
      );

      when(progressService.getProgressByUserId(1L)).thenReturn(sameLevelProgress);

      // Act & Assert
      mockMvc.perform(get("/api/progress/1")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(2)))
          .andExpect(jsonPath("$[0].levelId").value(1))
          .andExpect(jsonPath("$[1].levelId").value(1))
          .andExpect(jsonPath("$[0].stars").value(1))
          .andExpect(jsonPath("$[1].stars").value(3));

      verify(progressService, times(1)).getProgressByUserId(1L);
    }
  }
}
