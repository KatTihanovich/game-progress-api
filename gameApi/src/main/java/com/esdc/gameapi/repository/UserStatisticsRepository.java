package com.esdc.gameapi.repository;

import com.esdc.gameapi.domain.entity.UserStatistics;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for user statistics entities.
 */
public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {
  Optional<UserStatistics> findByUserId(Long userId);
}
