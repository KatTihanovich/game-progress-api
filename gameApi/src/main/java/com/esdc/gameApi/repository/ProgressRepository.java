package com.esdc.gameApi.repository;

import com.esdc.gameApi.domain.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

    List<Progress> findByUserId(Long userId);

    Optional<Progress> findByUserIdAndLevelId(Long userId, Long levelId);

    Optional<Progress> findFirstByUserIdAndLevelIdOrderByCreatedAtDesc(Long userId, Long levelId);
}
