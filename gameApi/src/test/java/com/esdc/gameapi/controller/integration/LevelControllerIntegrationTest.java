package com.esdc.gameapi.controller.integration;

import com.esdc.gameapi.domain.dto.LevelDto;
import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.repository.LevelRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@DisplayName("Level Controller Integration Tests")
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
class LevelControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private LevelRepository levelRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${admin.password}")
  private String adminPassword;

  private static final String ADMIN_PASSWORD_HEADER = "Admin-Password";

  private Level testLevel1;
  private Level testLevel2;

  @BeforeEach
  void setUp() {
    levelRepository.deleteAll();

    testLevel1 = Level.builder()
        .levelName("Level 1")
        .starsOnLevel(3)
        .bossOnLevel(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    testLevel2 = Level.builder()
        .levelName("Level 2 - Boss")
        .starsOnLevel(5)
        .bossOnLevel(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @AfterEach
  void tearDown() {
    levelRepository.deleteAll();
  }

  // ========== GET All Levels Tests ==========

  @Nested
  @DisplayName("GET /api/levels - Get All Levels")
  class GetAllLevelsTests {

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return all levels")
    void shouldReturnAllLevels() throws Exception {
      // Arrange
      levelRepository.save(testLevel1);
      levelRepository.save(testLevel2);

      // Act & Assert
      mockMvc.perform(get("/api/levels")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(2)))
          .andExpect(jsonPath("$[0].levelName").value("Level 1"))
          .andExpect(jsonPath("$[0].starsOnLevel").value(3))
          .andExpect(jsonPath("$[0].bossOnLevel").value(false))
          .andExpect(jsonPath("$[1].levelName").value("Level 2 - Boss"))
          .andExpect(jsonPath("$[1].starsOnLevel").value(5))
          .andExpect(jsonPath("$[1].bossOnLevel").value(true));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return empty list when no levels exist")
    void shouldReturnEmptyListWhenNoLevelsExist() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/levels")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return levels ordered by creation")
    void shouldReturnLevelsOrderedByCreation() throws Exception {
      // Arrange
      Level level3 = Level.builder()
          .levelName("Level 3")
          .starsOnLevel(4)
          .bossOnLevel(false)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      levelRepository.save(testLevel1);
      levelRepository.save(testLevel2);
      levelRepository.save(level3);

      // Act & Assert
      mockMvc.perform(get("/api/levels")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(3)))
          .andExpect(jsonPath("$[0].levelName").value("Level 1"))
          .andExpect(jsonPath("$[1].levelName").value("Level 2 - Boss"))
          .andExpect(jsonPath("$[2].levelName").value("Level 3"));
    }
  }

  // ========== GET Level By ID Tests ==========

  @Nested
  @DisplayName("GET /api/levels/{id} - Get Level By ID")
  class GetLevelByIdTests {

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return level by id")
    void shouldReturnLevelById() throws Exception {
      // Arrange
      Level savedLevel = levelRepository.save(testLevel1);

      // Act & Assert
      mockMvc.perform(get("/api/levels/" + savedLevel.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").value(savedLevel.getId()))
          .andExpect(jsonPath("$.levelName").value("Level 1"))
          .andExpect(jsonPath("$.starsOnLevel").value(3))
          .andExpect(jsonPath("$.bossOnLevel").value(false));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return 404 when level not found")
    void shouldReturn404WhenLevelNotFound() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/levels/999999")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should return boss level with correct flag")
    void shouldReturnBossLevelWithCorrectFlag() throws Exception {
      // Arrange
      Level bossLevel = levelRepository.save(testLevel2);

      // Act & Assert
      mockMvc.perform(get("/api/levels/" + bossLevel.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.bossOnLevel").value(true))
          .andExpect(jsonPath("$.starsOnLevel").value(5));
    }
  }

  // ========== POST Create Level Tests ==========

  @Nested
  @DisplayName("POST /api/levels/create - Create Level")
  class CreateLevelTests {

    @Test
    @Tag("integration")
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should create level with valid admin password")
    void shouldCreateLevelWithValidAdminPassword() throws Exception {
      // Arrange
      LevelDto newLevel = LevelDto.builder()
          .levelName("Level 3")
          .starsOnLevel(4)
          .bossOnLevel(false)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/levels/create")
              .header(ADMIN_PASSWORD_HEADER, adminPassword)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(newLevel)))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.levelName").value("Level 3"))
          .andExpect(jsonPath("$.starsOnLevel").value(4))
          .andExpect(jsonPath("$.bossOnLevel").value(false));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should reject creation with invalid admin password")
    void shouldRejectCreationWithInvalidAdminPassword() throws Exception {
      // Arrange
      LevelDto newLevel = LevelDto.builder()
          .levelName("Level 3")
          .starsOnLevel(4)
          .bossOnLevel(false)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/levels/create")
              .header(ADMIN_PASSWORD_HEADER, "wrongPassword")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(newLevel)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should create boss level")
    void shouldCreateBossLevel() throws Exception {
      // Arrange
      LevelDto bossLevel = LevelDto.builder()
          .levelName("Boss Level")
          .starsOnLevel(10)
          .bossOnLevel(true)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/levels/create")
              .header(ADMIN_PASSWORD_HEADER, adminPassword)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(bossLevel)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.levelName").value("Boss Level"))
          .andExpect(jsonPath("$.bossOnLevel").value(true))
          .andExpect(jsonPath("$.starsOnLevel").value(10));
    }

    @Test
    @Tag("integration")
    @WithMockUser
    @DisplayName("Should create level with zero stars")
    void shouldCreateLevelWithZeroStars() throws Exception {
      // Arrange
      LevelDto tutorialLevel = LevelDto.builder()
          .levelName("Tutorial")
          .starsOnLevel(0)
          .bossOnLevel(false)
          .build();

      // Act & Assert
      mockMvc.perform(post("/api/levels/create")
              .header(ADMIN_PASSWORD_HEADER, adminPassword)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(tutorialLevel)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.starsOnLevel").value(0));
    }

    // ========== PUT Update Level Tests ==========

    @Nested
    @DisplayName("PUT /api/levels/update/{id} - Update Level")
    class UpdateLevelTests {

      @Test
      @Tag("integration")
      @WithMockUser(roles = "ADMIN")
      @DisplayName("Should update level with valid admin password")
      void shouldUpdateLevelWithValidAdminPassword() throws Exception {
        // Arrange
        Level savedLevel = levelRepository.save(testLevel1);

        LevelDto updateDto = LevelDto.builder()
            .levelName("Updated Level")
            .starsOnLevel(7)
            .bossOnLevel(true)
            .build();

        // Act & Assert
        mockMvc.perform(put("/api/levels/update/" + savedLevel.getId())
                .header(ADMIN_PASSWORD_HEADER, adminPassword)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(savedLevel.getId()))
            .andExpect(jsonPath("$.levelName").value("Updated Level"))
            .andExpect(jsonPath("$.starsOnLevel").value(7))
            .andExpect(jsonPath("$.bossOnLevel").value(true));
      }

      @Test
      @Tag("integration")
      @WithMockUser
      @DisplayName("Should reject update with invalid admin password")
      void shouldRejectUpdateWithInvalidAdminPassword() throws Exception {
        // Arrange
        Level savedLevel = levelRepository.save(testLevel1);

        LevelDto updateDto = LevelDto.builder()
            .levelName("Updated Level")
            .starsOnLevel(7)
            .bossOnLevel(true)
            .build();

        // Act & Assert
        mockMvc.perform(put("/api/levels/update/" + savedLevel.getId())
                .header(ADMIN_PASSWORD_HEADER, "wrongPassword")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isUnauthorized());
      }

      @Test
      @Tag("integration")
      @WithMockUser
      @DisplayName("Should return 404 when updating non-existent level")
      void shouldReturn404WhenUpdatingNonExistentLevel() throws Exception {
        // Arrange
        LevelDto updateDto = LevelDto.builder()
            .levelName("Updated Level")
            .starsOnLevel(7)
            .bossOnLevel(true)
            .build();

        // Act & Assert
        mockMvc.perform(put("/api/levels/update/999999")
                .header(ADMIN_PASSWORD_HEADER, adminPassword)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isNotFound());
      }

      @Test
      @Tag("integration")
      @WithMockUser(roles = "ADMIN")
      @DisplayName("Should update boss level to regular level")
      void shouldUpdateBossLevelToRegularLevel() throws Exception {
        // Arrange
        Level bossLevel = levelRepository.save(testLevel2);

        LevelDto updateDto = LevelDto.builder()
            .levelName("No Longer Boss")
            .starsOnLevel(3)
            .bossOnLevel(false)
            .build();

        // Act & Assert
        mockMvc.perform(put("/api/levels/update/" + bossLevel.getId())
                .header(ADMIN_PASSWORD_HEADER, adminPassword)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bossOnLevel").value(false));
      }

      @Test
      @Tag("integration")
      @WithMockUser(roles = "ADMIN")
      @DisplayName("Should update only stars keeping other fields")
      void shouldUpdateOnlyStarsKeepingOtherFields() throws Exception {
        // Arrange
        Level savedLevel = levelRepository.save(testLevel1);

        LevelDto updateDto = LevelDto.builder()
            .levelName("Level 1")
            .starsOnLevel(5)
            .bossOnLevel(false)
            .build();

        // Act & Assert
        mockMvc.perform(put("/api/levels/update/" + savedLevel.getId())
                .header(ADMIN_PASSWORD_HEADER, adminPassword)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.levelName").value("Level 1"))
            .andExpect(jsonPath("$.starsOnLevel").value(5))
            .andExpect(jsonPath("$.bossOnLevel").value(false));
      }
    }

    // ========== DELETE Level Tests ==========

    @Nested
    @DisplayName("DELETE /api/levels/delete/{id} - Delete Level")
    class DeleteLevelTests {

      @Test
      @Tag("integration")
      @WithMockUser(roles = "ADMIN")
      @DisplayName("Should delete level with valid admin password")
      void shouldDeleteLevelWithValidAdminPassword() throws Exception {
        // Arrange
        Level savedLevel = levelRepository.save(testLevel1);

        // Act & Assert
        mockMvc.perform(delete("/api/levels/delete/" + savedLevel.getId())
                .header(ADMIN_PASSWORD_HEADER, adminPassword))
            .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/levels/" + savedLevel.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
      }

      @Test
      @Tag("integration")
      @WithMockUser
      @DisplayName("Should reject deletion with invalid admin password")
      void shouldRejectDeletionWithInvalidAdminPassword() throws Exception {
        // Arrange
        Level savedLevel = levelRepository.save(testLevel1);

        // Act & Assert
        mockMvc.perform(delete("/api/levels/delete/" + savedLevel.getId())
                .header(ADMIN_PASSWORD_HEADER, "wrongPassword"))
            .andExpect(status().isUnauthorized());
      }

      @Test
      @Tag("integration")
      @WithMockUser
      @DisplayName("Should return 404 when deleting non-existent level")
      void shouldReturn404WhenDeletingNonExistentLevel() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/levels/delete/999999")
                .header(ADMIN_PASSWORD_HEADER, adminPassword))
            .andExpect(status().isNotFound());
      }

      @Test
      @Tag("integration")
      @WithMockUser(roles = "ADMIN")
      @DisplayName("Should successfully delete boss level")
      void shouldSuccessfullyDeleteBossLevel() throws Exception {
        // Arrange
        Level bossLevel = levelRepository.save(testLevel2);

        // Act & Assert
        mockMvc.perform(delete("/api/levels/delete/" + bossLevel.getId())
                .header(ADMIN_PASSWORD_HEADER, adminPassword))
            .andExpect(status().isNoContent());
      }
    }

    // ========== Edge Cases Tests ==========

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

      @Test
      @Tag("integration")
      @WithMockUser(roles = "ADMIN")
      @DisplayName("Should handle level with very long name")
      void shouldHandleLevelWithVeryLongName() throws Exception {
        // Arrange
        String longName = "A".repeat(200);
        LevelDto levelDto = LevelDto.builder()
            .levelName(longName)
            .starsOnLevel(3)
            .bossOnLevel(false)
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/levels/create")
                .header(ADMIN_PASSWORD_HEADER, adminPassword)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(levelDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.levelName").value(longName));
      }

      @Test
      @Tag("integration")
      @WithMockUser(roles = "ADMIN")
      @DisplayName("Should handle level with special characters in name")
      void shouldHandleLevelWithSpecialCharactersInName() throws Exception {
        // Arrange
        LevelDto levelDto = LevelDto.builder()
            .levelName("Level @#$% Special!")
            .starsOnLevel(3)
            .bossOnLevel(false)
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/levels/create")
                .header(ADMIN_PASSWORD_HEADER, adminPassword)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(levelDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.levelName").value("Level @#$% Special!"));
      }

      @Test
      @Tag("integration")
      @WithMockUser(roles = "ADMIN")
      @DisplayName("Should handle level with maximum stars")
      void shouldHandleLevelWithMaximumStars() throws Exception {
        // Arrange
        LevelDto levelDto = LevelDto.builder()
            .levelName("Max Stars Level")
            .starsOnLevel(Integer.MAX_VALUE)
            .bossOnLevel(false)
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/levels/create")
                .header(ADMIN_PASSWORD_HEADER, adminPassword)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(levelDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.starsOnLevel").value(Integer.MAX_VALUE));
      }

      @Test
      @Tag("integration")
      @WithMockUser
      @DisplayName("Should reject creation without admin password header")
      void shouldRejectCreationWithoutAdminPasswordHeader() throws Exception {
        // Arrange
        LevelDto levelDto = LevelDto.builder()
            .levelName("Test Level")
            .starsOnLevel(3)
            .bossOnLevel(false)
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/levels/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(levelDto)))
            .andExpect(status().isInternalServerError());
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
        mockMvc.perform(get("/api/levels")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
      }

      @Test
      @Tag("integration")
      @WithMockUser
      @DisplayName("Should allow authenticated users to read levels")
      void shouldAllowAuthenticatedUsersToReadLevels() throws Exception {
        // Arrange
        levelRepository.save(testLevel1);

        // Act & Assert
        mockMvc.perform(get("/api/levels")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
      }

      @Test
      @Tag("integration")
      @WithMockUser
      @DisplayName("Should reject admin operations without admin password")
      void shouldRejectAdminOperationsWithoutAdminPassword() throws Exception {
        // Arrange
        LevelDto levelDto = LevelDto.builder()
            .levelName("Test Level")
            .starsOnLevel(3)
            .bossOnLevel(false)
            .build();

        // Act & Assert - Create
        mockMvc.perform(post("/api/levels/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(levelDto)))
            .andExpect(status().isInternalServerError());

        // Act & Assert - Update
        mockMvc.perform(put("/api/levels/update/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(levelDto)))
            .andExpect(status().isInternalServerError());

        // Act & Assert - Delete
        mockMvc.perform(delete("/api/levels/delete/1"))
            .andExpect(status().isInternalServerError());
      }
    }
  }
}
