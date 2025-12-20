package com.esdc.gameapi.controller.unit;

import com.esdc.gameapi.controller.AchievementController;
import com.esdc.gameapi.domain.dto.AchievementDto;
import com.esdc.gameapi.domain.dto.UserAchievementDto;
import com.esdc.gameapi.exception.GlobalExceptionHandler;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.service.AchievementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
@DisplayName("Achievement Controller Unit Tests")
class AchievementControllerTest {

  private MockMvc mockMvc;

  @Mock
  private AchievementService achievementService;

  @InjectMocks
  private AchievementController achievementController;

  private ObjectMapper objectMapper;

  private static final String ADMIN_PASSWORD_HEADER = "Admin-Password";
  private static final String CORRECT_ADMIN_PASSWORD = "testAdminPassword123";
  private static final String INCORRECT_ADMIN_PASSWORD = "wrongPassword";

  private AchievementDto testAchievementDto;
  private UserAchievementDto testUserAchievementDto;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    // Установка admin password через рефлексию
    ReflectionTestUtils.setField(achievementController, "adminPassword", CORRECT_ADMIN_PASSWORD);

    // Настройка MockMvc с обработчиком исключений
    mockMvc = MockMvcBuilders.standaloneSetup(achievementController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    testAchievementDto = AchievementDto.builder()
        .id(1L)
        .achievementName("First Steps")
        .achievementDescription("Complete level 1")
        .build();

    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    testUserAchievementDto = UserAchievementDto.builder()
        .achievementId(1L)
        .achievementName("First Steps")
        .achievementDescription("Complete level 1")
        .build();
  }

  // ========== GET All Achievements Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should return all achievements")
  void shouldReturnAllAchievements() throws Exception {
    // Arrange
    AchievementDto achievement2 = AchievementDto.builder()
        .id(2L)
        .achievementName("Speed Runner")
        .achievementDescription("Complete level in under 1 minute")
        .build();

    List<AchievementDto> achievements = Arrays.asList(testAchievementDto, achievement2);
    when(achievementService.getAllAchievements()).thenReturn(achievements);

    // Act & Assert
    mockMvc.perform(get("/api/achievements")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].achievementName").value("First Steps"))
        .andExpect(jsonPath("$[0].achievementDescription").value("Complete level 1"))
        .andExpect(jsonPath("$[1].id").value(2))
        .andExpect(jsonPath("$[1].achievementName").value("Speed Runner"));

    verify(achievementService, times(1)).getAllAchievements();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return empty list when no achievements exist")
  void shouldReturnEmptyListWhenNoAchievementsExist() throws Exception {
    // Arrange
    when(achievementService.getAllAchievements()).thenReturn(Collections.emptyList());

    // Act & Assert
    mockMvc.perform(get("/api/achievements")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(0)));

    verify(achievementService, times(1)).getAllAchievements();
  }

