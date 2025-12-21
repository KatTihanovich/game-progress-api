package com.esdc.gameapi.controller.unit;

import com.esdc.gameapi.controller.LevelController;
import com.esdc.gameapi.domain.dto.LevelDto;
import com.esdc.gameapi.exception.GlobalExceptionHandler;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.service.LevelService;
import com.esdc.gameapi.service.AdminAuthService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Level Controller Unit Tests")
class LevelControllerTest {

  private MockMvc mockMvc;

  @Mock
  private LevelService levelService;

  @Mock
  private AdminAuthService adminAuthService;

  @InjectMocks
  private LevelController levelController;

  private ObjectMapper objectMapper;

  private static final String ADMIN_PASSWORD_HEADER = "Admin-Password";
  private static final String CORRECT_ADMIN_PASSWORD = "159357";
  private static final String INCORRECT_ADMIN_PASSWORD = "wrongPassword";

  private LevelDto testLevel1;
  private LevelDto testLevel2;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    mockMvc = MockMvcBuilders.standaloneSetup(levelController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    testLevel1 = LevelDto.builder()
        .id(1L)
        .levelName("Level 1")
        .starsOnLevel(3)
        .bossOnLevel(false)
        .build();

    testLevel2 = LevelDto.builder()
        .id(2L)
        .levelName("Level 2 - Boss")
        .starsOnLevel(5)
        .bossOnLevel(true)
        .build();
  }

  // ========== GET All Levels Tests ==========

  @Nested
  @DisplayName("GET /api/levels")
  class GetAllLevelsTests {

    @Test
    @DisplayName("Should return all levels")
    void shouldReturnAllLevels() throws Exception {
      List<LevelDto> levels = Arrays.asList(testLevel1, testLevel2);
      when(levelService.getAllLevels()).thenReturn(levels);

      mockMvc.perform(get("/api/levels")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(2)))
          .andExpect(jsonPath("$[0].id").value(1))
          .andExpect(jsonPath("$[0].levelName").value("Level 1"))
          .andExpect(jsonPath("$[0].starsOnLevel").value(3))
          .andExpect(jsonPath("$[0].bossOnLevel").value(false))
          .andExpect(jsonPath("$[1].id").value(2))
          .andExpect(jsonPath("$[1].levelName").value("Level 2 - Boss"))
          .andExpect(jsonPath("$[1].starsOnLevel").value(5))
          .andExpect(jsonPath("$[1].bossOnLevel").value(true));

      verify(levelService, times(1)).getAllLevels();
    }

    @Test
    @DisplayName("Should return empty list when no levels exist")
    void shouldReturnEmptyListWhenNoLevelsExist() throws Exception {
      when(levelService.getAllLevels()).thenReturn(Collections.emptyList());

      mockMvc.perform(get("/api/levels")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(0)));

