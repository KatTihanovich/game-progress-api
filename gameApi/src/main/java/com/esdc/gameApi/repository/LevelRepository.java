package com.esdc.gameApi.repository;

import com.esdc.gameApi.domain.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LevelRepository extends JpaRepository<Level, Long> {
    Optional<Level> findByLevelName(String levelName);
}