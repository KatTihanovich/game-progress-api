package com.esdc.gameapi.controller.integration;

import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.domain.entity.Progress;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.domain.entity.UserStatistics;
import com.esdc.gameapi.repository.LevelRepository;
import com.esdc.gameapi.repository.ProgressRepository;
import com.esdc.gameapi.repository.UserRepository;
import com.esdc.gameapi.repository.UserStatisticsRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("User Statistics Controller Integration Tests")
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=mySecretKeyForTestingPurposesOnlyMustBeAtLeast256BitsLong",
    "jwt.expiration=3600000",
    "admin.password=testAdminPassword123"
})
class UserStatisticsControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserStatisticsRepository statisticsRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private LevelRepository levelRepository;

  @Autowired
  private ProgressRepository progressRepository;

  @Autowired
  private ObjectMapper objectMapper;

  private User testUser;
  private Level testLevel1;
  private Level testLevel2;
  private Level testLevel3;

  @BeforeEach
  void setUp() {
    statisticsRepository.deleteAll();
    progressRepository.deleteAll();
    levelRepository.deleteAll();
    userRepository.deleteAll();

    // Create test user
    testUser = User.builder()
        .nickname("testuser")
        .passwordHash("hashedPassword")
        .age(25)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    testUser = userRepository.save(testUser);

    // Create test levels
    testLevel1 = Level.builder()
        .levelName("Level 1")
        .starsOnLevel(3)
        .bossOnLevel(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    testLevel1 = levelRepository.save(testLevel1);

    testLevel2 = Level.builder()
        .levelName("Level 2")
        .starsOnLevel(5)
        .bossOnLevel(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    testLevel2 = levelRepository.save(testLevel2);

    testLevel3 = Level.builder()
        .levelName("Boss Level")
        .starsOnLevel(10)
        .bossOnLevel(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    testLevel3 = levelRepository.save(testLevel3);
  }

  @AfterEach
  void tearDown() {
    statisticsRepository.deleteAll();
    progressRepository.deleteAll();
    levelRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ========== GET Statistics Tests ==========

  @Nested
  @DisplayName("GET /api/statistics/{userId} - Get User Statistics")
  class GetStatisticsTests {

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return user statistics when exists")
    void shouldReturnUserStatisticsWhenExists() throws Exception {
      // Arrange
      UserStatistics statistics = new UserStatistics(testUser);
      statistics.setTotalLevelsCompleted(5);
      statistics.setTotalTimePlayed("01:30:45");
      statistics.setTotalKilledEnemies(100);
      statistics.setTotalSolvedPuzzles(50);
      statistics.setTotalStars(120);
      statisticsRepository.save(statistics);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.totalLevelsCompleted").value(5))
          .andExpect(jsonPath("$.totalTimePlayed").value("01:30:45"))
          .andExpect(jsonPath("$.totalKilledEnemies").value(100))
          .andExpect(jsonPath("$.totalSolvedPuzzles").value(50))
          .andExpect(jsonPath("$.totalStars").value(120));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return 404 when statistics not found")
    void shouldReturn404WhenStatisticsNotFound() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/statistics/999999")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return zero statistics for new user")
    void shouldReturnZeroStatisticsForNewUser() throws Exception {
      // Arrange
      UserStatistics statistics = new UserStatistics(testUser);
      statisticsRepository.save(statistics);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalLevelsCompleted").value(0))
          .andExpect(jsonPath("$.totalTimePlayed").value("00:00:00"))
          .andExpect(jsonPath("$.totalKilledEnemies").value(0))
          .andExpect(jsonPath("$.totalSolvedPuzzles").value(0))
          .andExpect(jsonPath("$.totalStars").value(0));
    }
  }

  // ========== GET Stars Progress Tests ==========

  @Nested
  @DisplayName("GET /api/statistics/{userId}/stars-progress - Get Stars Progress")
  class GetStarsProgressTests {

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return stars progress with percentage")
    void shouldReturnStarsProgressWithPercentage() throws Exception {
      // Arrange
      UserStatistics statistics = new UserStatistics(testUser);
      statistics.setTotalStars(9); // 9 out of 18
      statisticsRepository.save(statistics);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/" + testUser.getId() + "/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.currentStars").value(9))
          .andExpect(jsonPath("$.maxPossibleStars").value(18))
          .andExpect(jsonPath("$.progressPercentage").value(50.0));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return 404 when statistics not found")
    void shouldReturn404WhenStatisticsNotFound() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/statistics/999999/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return zero progress for new user")
    void shouldReturnZeroProgressForNewUser() throws Exception {
      // Arrange
      UserStatistics statistics = new UserStatistics(testUser);
      statisticsRepository.save(statistics);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/" + testUser.getId() + "/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.currentStars").value(0))
          .andExpect(jsonPath("$.maxPossibleStars").value(18))
          .andExpect(jsonPath("$.progressPercentage").value(0.0));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return 100% when all stars collected")
    void shouldReturn100PercentWhenAllStarsCollected() throws Exception {
      // Arrange
      UserStatistics statistics = new UserStatistics(testUser);
      statistics.setTotalStars(18);
      statisticsRepository.save(statistics);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/" + testUser.getId() + "/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.currentStars").value(18))
          .andExpect(jsonPath("$.maxPossibleStars").value(18))
          .andExpect(jsonPath("$.progressPercentage").value(100.0));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should round percentage to two decimals")
    void shouldRoundPercentageToTwoDecimals() throws Exception {
      // Arrange
      UserStatistics statistics = new UserStatistics(testUser);
      statistics.setTotalStars(5);
      statisticsRepository.save(statistics);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/" + testUser.getId() + "/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.progressPercentage").value(27.78));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return zero percentage when no levels exist")
    void shouldReturnZeroPercentageWhenNoLevelsExist() throws Exception {
      // Arrange
      levelRepository.deleteAll();
      UserStatistics statistics = new UserStatistics(testUser);
      statisticsRepository.save(statistics);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/" + testUser.getId() + "/stars-progress")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.progressPercentage").value(0.0));
    }
  }

  // ========== Edge Cases Tests ==========

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {
    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should handle multiple users independently")
    void shouldHandleMultipleUsersIndependently() throws Exception {
      // Arrange - Second user
      User user2 = User.builder()
          .nickname("testuser2")
          .passwordHash("hashedPassword2")
          .age(30)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();
      user2 = userRepository.save(user2);

      UserStatistics stats1 = new UserStatistics(testUser);
      stats1.setTotalStars(10);
      statisticsRepository.save(stats1);

      UserStatistics stats2 = new UserStatistics(user2);
      stats2.setTotalStars(5);
      statisticsRepository.save(stats2);

      // Act & Assert - User 1
      mockMvc.perform(get("/api/statistics/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalStars").value(10));

      // Act & Assert - User 2
      mockMvc.perform(get("/api/statistics/" + user2.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalStars").value(5));
    }
  }

  // ========== Security Tests ==========

  @Nested
  @DisplayName("Security Tests")
  class SecurityTests {

    @Test
    @Tag("integration")
    @DisplayName("Should deny access without authentication")
    void shouldDenyAccessWithoutAuthentication() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/statistics/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should allow authenticated users to read statistics")
    void shouldAllowAuthenticatedUsersToReadStatistics() throws Exception {
      // Arrange
      UserStatistics statistics = new UserStatistics(testUser);
      statisticsRepository.save(statistics);

      // Act & Assert
      mockMvc.perform(get("/api/statistics/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }
  }
}
