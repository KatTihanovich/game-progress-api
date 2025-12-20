package com.esdc.gameapi.repository;

import com.esdc.gameapi.domain.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
  Optional<Achievement> findByAchievementName(String achievementName);
}
