package com.esdc.gameapi.controller.integration;

import com.esdc.gameapi.domain.entity.Achievement;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.domain.entity.UserAchievement;
import com.esdc.gameapi.domain.dto.AchievementDto;
import com.esdc.gameapi.repository.AchievementRepository;
import com.esdc.gameapi.repository.UserAchievementRepository;
import com.esdc.gameapi.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
@DisplayName("Achievement Controller Integration Tests")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=mySecretKeyForTestingPurposesOnlyMustBeAtLeast256BitsLong",
    "jwt.expiration=3600000",
    "admin.password=testAdminPassword123"
})
class AchievementControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AchievementRepository achievementRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserAchievementRepository userAchievementRepository;

  @Value("${admin.password}")
  private String adminPassword;

  private static final String ADMIN_PASSWORD_HEADER = "Admin-Password";
  private static final String WRONG_PASSWORD = "wrongPassword123";

  private Achievement testAchievement1;
  private Achievement testAchievement2;
  private User testUser;

  @BeforeEach
  void setUp() {
    // Очистка БД
    userAchievementRepository.deleteAll();
    achievementRepository.deleteAll();
    userRepository.deleteAll();

    // Создание тестовых данных
    testUser = new User();
    testUser.setNickname("testuser");
    testUser.setPasswordHash("password");
    testUser = userRepository.save(testUser);

    testAchievement1 = new Achievement();
    testAchievement1.setAchievementName("First Steps");
    testAchievement1.setAchievementDescription("Complete level 1");
    testAchievement1 = achievementRepository.save(testAchievement1);

    testAchievement2 = new Achievement();
    testAchievement2.setAchievementName("Speed Runner");
    testAchievement2.setAchievementDescription("Complete level in under 60 seconds");
    testAchievement2 = achievementRepository.save(testAchievement2);
  }

  @AfterEach
  void tearDown() {
    userAchievementRepository.deleteAll();
    achievementRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ========== GET All Achievements Tests ==========

  @Nested
  @DisplayName("GET /api/achievements - Get All Achievements")
  class GetAllAchievementsTests {

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should get all achievements")
    void shouldGetAllAchievements() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/achievements")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(2)))
          .andExpect(jsonPath("$[0].achievementName").value("First Steps"))
          .andExpect(jsonPath("$[0].achievementDescription").value("Complete level 1"))
          .andExpect(jsonPath("$[1].achievementName").value("Speed Runner"))
          .andExpect(jsonPath("$[1].achievementDescription").value("Complete level in under 60 seconds"));
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should return empty list when no achievements exist")
    void shouldReturnEmptyListWhenNoAchievementsExist() throws Exception {
      // Arrange
      achievementRepository.deleteAll();

      // Act & Assert
      mockMvc.perform(get("/api/achievements")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(0)));
    }
  }

  // ========== GET User Achievements Tests ==========

  @Nested
  @DisplayName("GET /api/achievements/user/{userId} - Get User Achievements")
  class GetUserAchievementsTests {

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should get user achievements")
    void shouldGetUserAchievements() throws Exception {
      // Arrange
      UserAchievement userAchievement = new UserAchievement();
      userAchievement.setUser(testUser);
      userAchievement.setAchievement(testAchievement1);
      userAchievementRepository.save(userAchievement);

      // Act & Assert
      mockMvc.perform(get("/api/achievements/user/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(1)))
          .andExpect(jsonPath("$[0].achievementName").value("First Steps"))
          .andExpect(jsonPath("$[0].achievementDescription").value("Complete level 1"));
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should return empty list when user has no achievements")
    void shouldReturnEmptyListWhenUserHasNoAchievements() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/achievements/user/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/achievements/user/999999")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should get multiple achievements for user")
    void shouldGetMultipleAchievementsForUser() throws Exception {
      // Arrange
      UserAchievement userAchievement1 = new UserAchievement();
      userAchievement1.setUser(testUser);
      userAchievement1.setAchievement(testAchievement1);
      userAchievementRepository.save(userAchievement1);

      UserAchievement userAchievement2 = new UserAchievement();
      userAchievement2.setUser(testUser);
      userAchievement2.setAchievement(testAchievement2);
      userAchievementRepository.save(userAchievement2);

      // Act & Assert
      mockMvc.perform(get("/api/achievements/user/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(2)))
          .andExpect(jsonPath("$[0].achievementName").value("First Steps"))
          .andExpect(jsonPath("$[1].achievementName").value("Speed Runner"));
    }
  }

  // ========== POST Create Achievement Tests ==========

  @Nested
  @DisplayName("POST /api/achievements/create - Create Achievement")
  class PostCreateAchievementTests {

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should create achievement with valid admin password")
    void shouldCreateAchievementWithValidAdminPassword() throws Exception {
      // Arrange
      AchievementDto newAchievement = AchievementDto.builder()
          .achievementName("Puzzle Master")
          .achievementDescription("Solve 50 puzzles")
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/achievements/create")
              .header(ADMIN_PASSWORD_HEADER, adminPassword)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(newAchievement)))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.achievementName").value("Puzzle Master"))
          .andExpect(jsonPath("$.achievementDescription").value("Solve 50 puzzles"));

      // Verify in database
      long count = achievementRepository.count();
      assert count == 3; // 2 from setUp + 1 new
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should reject creation with invalid admin password")
    void shouldRejectCreationWithInvalidAdminPassword() throws Exception {
      // Arrange
      AchievementDto newAchievement = AchievementDto.builder()
          .achievementName("Test Achievement")
          .achievementDescription("Test description")
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/achievements/create")
              .header(ADMIN_PASSWORD_HEADER, WRONG_PASSWORD)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(newAchievement)))
          .andExpect(status().isUnauthorized());

      // Verify database unchanged
      long count = achievementRepository.count();
      assert count == 2; // Only setUp data
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should reject creation without admin password header")
    void shouldRejectCreationWithoutAdminPasswordHeader() throws Exception {
      // Arrange
      AchievementDto newAchievement = AchievementDto.builder()
          .achievementName("Test Achievement")
          .achievementDescription("Test description")
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/achievements/create")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(newAchievement)))
          .andExpect(status().isInternalServerError());
    }
  }

  // ========== PUT Update Achievement Tests ==========

  @Nested
  @DisplayName("PUT /api/achievements/update/{id} - Update Achievement")
  class PutUpdateAchievementTests {

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should update achievement with valid admin password")
    void shouldUpdateAchievementWithValidAdminPassword() throws Exception {
      // Arrange
      AchievementDto updateDto = AchievementDto.builder()
          .achievementName("Updated Name")
          .achievementDescription("Updated description")
          .build();

      // Act & Assert
      mockMvc.perform(put("/api/achievements/update/" + testAchievement1.getId())
              .header(ADMIN_PASSWORD_HEADER, adminPassword)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").value(testAchievement1.getId()))
          .andExpect(jsonPath("$.achievementName").value("Updated Name"))
          .andExpect(jsonPath("$.achievementDescription").value("Updated description"));

      // Verify in database
      Achievement updated = achievementRepository.findById(testAchievement1.getId()).orElseThrow();
      assert updated.getAchievementName().equals("Updated Name");
      assert updated.getAchievementDescription().equals("Updated description");
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should reject update with invalid admin password")
    void shouldRejectUpdateWithInvalidAdminPassword() throws Exception {
      // Arrange
      AchievementDto updateDto = AchievementDto.builder()
          .achievementName("Updated Name")
          .achievementDescription("Updated description")
          .build();

      // Act & Assert
      mockMvc.perform(put("/api/achievements/update/" + testAchievement1.getId())
              .header(ADMIN_PASSWORD_HEADER, WRONG_PASSWORD)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isUnauthorized());

      // Verify database unchanged
      Achievement unchanged = achievementRepository.findById(testAchievement1.getId()).orElseThrow();
      assert unchanged.getAchievementName().equals("First Steps");
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 404 when updating non-existent achievement")
    void shouldReturn404WhenUpdatingNonExistentAchievement() throws Exception {
      // Arrange
      AchievementDto updateDto = AchievementDto.builder()
          .achievementName("Updated Name")
          .achievementDescription("Updated description")
          .build();

      // Act & Assert
      mockMvc.perform(put("/api/achievements/update/999999")
              .header(ADMIN_PASSWORD_HEADER, adminPassword)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isNotFound());
    }
  }

  // ========== DELETE Achievement Tests ==========

  @Nested
  @DisplayName("DELETE /api/achievements/delete/{id} - Delete Achievement")
  class DeleteAchievementTests {

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should delete achievement with valid admin password")
    void shouldDeleteAchievementWithValidAdminPassword() throws Exception {
      // Act & Assert
      mockMvc.perform(delete("/api/achievements/delete/" + testAchievement1.getId())
              .header(ADMIN_PASSWORD_HEADER, adminPassword))
          .andExpect(status().isNoContent());

      // Verify deletion in database
      boolean exists = achievementRepository.existsById(testAchievement1.getId());
      assert !exists;
      long count = achievementRepository.count();
      assert count == 1; // Only testAchievement2 remains
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should reject deletion with invalid admin password")
    void shouldRejectDeletionWithInvalidAdminPassword() throws Exception {
      // Act & Assert
      mockMvc.perform(delete("/api/achievements/delete/" + testAchievement1.getId())
              .header(ADMIN_PASSWORD_HEADER, WRONG_PASSWORD))
          .andExpect(status().isUnauthorized());

      // Verify database unchanged
      boolean exists = achievementRepository.existsById(testAchievement1.getId());
      assert exists;
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 404 when deleting non-existent achievement")
    void shouldReturn404WhenDeletingNonExistentAchievement() throws Exception {
      // Act & Assert
      mockMvc.perform(delete("/api/achievements/delete/999999")
              .header(ADMIN_PASSWORD_HEADER, adminPassword))
          .andExpect(status().isNotFound());
    }
  }

  // ========== Data Validation Tests ==========

  @Nested
  @DisplayName("Data Validation Tests")
  class DataValidationTests {

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should handle special characters in achievement name")
    void shouldHandleSpecialCharactersInAchievementName() throws Exception {
      // Arrange
      AchievementDto dto = AchievementDto.builder()
          .achievementName("Master of \"Quotes\" & Special!@#$%")
          .achievementDescription("Test special characters")
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/achievements/create")
              .header(ADMIN_PASSWORD_HEADER, adminPassword)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(dto)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.achievementName").value("Master of \"Quotes\" & Special!@#$%"));
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should handle long achievement description")
    void shouldHandleLongAchievementDescription() throws Exception {
      // Arrange
      String longDescription = "A".repeat(255);
      AchievementDto dto = AchievementDto.builder()
          .achievementName("Long Description Test")
          .achievementDescription(longDescription)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/achievements/create")
              .header(ADMIN_PASSWORD_HEADER, adminPassword)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(dto)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.achievementDescription").value(longDescription));
    }
  }

  // ========== Concurrent Operations Tests ==========

  @Nested
  @DisplayName("Concurrent Operations Tests")
  class ConcurrentOperationsTests {

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should handle multiple achievements for same user")
    void shouldHandleMultipleAchievementsForSameUser() throws Exception {
      // Arrange - Create multiple achievements for one user
      UserAchievement ua1 = new UserAchievement();
      ua1.setUser(testUser);
      ua1.setAchievement(testAchievement1);
      userAchievementRepository.save(ua1);

      UserAchievement ua2 = new UserAchievement();
      ua2.setUser(testUser);
      ua2.setAchievement(testAchievement2);
      userAchievementRepository.save(ua2);

      // Act & Assert
      mockMvc.perform(get("/api/achievements/user/" + testUser.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @Tag("integration")
    @WithMockUser(roles = "USER")
    @DisplayName("Should preserve achievement order when retrieving all")
    void shouldPreserveAchievementOrderWhenRetrievingAll() throws Exception {
      // Arrange - Create third achievement
      Achievement achievement3 = new Achievement();
      achievement3.setAchievementName("Third Achievement");
      achievement3.setAchievementDescription("Third description");
      achievementRepository.save(achievement3);

      // Act & Assert
      mockMvc.perform(get("/api/achievements")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(3)))
          .andExpect(jsonPath("$[0].achievementName").exists())
          .andExpect(jsonPath("$[1].achievementName").exists())
          .andExpect(jsonPath("$[2].achievementName").exists());
    }
  }
}
