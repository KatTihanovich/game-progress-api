package com.esdc.gameapi.repository;

import com.esdc.gameapi.domain.entity.Achievement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for achievement entities.
 */
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
  Optional<Achievement> findByAchievementName(String achievementName);
}
