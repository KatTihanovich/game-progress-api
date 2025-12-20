package com.esdc.gameapi.repository;

import com.esdc.gameapi.domain.entity.UserAchievement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for user achievements entities.
 */
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
  List<UserAchievement> findByUserId(Long userId);
}