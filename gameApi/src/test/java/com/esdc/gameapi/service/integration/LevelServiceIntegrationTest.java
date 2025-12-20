package com.esdc.gameapi.service.integration;

import com.esdc.gameapi.domain.dto.LevelDto;
import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.LevelRepository;
import com.esdc.gameapi.service.LevelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=mySecretKeyForTestingPurposesOnlyMustBeAtLeast256BitsLong",
    "jwt.expiration=3600000"
})
@DisplayName("Level Service Integration Tests")
class LevelServiceIntegrationTest {

  @Autowired
  private LevelService levelService;

  @Autowired
  private LevelRepository levelRepository;

  private Level savedLevel1;
  private Level savedLevel2;

  @BeforeEach
  void setUp() {
    // Очистка базы перед каждым тестом
    levelRepository.deleteAll();

    // Создаем тестовые уровни
    savedLevel1 = levelRepository.save(Level.builder()
        .levelName("Forest Level")
        .starsOnLevel(3)
        .bossOnLevel(true)
        .build());

    savedLevel2 = levelRepository.save(Level.builder()
        .levelName("Desert Level")
        .starsOnLevel(2)
        .bossOnLevel(false)
        .build());
  }

  @AfterEach
  void tearDown() {
    levelRepository.deleteAll();
  }

  @Test
  @Tag("integration")
  @DisplayName("Should get all levels from database")
  void shouldGetAllLevelsFromDatabase() {
    // Act
    List<LevelDto> levels = levelService.getAllLevels();

    // Assert
    assertThat(levels).hasSize(2);
    assertThat(levels).extracting(LevelDto::getLevelName)
        .containsExactlyInAnyOrder("Forest Level", "Desert Level");
  }

  @Test
  @Tag("integration")
  @DisplayName("Should return empty list when no levels in database")
  void shouldReturnEmptyListWhenNoLevels() {
    // Arrange
    levelRepository.deleteAll();

    // Act
    List<LevelDto> levels = levelService.getAllLevels();

    // Assert
    assertThat(levels).isEmpty();
  }

