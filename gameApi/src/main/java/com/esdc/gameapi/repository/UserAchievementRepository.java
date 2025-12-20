package com.esdc.gameapi.repository;

import com.esdc.gameapi.domain.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
  List<UserAchievement> findByUserId(Long userId);

  boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);
}