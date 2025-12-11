package com.esdc.gameApi.repository;

import com.esdc.gameApi.domain.entity.UserStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {
    Optional<UserStatistics> findByUserId(Long userId);
}
