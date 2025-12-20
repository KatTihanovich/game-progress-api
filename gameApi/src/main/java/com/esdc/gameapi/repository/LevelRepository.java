package com.esdc.gameapi.repository;

import com.esdc.gameapi.domain.entity.Level;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for level entities.
 */
public interface LevelRepository extends JpaRepository<Level, Long> {
  Optional<Level> findByLevelName(String levelName);
}