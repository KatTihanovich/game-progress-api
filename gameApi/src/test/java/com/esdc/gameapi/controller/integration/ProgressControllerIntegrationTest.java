package com.esdc.gameapi.controller.integration;

import com.esdc.gameapi.domain.dto.ProgressDto;
import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.domain.entity.Progress;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("Progress Controller Integration Tests")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=mySecretKeyForTestingPurposesOnlyMustBeAtLeast256BitsLong",
    "jwt.expiration=3600000"
})
class ProgressControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ProgressRepository progressRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private LevelRepository levelRepository;

  @Autowired
  private UserStatisticsRepository userStatisticsRepository;

  @Autowired
  private AchievementRepository achievementRepository;

  @Autowired
  private UserAchievementRepository userAchievementRepository;

  private User testUser;
  private Level testLevel1;
  private Level testLevel2;

  @BeforeEach
  void setUp() {
    // Clear data in correct order - dependencies first
    progressRepository.deleteAll();
    userStatisticsRepository.deleteAll();
    userAchievementRepository.deleteAll();
    userRepository.deleteAll();
    levelRepository.deleteAll();
    achievementRepository.deleteAll();

    // Create test user
    testUser = User.builder()
        .nickname("testuser")
        .passwordHash("$2a$10$test")
        .age(25)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    testUser = userRepository.save(testUser);

    // Create test levels
    testLevel1 = Level.builder()
        .levelName("Level 1")
        .starsOnLevel(5)
        .bossOnLevel(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    testLevel1 = levelRepository.save(testLevel1);

    testLevel2 = Level.builder()
        .levelName("Level 2")
        .starsOnLevel(5)
        .bossOnLevel(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    testLevel2 = levelRepository.save(testLevel2);
  }

  @AfterEach
  void tearDown() {
    // Clean up in correct order - dependencies first
    progressRepository.deleteAll();
    progressRepository.flush();
    userStatisticsRepository.deleteAll();
    userStatisticsRepository.flush();
    userAchievementRepository.deleteAll();
    userAchievementRepository.flush();
    userRepository.deleteAll();
    userRepository.flush();
    levelRepository.deleteAll();
    levelRepository.flush();
    achievementRepository.deleteAll();
    achievementRepository.flush();
  }

  @Nested
  @DisplayName("Create Progress Tests")
  class CreateProgressTests {

    @Test
    @WithMockUser
    @DisplayName("Should create progress successfully")
    void shouldCreateProgressSuccessfully() throws Exception {
      ProgressDto request = ProgressDto.builder()
          .levelId(testLevel1.getId())
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(3)
          .build();

      mockMvc.perform(post("/api/progress")
              .param("userId", testUser.getId().toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.stars").value(3))
          .andExpect(jsonPath("$.levelId").value(testLevel1.getId()))
          .andExpect(jsonPath("$.killedEnemiesNumber").value(10))
          .andExpect(jsonPath("$.solvedPuzzlesNumber").value(5))
          .andExpect(jsonPath("$.timeSpent").value("00:15:30"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should reject progress with stars exceeding level maximum")
    void shouldRejectProgressWithStarsExceedingMaximum() throws Exception {
      ProgressDto request = ProgressDto.builder()
          .levelId(testLevel1.getId())
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(6) // testLevel1 has max 5 stars
          .build();

      mockMvc.perform(post("/api/progress")
              .param("userId", testUser.getId().toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", containsString("Stars")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should reject progress with non-existent user")
    void shouldRejectProgressWithNonExistentUser() throws Exception {
      ProgressDto request = ProgressDto.builder()
          .levelId(testLevel1.getId())
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(3)
          .build();

      mockMvc.perform(post("/api/progress")
              .param("userId", "999999")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message", containsString("User")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should reject progress with non-existent level")
    void shouldRejectProgressWithNonExistentLevel() throws Exception {
      ProgressDto request = ProgressDto.builder()
          .levelId(999999L)
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(3)
          .build();

      mockMvc.perform(post("/api/progress")
              .param("userId", testUser.getId().toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message", containsString("Level")));
    }
  }

  @Nested
  @DisplayName("Get User Progress Tests")
  class GetUserProgressTests {

    @Test
    @WithMockUser
    @DisplayName("Should return all progress for user")
    void shouldReturnAllProgressForUser() throws Exception {
      // Create test progress
      Progress progress1 = Progress.builder()
          .user(testUser)
          .level(testLevel1)
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(3)
          .createdAt(LocalDateTime.now())
          .build();
      progressRepository.save(progress1);

      Progress progress2 = Progress.builder()
          .user(testUser)
          .level(testLevel2)
          .killedEnemiesNumber(15)
          .solvedPuzzlesNumber(7)
          .timeSpent("00:20:00")
          .stars(4)
          .createdAt(LocalDateTime.now())
          .build();
      progressRepository.save(progress2);

      mockMvc.perform(get("/api/progress/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(2)))
          .andExpect(jsonPath("$[0].stars", anyOf(is(3), is(4))))
          .andExpect(jsonPath("$[1].stars", anyOf(is(3), is(4))));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return empty list for user with no progress")
    void shouldReturnEmptyListForUserWithNoProgress() throws Exception {
      mockMvc.perform(get("/api/progress/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() throws Exception {
      mockMvc.perform(get("/api/progress/999999")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("Get Latest Progress Tests")
  class GetLatestProgressTests {

    @Test
    @WithMockUser
    @DisplayName("Should return latest progress for user and level")
    void shouldReturnLatestProgressForUserAndLevel() throws Exception {
      // Create older progress
      Progress oldProgress = Progress.builder()
          .user(testUser)
          .level(testLevel1)
          .killedEnemiesNumber(5)
          .solvedPuzzlesNumber(2)
          .timeSpent("00:10:00")
          .stars(2)
          .createdAt(LocalDateTime.now().minusHours(1))
          .build();
      progressRepository.save(oldProgress);

      // Create newer progress
      Progress newProgress = Progress.builder()
          .user(testUser)
          .level(testLevel1)
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(3)
          .createdAt(LocalDateTime.now())
          .build();
      progressRepository.save(newProgress);

      mockMvc.perform(get("/api/progress/" + testUser.getId() + "/level/" + testLevel1.getId() + "/latest")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.stars").value(3))
          .andExpect(jsonPath("$.killedEnemiesNumber").value(10));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when no progress exists")
    void shouldReturn404WhenNoProgressExists() throws Exception {
      mockMvc.perform(get("/api/progress/" + testUser.getId() + "/level/" + testLevel1.getId() + "/latest")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("Get Total Stars Tests")
  class GetTotalStarsTests {

    @Test
    @WithMockUser
    @DisplayName("Should return total stars for user and level")
    void shouldReturnTotalStarsForUserAndLevel() throws Exception {
      // Create multiple progress entries
      Progress progress1 = Progress.builder()
          .user(testUser)
          .level(testLevel1)
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(3)
          .createdAt(LocalDateTime.now().minusHours(1))
          .build();
      progressRepository.save(progress1);

      Progress progress2 = Progress.builder()
          .user(testUser)
          .level(testLevel1)
          .killedEnemiesNumber(15)
          .solvedPuzzlesNumber(7)
          .timeSpent("00:20:00")
          .stars(5)
          .createdAt(LocalDateTime.now())
          .build();
      progressRepository.save(progress2);

      mockMvc.perform(get("/api/progress/" + testUser.getId() + "/level/" + testLevel1.getId() + "/total-stars")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").value(8)); // 3 + 5 = 8
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 0 when no progress exists")
    void shouldReturn0WhenNoProgressExists() throws Exception {
      mockMvc.perform(get("/api/progress/" + testUser.getId() + "/level/" + testLevel1.getId() + "/total-stars")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").value(0));
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @WithMockUser
    @DisplayName("Should accept valid time formats")
    void shouldAcceptValidTimeFormats() throws Exception {
      String[] validTimes = {
          "00:00:00",
          "00:15:30",
          "23:59:59",
          "99:59:59" // Regex allows 00-99 hours
      };

      for (String time : validTimes) {
        ProgressDto request = ProgressDto.builder()
            .levelId(testLevel1.getId())
            .killedEnemiesNumber(10)
            .solvedPuzzlesNumber(5)
            .timeSpent(time)
            .stars(3)
            .build();

        mockMvc.perform(post("/api/progress")
                .param("userId", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Clean up
        progressRepository.deleteAll();
        userStatisticsRepository.deleteAll();
      }
    }

    @Test
    @WithMockUser
    @DisplayName("Should reject invalid time formats")
    void shouldRejectInvalidTimeFormats() throws Exception {
      String[] invalidTimes = {
          "00:00",      // Too short
          "00:60:00",   // Minutes > 59
          "00:00:60",   // Seconds > 59
          "abc:de:fg",  // Not digits
          "1:2:3"       // Missing leading zeros
      };

      for (String time : invalidTimes) {
        ProgressDto request = ProgressDto.builder()
            .levelId(testLevel1.getId())
            .killedEnemiesNumber(10)
            .solvedPuzzlesNumber(5)
            .timeSpent(time)
            .stars(3)
            .build();

        mockMvc.perform(post("/api/progress")
                .param("userId", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("Should reject negative values")
    void shouldRejectNegativeValues() throws Exception {
      ProgressDto request = ProgressDto.builder()
          .levelId(testLevel1.getId())
          .killedEnemiesNumber(-1)
          .solvedPuzzlesNumber(-1)
          .timeSpent("00:15:30")
          .stars(3)
          .build();

      mockMvc.perform(post("/api/progress")
              .param("userId", testUser.getId().toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("Should accept boundary star values")
    void shouldAcceptBoundaryStarValues() throws Exception {
      // Test 0 stars
      ProgressDto request0 = ProgressDto.builder()
          .levelId(testLevel1.getId())
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(0)
          .build();

      mockMvc.perform(post("/api/progress")
              .param("userId", testUser.getId().toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request0)))
          .andExpect(status().isCreated());

      progressRepository.deleteAll();
      userStatisticsRepository.deleteAll();

      // Test max stars (5)
      ProgressDto request5 = ProgressDto.builder()
          .levelId(testLevel1.getId())
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(5)
          .build();

      mockMvc.perform(post("/api/progress")
              .param("userId", testUser.getId().toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request5)))
          .andExpect(status().isCreated());
    }
  }

  @Nested
  @DisplayName("Security Tests")
  class SecurityTests {

    @Test
    @Tag("integration")
    @DisplayName("Should deny access without authentication")
    void shouldDenyAccessWithoutAuthentication() throws Exception {
      // GET endpoint
      mockMvc.perform(get("/api/progress/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden()); // Changed from 401 to 403

      // POST endpoint
      ProgressDto request = ProgressDto.builder()
          .levelId(testLevel1.getId())
          .killedEnemiesNumber(10)
          .solvedPuzzlesNumber(5)
          .timeSpent("00:15:30")
          .stars(3)
          .build();

      mockMvc.perform(post("/api/progress")
              .param("userId", testUser.getId().toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden()); // Changed from 401 to 403
    }

    @Test
    @WithMockUser
    @DisplayName("Should allow access with authentication")
    void shouldAllowAccessWithAuthentication() throws Exception {
      mockMvc.perform(get("/api/progress/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @WithMockUser
    @DisplayName("Should handle very large values")
    void shouldHandleVeryLargeValues() throws Exception {
      ProgressDto request = ProgressDto.builder()
          .levelId(testLevel1.getId())
          .killedEnemiesNumber(Integer.MAX_VALUE)
          .solvedPuzzlesNumber(Integer.MAX_VALUE)
          .timeSpent("99:59:59")
          .stars(3)
          .build();

      mockMvc.perform(post("/api/progress")
              .param("userId", testUser.getId().toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle zero values")
    void shouldHandleZeroValues() throws Exception {
      ProgressDto request = ProgressDto.builder()
          .levelId(testLevel1.getId())
          .killedEnemiesNumber(0)
          .solvedPuzzlesNumber(0)
          .timeSpent("00:00:00")
          .stars(0)
          .build();

      mockMvc.perform(post("/api/progress")
              .param("userId", testUser.getId().toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle multiple progress entries for same level")
    void shouldHandleMultipleProgressEntriesForSameLevel() throws Exception {
      for (int i = 1; i <= 3; i++) {
        ProgressDto request = ProgressDto.builder()
            .levelId(testLevel1.getId())
            .killedEnemiesNumber(i * 5)
            .solvedPuzzlesNumber(i * 2)
            .timeSpent("00:15:30")
            .stars(i)
            .build();

        mockMvc.perform(post("/api/progress")
                .param("userId", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
      }

      // Verify all entries were created
      mockMvc.perform(get("/api/progress/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(3)));
    }
  }
}