  @Test
  @Tag("integration")
  @DisplayName("Should get level by id from database")
  void shouldGetLevelByIdFromDatabase() {
    // Act
    LevelDto result = levelService.getLevelById(savedLevel1.getId());

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(savedLevel1.getId());
    assertThat(result.getLevelName()).isEqualTo("Forest Level");
    assertThat(result.getStarsOnLevel()).isEqualTo(3);
    assertThat(result.getBossOnLevel()).isTrue();
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when level not found in database")
  void shouldThrowExceptionWhenLevelNotFoundInDatabase() {
    // Arrange
    Long nonExistentId = 999L;

    // Act & Assert
    assertThatThrownBy(() -> levelService.getLevelById(nonExistentId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Level")
        .hasMessageContaining("999");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should create and persist level to database")
  void shouldCreateAndPersistLevel() {
    // Arrange
    LevelDto newLevel = LevelDto.builder()
        .levelName("Mountain Level")
        .starsOnLevel(5)
        .bossOnLevel(true)
        .build();

    // Act
    LevelDto created = levelService.createLevel(newLevel);

    // Assert
    assertThat(created.getId()).isNotNull();
    assertThat(created.getLevelName()).isEqualTo("Mountain Level");
    assertThat(created.getStarsOnLevel()).isEqualTo(5);
    assertThat(created.getBossOnLevel()).isTrue();

    // Verify it's actually in database
    Level fromDb = levelRepository.findById(created.getId()).orElseThrow();
    assertThat(fromDb.getLevelName()).isEqualTo("Mountain Level");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should update existing level in database")
  void shouldUpdateExistingLevelInDatabase() {
    // Arrange
    LevelDto updateDto = LevelDto.builder()
        .levelName("Updated Forest")
        .starsOnLevel(4)
        .bossOnLevel(false)
        .build();

    // Act
    LevelDto updated = levelService.updateLevel(savedLevel1.getId(), updateDto);

    // Assert
    assertThat(updated.getId()).isEqualTo(savedLevel1.getId());
    assertThat(updated.getLevelName()).isEqualTo("Updated Forest");
    assertThat(updated.getStarsOnLevel()).isEqualTo(4);
    assertThat(updated.getBossOnLevel()).isFalse();

    // Verify changes persisted
    Level fromDb = levelRepository.findById(savedLevel1.getId()).orElseThrow();
    assertThat(fromDb.getLevelName()).isEqualTo("Updated Forest");
    assertThat(fromDb.getStarsOnLevel()).isEqualTo(4);
    assertThat(fromDb.getBossOnLevel()).isFalse();
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when updating non-existent level")
  void shouldThrowExceptionWhenUpdatingNonExistentLevel() {
    // Arrange
    LevelDto updateDto = LevelDto.builder()
        .levelName("Ghost Level")
        .starsOnLevel(3)
        .bossOnLevel(true)
        .build();

    // Act & Assert
    assertThatThrownBy(() -> levelService.updateLevel(999L, updateDto))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should delete level from database")
  void shouldDeleteLevelFromDatabase() {
    // Arrange
    Long levelId = savedLevel1.getId();

    // Act
    levelService.deleteLevel(levelId);

    // Assert
    assertThat(levelRepository.findById(levelId)).isEmpty();
    assertThat(levelRepository.findAll()).hasSize(1); // Only savedLevel2 remains
  }

  @Test
  @Tag("integration")
  @DisplayName("Should throw exception when deleting non-existent level")
  void shouldThrowExceptionWhenDeletingNonExistentLevel() {
    // Act & Assert
    assertThatThrownBy(() -> levelService.deleteLevel(999L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should handle multiple create operations")
  void shouldHandleMultipleCreateOperations() {
    // Arrange
    LevelDto level3 = LevelDto.builder()
        .levelName("Ice Level")
        .starsOnLevel(3)
        .bossOnLevel(true)
        .build();

    LevelDto level4 = LevelDto.builder()
        .levelName("Fire Level")
        .starsOnLevel(3)
        .bossOnLevel(true)
        .build();

    // Act
    levelService.createLevel(level3);
    levelService.createLevel(level4);

    // Assert
    List<LevelDto> allLevels = levelService.getAllLevels();
    assertThat(allLevels).hasSize(4); // 2 initial + 2 new
    assertThat(allLevels).extracting(LevelDto::getLevelName)
        .contains("Ice Level", "Fire Level");
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should persist level with zero stars")
  void shouldPersistLevelWithZeroStars() {
    // Arrange
    LevelDto tutorialLevel = LevelDto.builder()
        .levelName("Tutorial")
        .starsOnLevel(0)
        .bossOnLevel(false)
        .build();

    // Act
    LevelDto created = levelService.createLevel(tutorialLevel);

    // Assert
    Level fromDb = levelRepository.findById(created.getId()).orElseThrow();
    assertThat(fromDb.getStarsOnLevel()).isEqualTo(0);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should persist level with high star count")
  void shouldPersistLevelWithHighStarCount() {
    // Arrange
    LevelDto ultimateLevel = LevelDto.builder()
        .levelName("Ultimate Challenge")
        .starsOnLevel(10)
        .bossOnLevel(true)
        .build();

    // Act
    LevelDto created = levelService.createLevel(ultimateLevel);

    // Assert
    Level fromDb = levelRepository.findById(created.getId()).orElseThrow();
    assertThat(fromDb.getStarsOnLevel()).isEqualTo(10);
  }

  @Test
  @Tag("integration")
  @Transactional
  @DisplayName("Should update only specified fields")
  void shouldUpdateOnlySpecifiedFields() {
    // Arrange
    Long originalId = savedLevel1.getId();
    LevelDto updateDto = LevelDto.builder()
        .levelName("Forest Level") // Same name
        .starsOnLevel(5) // Different stars
        .bossOnLevel(true) // Same boss
        .build();

    // Act
    LevelDto updated = levelService.updateLevel(originalId, updateDto);

    // Assert
    assertThat(updated.getId()).isEqualTo(originalId);
    assertThat(updated.getStarsOnLevel()).isEqualTo(5);

    Level fromDb = levelRepository.findById(originalId).orElseThrow();
    assertThat(fromDb.getStarsOnLevel()).isEqualTo(5);
  }
}