      verify(levelService, times(1)).getAllLevels();
    }
  }

  // ========== GET Level By ID Tests ==========

  @Nested
  @DisplayName("GET /api/levels/{id}")
  class GetLevelByIdTests {

    @Test
    @DisplayName("Should return level by id")
    void shouldReturnLevelById() throws Exception {
      when(levelService.getLevelById(1L)).thenReturn(testLevel1);

      mockMvc.perform(get("/api/levels/1")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.levelName").value("Level 1"))
          .andExpect(jsonPath("$.starsOnLevel").value(3))
          .andExpect(jsonPath("$.bossOnLevel").value(false));

      verify(levelService, times(1)).getLevelById(1L);
    }

    @Test
    @DisplayName("Should return 404 when level not found")
    void shouldReturn404WhenLevelNotFound() throws Exception {
      when(levelService.getLevelById(999L))
          .thenThrow(new ResourceNotFoundException("Level", "id", 999L));

      mockMvc.perform(get("/api/levels/999")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());

      verify(levelService, times(1)).getLevelById(999L);
    }
  }

  // ========== POST Create Level Tests ==========

  @Nested
  @DisplayName("POST /api/levels/create")
  class CreateLevelTests {

    @Test
    @DisplayName("Should create level with valid admin password")
    void shouldCreateLevelWithValidAdminPassword() throws Exception {
      LevelDto newLevel = LevelDto.builder()
          .levelName("Level 3")
          .starsOnLevel(4)
          .bossOnLevel(false)
          .build();

      LevelDto createdLevel = LevelDto.builder()
          .id(3L)
          .levelName("Level 3")
          .starsOnLevel(4)
          .bossOnLevel(false)
          .build();

      when(levelService.createLevel(any(LevelDto.class))).thenReturn(createdLevel);
      doNothing().when(adminAuthService).validateAdminPassword(CORRECT_ADMIN_PASSWORD);

      mockMvc.perform(post("/api/levels/create")
              .header(ADMIN_PASSWORD_HEADER, CORRECT_ADMIN_PASSWORD)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(newLevel)))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").value(3))
          .andExpect(jsonPath("$.levelName").value("Level 3"))
          .andExpect(jsonPath("$.starsOnLevel").value(4))
          .andExpect(jsonPath("$.bossOnLevel").value(false));

      verify(adminAuthService, times(1))
          .validateAdminPassword(CORRECT_ADMIN_PASSWORD);
      verify(levelService, times(1)).createLevel(any(LevelDto.class));
    }

    @Test
    @DisplayName("Should reject creation with invalid admin password")
    void shouldRejectCreationWithInvalidAdminPassword() throws Exception {
      LevelDto newLevel = LevelDto.builder()
          .levelName("Level 3")
          .starsOnLevel(4)
          .bossOnLevel(false)
          .build();

      doThrow(new com.esdc.gameapi.exception.UnauthorizedException("Invalid admin password"))
          .when(adminAuthService).validateAdminPassword(INCORRECT_ADMIN_PASSWORD);

      mockMvc.perform(post("/api/levels/create")
              .header(ADMIN_PASSWORD_HEADER, INCORRECT_ADMIN_PASSWORD)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(newLevel)))
          .andExpect(status().isUnauthorized());

      verify(adminAuthService, times(1))
          .validateAdminPassword(INCORRECT_ADMIN_PASSWORD);
      verify(levelService, never()).createLevel(any(LevelDto.class));
    }

    @Test
    @DisplayName("Should reject creation without admin password header")
    void shouldRejectCreationWithoutAdminPasswordHeader() throws Exception {
      LevelDto newLevel = LevelDto.builder()
          .levelName("Level 3")
          .starsOnLevel(4)
          .bossOnLevel(false)
          .build();

      mockMvc.perform(post("/api/levels/create")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(newLevel)))
          .andExpect(status().isInternalServerError());

      verify(levelService, never()).createLevel(any(LevelDto.class));
    }

    @Test
    @DisplayName("Should create boss level")
    void shouldCreateBossLevel() throws Exception {
      LevelDto bossLevel = LevelDto.builder()
          .levelName("Boss Level")
          .starsOnLevel(10)
          .bossOnLevel(true)
          .build();

      LevelDto createdBossLevel = LevelDto.builder()
          .id(4L)
          .levelName("Boss Level")
          .starsOnLevel(10)
          .bossOnLevel(true)
          .build();

      when(levelService.createLevel(any(LevelDto.class))).thenReturn(createdBossLevel);
      doNothing().when(adminAuthService).validateAdminPassword(CORRECT_ADMIN_PASSWORD);

      mockMvc.perform(post("/api/levels/create")
              .header(ADMIN_PASSWORD_HEADER, CORRECT_ADMIN_PASSWORD)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(bossLevel)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.bossOnLevel").value(true))
          .andExpect(jsonPath("$.starsOnLevel").value(10));

      verify(adminAuthService, times(1))
          .validateAdminPassword(CORRECT_ADMIN_PASSWORD);
      verify(levelService, times(1)).createLevel(any(LevelDto.class));
    }
  }

  // ========== PUT Update Level Tests ==========

  @Nested
  @DisplayName("PUT /api/levels/update/{id}")
  class UpdateLevelTests {

    @Test
    @DisplayName("Should update level with valid admin password")
    void shouldUpdateLevelWithValidAdminPassword() throws Exception {
      LevelDto updateDto = LevelDto.builder()
          .levelName("Updated Level")
          .starsOnLevel(7)
          .bossOnLevel(true)
          .build();

      LevelDto updatedLevel = LevelDto.builder()
          .id(1L)
          .levelName("Updated Level")
          .starsOnLevel(7)
          .bossOnLevel(true)
          .build();

      when(levelService.updateLevel(eq(1L), any(LevelDto.class))).thenReturn(updatedLevel);
      doNothing().when(adminAuthService).validateAdminPassword(CORRECT_ADMIN_PASSWORD);

      mockMvc.perform(put("/api/levels/update/1")
              .header(ADMIN_PASSWORD_HEADER, CORRECT_ADMIN_PASSWORD)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.levelName").value("Updated Level"))
          .andExpect(jsonPath("$.starsOnLevel").value(7))
          .andExpect(jsonPath("$.bossOnLevel").value(true));

      verify(adminAuthService, times(1))
          .validateAdminPassword(CORRECT_ADMIN_PASSWORD);
      verify(levelService, times(1))
          .updateLevel(eq(1L), any(LevelDto.class));
    }

    @Test
    @DisplayName("Should reject update with invalid admin password")
    void shouldRejectUpdateWithInvalidAdminPassword() throws Exception {
      LevelDto updateDto = LevelDto.builder()
          .levelName("Updated Level")
          .starsOnLevel(7)
          .bossOnLevel(true)
          .build();

      doThrow(new com.esdc.gameapi.exception.UnauthorizedException("Invalid admin password"))
          .when(adminAuthService).validateAdminPassword(INCORRECT_ADMIN_PASSWORD);

      mockMvc.perform(put("/api/levels/update/1")
              .header(ADMIN_PASSWORD_HEADER, INCORRECT_ADMIN_PASSWORD)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isUnauthorized());

      verify(adminAuthService, times(1))
          .validateAdminPassword(INCORRECT_ADMIN_PASSWORD);
      verify(levelService, never()).updateLevel(any(), any());
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent level")
    void shouldReturn404WhenUpdatingNonExistentLevel() throws Exception {
      LevelDto updateDto = LevelDto.builder()
          .levelName("Updated Level")
          .starsOnLevel(7)
          .bossOnLevel(true)
          .build();

      when(levelService.updateLevel(eq(999L), any(LevelDto.class)))
          .thenThrow(new ResourceNotFoundException("Level", "id", 999L));
      doNothing().when(adminAuthService).validateAdminPassword(CORRECT_ADMIN_PASSWORD);

      mockMvc.perform(put("/api/levels/update/999")
              .header(ADMIN_PASSWORD_HEADER, CORRECT_ADMIN_PASSWORD)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isNotFound());

      verify(adminAuthService, times(1))
          .validateAdminPassword(CORRECT_ADMIN_PASSWORD);
      verify(levelService, times(1))
          .updateLevel(eq(999L), any(LevelDto.class));
    }
  }

  // ========== DELETE Level Tests ==========

  @Nested
  @DisplayName("DELETE /api/levels/delete/{id}")
  class DeleteLevelTests {

    @Test
    @DisplayName("Should delete level with valid admin password")
    void shouldDeleteLevelWithValidAdminPassword() throws Exception {
      doNothing().when(levelService).deleteLevel(1L);
      doNothing().when(adminAuthService).validateAdminPassword(CORRECT_ADMIN_PASSWORD);

      mockMvc.perform(delete("/api/levels/delete/1")
              .header(ADMIN_PASSWORD_HEADER, CORRECT_ADMIN_PASSWORD))
          .andExpect(status().isNoContent());

      verify(adminAuthService, times(1))
          .validateAdminPassword(CORRECT_ADMIN_PASSWORD);
      verify(levelService, times(1)).deleteLevel(1L);
    }

    @Test
    @DisplayName("Should reject deletion with invalid admin password")
    void shouldRejectDeletionWithInvalidAdminPassword() throws Exception {
      doThrow(new com.esdc.gameapi.exception.UnauthorizedException("Invalid admin password"))
          .when(adminAuthService).validateAdminPassword(INCORRECT_ADMIN_PASSWORD);

      mockMvc.perform(delete("/api/levels/delete/1")
              .header(ADMIN_PASSWORD_HEADER, INCORRECT_ADMIN_PASSWORD))
          .andExpect(status().isUnauthorized());

      verify(adminAuthService, times(1))
          .validateAdminPassword(INCORRECT_ADMIN_PASSWORD);
      verify(levelService, never()).deleteLevel(any());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent level")
    void shouldReturn404WhenDeletingNonExistentLevel() throws Exception {
      doNothing().when(adminAuthService).validateAdminPassword(CORRECT_ADMIN_PASSWORD);
      doThrow(new ResourceNotFoundException("Level", "id", 999L))
          .when(levelService).deleteLevel(999L);

      mockMvc.perform(delete("/api/levels/delete/999")
              .header(ADMIN_PASSWORD_HEADER, CORRECT_ADMIN_PASSWORD))
          .andExpect(status().isNotFound());

      verify(adminAuthService, times(1))
          .validateAdminPassword(CORRECT_ADMIN_PASSWORD);
      verify(levelService, times(1)).deleteLevel(999L);
    }

    @Test
    @DisplayName("Should reject deletion without admin password header")
    void shouldRejectDeletionWithoutAdminPasswordHeader() throws Exception {
      mockMvc.perform(delete("/api/levels/delete/1"))
          .andExpect(status().isInternalServerError());

      verify(levelService, never()).deleteLevel(any());
    }
  }

  // ========== Edge Cases Tests ==========

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle level with zero stars")
    void shouldHandleLevelWithZeroStars() throws Exception {
      LevelDto zeroStarsLevel = LevelDto.builder()
          .id(5L)
          .levelName("Tutorial Level")
          .starsOnLevel(0)
          .bossOnLevel(false)
          .build();

      when(levelService.getLevelById(5L)).thenReturn(zeroStarsLevel);

      mockMvc.perform(get("/api/levels/5")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.starsOnLevel").value(0));

      verify(levelService, times(1)).getLevelById(5L);
    }

    @Test
    @DisplayName("Should handle multiple levels")
    void shouldHandleMultipleLevels() throws Exception {
      List<LevelDto> levels = Arrays.asList(
          LevelDto.builder()
              .id(1L)
              .levelName("Level 1")
              .starsOnLevel(3)
              .bossOnLevel(false)
              .build(),
          LevelDto.builder()
              .id(2L)
              .levelName("Level 2")
              .starsOnLevel(5)
              .bossOnLevel(false)
              .build(),
          LevelDto.builder()
              .id(3L)
              .levelName("Boss Level")
              .starsOnLevel(10)
              .bossOnLevel(true)
              .build()
      );

      when(levelService.getAllLevels()).thenReturn(levels);

      mockMvc.perform(get("/api/levels")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(3)))
          .andExpect(jsonPath("$[2].bossOnLevel").value(true));

      verify(levelService, times(1)).getAllLevels();
    }
  }
}
