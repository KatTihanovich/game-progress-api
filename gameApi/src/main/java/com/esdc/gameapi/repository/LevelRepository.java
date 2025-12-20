package com.esdc.gameapi.repository;

import com.esdc.gameapi.domain.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LevelRepository extends JpaRepository<Level, Long> {
  Optional<Level> findByLevelName(String levelName);
}