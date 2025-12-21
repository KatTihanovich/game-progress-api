package com.esdc.gameapi.service;

import com.esdc.gameapi.domain.dto.LevelDto;
import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.LevelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing game levels CRUD operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LevelService {

  private final LevelRepository levelRepository;

  /**
   * Gets all levels.
   */
  @Transactional(readOnly = true)
  public List<LevelDto> getAllLevels() {
    log.debug("Fetching all levels");
    List<LevelDto> levels = levelRepository.findAll().stream()
        .map(this::toDto)
        .toList();
    log.debug("Found {} levels", levels.size());
    return levels;
  }

  /**
   * Gets level by ID.
   */
  @Transactional(readOnly = true)
  public LevelDto getLevelById(Long id) {
    log.debug("Fetching level by id: {}", id);
    Level level = levelRepository.findById(id)
        .orElseThrow(() -> {
          log.warn("Level not found: {}", id);
          return new ResourceNotFoundException("Level", "id", id);
        });
    return toDto(level);
  }

  /**
   * Creates new level.
   */
  @Transactional
  public LevelDto createLevel(LevelDto dto) {
    log.info("Creating level: {}", dto.getLevelName());

    Level level = Level.builder()
        .levelName(dto.getLevelName())
        .starsOnLevel(dto.getStarsOnLevel())
        .bossOnLevel(dto.getBossOnLevel())
        .build();

    Level saved = levelRepository.save(level);
    log.info("Level created: {}", saved.getId());
    return toDto(saved);
  }

  /**
   * Updates existing level.
   */
  @Transactional
  public LevelDto updateLevel(Long id, LevelDto dto) {
    log.info("Updating level: {}", id);

    Level level = levelRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Level", "id", id));

    level.setLevelName(dto.getLevelName());
    level.setStarsOnLevel(dto.getStarsOnLevel());
    level.setBossOnLevel(dto.getBossOnLevel());

    Level updated = levelRepository.save(level);
    log.info("Level updated: {}", updated.getId());
    return toDto(updated);
  }

  /**
   * Deletes level by ID.
   */
  @Transactional
  public void deleteLevel(Long id) {
    log.info("Deleting level: {}", id);

    Level level = levelRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Level", "id", id));

    levelRepository.delete(level);
    log.info("Level deleted: {}", id);
  }

  private LevelDto toDto(Level level) {
    return LevelDto.builder()
        .id(level.getId())
        .levelName(level.getLevelName())
        .starsOnLevel(level.getStarsOnLevel())
        .bossOnLevel(level.getBossOnLevel())
        .build();
  }
}
