package com.esdc.gameapi.repository;

import com.esdc.gameapi.domain.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

  List<Progress> findByUserId(Long userId);

  List<Progress> findByUserIdAndLevelId(Long userId, Long levelId);

  @Query("SELECT COALESCE(SUM(p.stars), 0) FROM Progress p WHERE p.user.id = :userId AND p.level.id = :levelId")
  Integer getTotalStarsByUserIdAndLevelId(@Param("userId") Long userId, @Param("levelId") Long levelId);
}