  // ========== GET User Achievements Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should return user achievements")
  void shouldReturnUserAchievements() throws Exception {
    // Arrange
    List<UserAchievementDto> userAchievements = Arrays.asList(testUserAchievementDto);
    when(achievementService.getAchievementsByUserId(1L)).thenReturn(userAchievements);

    // Act & Assert
    mockMvc.perform(get("/api/achievements/user/1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].achievementId").value(1))
        .andExpect(jsonPath("$[0].achievementName").value("First Steps"))
        .andExpect(jsonPath("$[0].achievementDescription").value("Complete level 1"));

    verify(achievementService, times(1)).getAchievementsByUserId(1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return empty list when user has no achievements")
  void shouldReturnEmptyListWhenUserHasNoAchievements() throws Exception {
    // Arrange
    when(achievementService.getAchievementsByUserId(1L)).thenReturn(Collections.emptyList());

    // Act & Assert
    mockMvc.perform(get("/api/achievements/user/1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

    verify(achievementService, times(1)).getAchievementsByUserId(1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle user not found when getting achievements")
  void shouldHandleUserNotFoundWhenGettingAchievements() throws Exception {
    // Arrange
    when(achievementService.getAchievementsByUserId(999L))
        .thenThrow(new ResourceNotFoundException("User", "id", 999L));

    // Act & Assert
    mockMvc.perform(get("/api/achievements/user/999")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());

    verify(achievementService, times(1)).getAchievementsByUserId(999L);
  }

  // ========== POST Create Achievement Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should create achievement with valid admin password")
  void shouldCreateAchievementWithValidAdminPassword() throws Exception {
    // Arrange
    AchievementDto createDto = AchievementDto.builder()
        .achievementName("First Steps")
        .achievementDescription("Complete level 1")
        .build();

    when(achievementService.createAchievement(any(AchievementDto.class)))
        .thenReturn(testAchievementDto);

    // Act & Assert
    mockMvc.perform(post("/api/achievements/create")
            .header(ADMIN_PASSWORD_HEADER, CORRECT_ADMIN_PASSWORD)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.achievementName").value("First Steps"))
        .andExpect(jsonPath("$.achievementDescription").value("Complete level 1"));

    verify(achievementService, times(1)).createAchievement(any(AchievementDto.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should reject creation with invalid admin password")
  void shouldRejectCreationWithInvalidAdminPassword() throws Exception {
    // Arrange
    AchievementDto createDto = AchievementDto.builder()
        .achievementName("First Steps")
        .achievementDescription("Complete level 1")
        .build();

    // Act & Assert
    mockMvc.perform(post("/api/achievements/create")
            .header(ADMIN_PASSWORD_HEADER, INCORRECT_ADMIN_PASSWORD)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isUnauthorized());

    verify(achievementService, never()).createAchievement(any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should reject creation without admin password header")
  void shouldRejectCreationWithoutAdminPasswordHeader() throws Exception {
    // Arrange
    AchievementDto createDto = AchievementDto.builder()
        .achievementName("First Steps")
        .achievementDescription("Complete level 1")
        .build();

    // Act & Assert
    mockMvc.perform(post("/api/achievements/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isInternalServerError());

    verify(achievementService, never()).createAchievement(any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should reject creation with empty admin password")
  void shouldRejectCreationWithEmptyAdminPassword() throws Exception {
    // Arrange
    AchievementDto createDto = AchievementDto.builder()
        .achievementName("First Steps")
        .achievementDescription("Complete level 1")
        .build();

    // Act & Assert
    mockMvc.perform(post("/api/achievements/create")
            .header(ADMIN_PASSWORD_HEADER, "")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isUnauthorized());

    verify(achievementService, never()).createAchievement(any());
  }

  // ========== PUT Update Achievement Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should update achievement with valid admin password")
  void shouldUpdateAchievementWithValidAdminPassword() throws Exception {
    // Arrange
    AchievementDto updateDto = AchievementDto.builder()
        .achievementName("Updated Name")
        .achievementDescription("Updated description")
        .build();

    AchievementDto updatedAchievement = AchievementDto.builder()
        .id(1L)
        .achievementName("Updated Name")
        .achievementDescription("Updated description")
        .build();

    when(achievementService.updateAchievement(eq(1L), any(AchievementDto.class)))
        .thenReturn(updatedAchievement);

    // Act & Assert
    mockMvc.perform(put("/api/achievements/update/1")
            .header(ADMIN_PASSWORD_HEADER, CORRECT_ADMIN_PASSWORD)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.achievementName").value("Updated Name"))
        .andExpect(jsonPath("$.achievementDescription").value("Updated description"));

    verify(achievementService, times(1)).updateAchievement(eq(1L), any(AchievementDto.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should reject update with invalid admin password")
  void shouldRejectUpdateWithInvalidAdminPassword() throws Exception {
    // Arrange
    AchievementDto updateDto = AchievementDto.builder()
        .achievementName("Updated Name")
        .achievementDescription("Updated description")
        .build();

    // Act & Assert
    mockMvc.perform(put("/api/achievements/update/1")
            .header(ADMIN_PASSWORD_HEADER, INCORRECT_ADMIN_PASSWORD)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isUnauthorized());

    verify(achievementService, never()).updateAchievement(any(), any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle achievement not found when updating")
  void shouldHandleAchievementNotFoundWhenUpdating() throws Exception {
    // Arrange
    AchievementDto updateDto = AchievementDto.builder()
        .achievementName("Updated Name")
        .achievementDescription("Updated description")
        .build();

    when(achievementService.updateAchievement(eq(999L), any(AchievementDto.class)))
        .thenThrow(new ResourceNotFoundException("Achievement", "id", 999L));

    // Act & Assert
    mockMvc.perform(put("/api/achievements/update/999")
            .header(ADMIN_PASSWORD_HEADER, CORRECT_ADMIN_PASSWORD)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
        .andExpect(status().isNotFound());

    verify(achievementService, times(1)).updateAchievement(eq(999L), any(AchievementDto.class));
  }

  // ========== DELETE Achievement Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should delete achievement with valid admin password")
  void shouldDeleteAchievementWithValidAdminPassword() throws Exception {
    // Arrange
    doNothing().when(achievementService).deleteAchievement(1L);

    // Act & Assert
    mockMvc.perform(delete("/api/achievements/delete/1")
            .header(ADMIN_PASSWORD_HEADER, CORRECT_ADMIN_PASSWORD))
        .andExpect(status().isNoContent());

    verify(achievementService, times(1)).deleteAchievement(1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should reject deletion with invalid admin password")
  void shouldRejectDeletionWithInvalidAdminPassword() throws Exception {
    // Act & Assert
    mockMvc.perform(delete("/api/achievements/delete/1")
            .header(ADMIN_PASSWORD_HEADER, INCORRECT_ADMIN_PASSWORD))
        .andExpect(status().isUnauthorized());

    verify(achievementService, never()).deleteAchievement(any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should reject deletion without admin password header")
  void shouldRejectDeletionWithoutAdminPasswordHeader() throws Exception {
    // Act & Assert
    mockMvc.perform(delete("/api/achievements/delete/1"))
        .andExpect(status().isInternalServerError());

    verify(achievementService, never()).deleteAchievement(any());
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle achievement not found when deleting")
  void shouldHandleAchievementNotFoundWhenDeleting() throws Exception {
    // Arrange
    doThrow(new ResourceNotFoundException("Achievement", "id", 999L))
        .when(achievementService).deleteAchievement(999L);

    // Act & Assert
    mockMvc.perform(delete("/api/achievements/delete/999")
            .header(ADMIN_PASSWORD_HEADER, CORRECT_ADMIN_PASSWORD))
        .andExpect(status().isNotFound());

    verify(achievementService, times(1)).deleteAchievement(999L);
  }

  // ========== Admin Password Validation Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should reject case-sensitive password mismatch")
  void shouldRejectCaseSensitivePasswordMismatch() throws Exception {
    // Arrange
    AchievementDto createDto = AchievementDto.builder()
        .achievementName("Test")
        .achievementDescription("Test achievement")
        .build();

    // Act & Assert
    mockMvc.perform(post("/api/achievements/create")
            .header(ADMIN_PASSWORD_HEADER, "TESTADMINPASSWORD123")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDto)))
        .andExpect(status().isUnauthorized());
  }

  // ========== Multiple Achievements Tests ==========

  @Test
  @Tag("unit")
  @DisplayName("Should handle multiple achievements with different descriptions")
  void shouldHandleMultipleAchievementsWithDifferentDescriptions() throws Exception {
    // Arrange
    List<AchievementDto> achievements = Arrays.asList(
        AchievementDto.builder()
            .id(1L)
            .achievementName("First Steps")
            .achievementDescription("Complete level 1")
            .build(),
        AchievementDto.builder()
            .id(2L)
            .achievementName("Enemy Slayer")
            .achievementDescription("Kill 100 enemies")
            .build(),
        AchievementDto.builder()
            .id(3L)
            .achievementName("Puzzle Master")
            .achievementDescription("Solve 50 puzzles")
            .build()
    );

    when(achievementService.getAllAchievements()).thenReturn(achievements);

    // Act & Assert
    mockMvc.perform(get("/api/achievements")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].achievementDescription").value("Complete level 1"))
        .andExpect(jsonPath("$[1].achievementDescription").value("Kill 100 enemies"))
        .andExpect(jsonPath("$[2].achievementDescription").value("Solve 50 puzzles"));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle user with multiple unlocked achievements")
  void shouldHandleUserWithMultipleUnlockedAchievements() throws Exception {
    // Arrange
    List<UserAchievementDto> userAchievements = Arrays.asList(
        UserAchievementDto.builder()
            .achievementId(1L)
            .achievementName("First Steps")
            .achievementDescription("Complete level 1")
            .build(),
        UserAchievementDto.builder()
            .achievementId(2L)
            .achievementName("Speed Runner")
            .achievementDescription("Complete level in under 1 minute")
            .build()
    );

    when(achievementService.getAchievementsByUserId(1L)).thenReturn(userAchievements);

    // Act & Assert
    mockMvc.perform(get("/api/achievements/user/1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].achievementId").value(1))
        .andExpect(jsonPath("$[0].achievementName").value("First Steps"))
        .andExpect(jsonPath("$[0].achievementDescription").value("Complete level 1"))
        .andExpect(jsonPath("$[1].achievementId").value(2))
        .andExpect(jsonPath("$[1].achievementName").value("Speed Runner"))
        .andExpect(jsonPath("$[1].achievementDescription").value("Complete level in under 1 minute"));
  }
}
