package com.esdc.gameapi.service.unit;

import com.esdc.gameapi.domain.dto.LevelDto;
import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.LevelRepository;
import com.esdc.gameapi.service.LevelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Level Service Unit Tests")
class LevelServiceTest {

  @Mock
  private LevelRepository levelRepository;

  @InjectMocks
  private LevelService levelService;

  private Level testLevel;
  private LevelDto testLevelDto;

  @BeforeEach
  void setUp() {
    testLevel = Level.builder()
        .id(1L)
        .levelName("Test Level")
        .starsOnLevel(3)
        .bossOnLevel(true)
        .build();

    testLevelDto = LevelDto.builder()
        .levelName("Test Level")
        .starsOnLevel(3)
        .bossOnLevel(true)
        .build();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should get all levels")
  void shouldGetAllLevels() {
    // Arrange
    Level level2 = Level.builder()
        .id(2L)
        .levelName("Level 2")
        .starsOnLevel(2)
        .bossOnLevel(false)
        .build();

    when(levelRepository.findAll()).thenReturn(Arrays.asList(testLevel, level2));

    // Act
    List<LevelDto> result = levelService.getAllLevels();

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getLevelName()).isEqualTo("Test Level");
    assertThat(result.get(1).getLevelName()).isEqualTo("Level 2");
    verify(levelRepository, times(1)).findAll();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return empty list when no levels exist")
  void shouldReturnEmptyListWhenNoLevels() {
    // Arrange
    when(levelRepository.findAll()).thenReturn(List.of());

    // Act
    List<LevelDto> result = levelService.getAllLevels();

    // Assert
    assertThat(result).isEmpty();
    verify(levelRepository, times(1)).findAll();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should get level by id")
  void shouldGetLevelById() {
    // Arrange
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));

    // Act
    LevelDto result = levelService.getLevelById(1L);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getLevelName()).isEqualTo("Test Level");
    assertThat(result.getStarsOnLevel()).isEqualTo(3);
    assertThat(result.getBossOnLevel()).isTrue();
    verify(levelRepository, times(1)).findById(1L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when level not found by id")
  void shouldThrowExceptionWhenLevelNotFound() {
    // Arrange
    when(levelRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> levelService.getLevelById(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Level")
        .hasMessageContaining("999");

    verify(levelRepository, times(1)).findById(999L);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should create level successfully")
  void shouldCreateLevel() {
    // Arrange
    when(levelRepository.save(any(Level.class))).thenReturn(testLevel);

    // Act
    LevelDto result = levelService.createLevel(testLevelDto);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getLevelName()).isEqualTo("Test Level");
    assertThat(result.getStarsOnLevel()).isEqualTo(3);
    assertThat(result.getBossOnLevel()).isTrue();
    verify(levelRepository, times(1)).save(any(Level.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should create level with no boss")
  void shouldCreateLevelWithNoBoss() {
    // Arrange
    LevelDto noBossDto = LevelDto.builder()
        .levelName("Easy Level")
        .starsOnLevel(1)
        .bossOnLevel(false)
        .build();

    Level noBossLevel = Level.builder()
        .id(2L)
        .levelName("Easy Level")
        .starsOnLevel(1)
        .bossOnLevel(false)
        .build();

    when(levelRepository.save(any(Level.class))).thenReturn(noBossLevel);

    // Act
    LevelDto result = levelService.createLevel(noBossDto);

    // Assert
    assertThat(result.getBossOnLevel()).isFalse();
    verify(levelRepository, times(1)).save(any(Level.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should update level successfully")
  void shouldUpdateLevel() {
    // Arrange
    LevelDto updateDto = LevelDto.builder()
        .levelName("Updated Level")
        .starsOnLevel(5)
        .bossOnLevel(false)
        .build();

    Level updatedLevel = Level.builder()
        .id(1L)
        .levelName("Updated Level")
        .starsOnLevel(5)
        .bossOnLevel(false)
        .build();

    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    when(levelRepository.save(any(Level.class))).thenReturn(updatedLevel);

    // Act
    LevelDto result = levelService.updateLevel(1L, updateDto);

    // Assert
    assertThat(result.getLevelName()).isEqualTo("Updated Level");
    assertThat(result.getStarsOnLevel()).isEqualTo(5);
    assertThat(result.getBossOnLevel()).isFalse();
    verify(levelRepository, times(1)).findById(1L);
    verify(levelRepository, times(1)).save(any(Level.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when updating non-existent level")
  void shouldThrowExceptionWhenUpdatingNonExistentLevel() {
    // Arrange
    when(levelRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> levelService.updateLevel(999L, testLevelDto))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(levelRepository, times(1)).findById(999L);
    verify(levelRepository, never()).save(any(Level.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should delete level successfully")
  void shouldDeleteLevel() {
    // Arrange
    when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
    doNothing().when(levelRepository).delete(testLevel);

    // Act
    levelService.deleteLevel(1L);

    // Assert
    verify(levelRepository, times(1)).findById(1L);
    verify(levelRepository, times(1)).delete(testLevel);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should throw exception when deleting non-existent level")
  void shouldThrowExceptionWhenDeletingNonExistentLevel() {
    // Arrange
    when(levelRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> levelService.deleteLevel(999L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(levelRepository, times(1)).findById(999L);
    verify(levelRepository, never()).delete(any(Level.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle level with zero stars")
  void shouldHandleLevelWithZeroStars() {
    // Arrange
    LevelDto zeroStarsDto = LevelDto.builder()
        .levelName("Training Level")
        .starsOnLevel(0)
        .bossOnLevel(false)
        .build();

    Level zeroStarsLevel = Level.builder()
        .id(3L)
        .levelName("Training Level")
        .starsOnLevel(0)
        .bossOnLevel(false)
        .build();

    when(levelRepository.save(any(Level.class))).thenReturn(zeroStarsLevel);

    // Act
    LevelDto result = levelService.createLevel(zeroStarsDto);

    // Assert
    assertThat(result.getStarsOnLevel()).isEqualTo(0);
    verify(levelRepository, times(1)).save(any(Level.class));
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle level with maximum stars")
  void shouldHandleLevelWithMaxStars() {
    // Arrange
    LevelDto maxStarsDto = LevelDto.builder()
        .levelName("Ultimate Level")
        .starsOnLevel(10)
        .bossOnLevel(true)
        .build();

    Level maxStarsLevel = Level.builder()
        .id(4L)
        .levelName("Ultimate Level")
        .starsOnLevel(10)
        .bossOnLevel(true)
        .build();

    when(levelRepository.save(any(Level.class))).thenReturn(maxStarsLevel);

    // Act
    LevelDto result = levelService.createLevel(maxStarsDto);

    // Assert
    assertThat(result.getStarsOnLevel()).isEqualTo(10);
    verify(levelRepository, times(1)).save(any(Level.class));
  }
}
