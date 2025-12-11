package com.esdc.gameApi.controller;

import com.esdc.gameApi.repository.ProgressRepository;
import com.esdc.gameApi.service.AchievementService;
import com.esdc.gameApi.domain.dto.AchievementDto;
import com.esdc.gameApi.domain.dto.UserAchievementDto;
import com.esdc.gameApi.domain.entity.Progress;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;
    private final ProgressRepository progressRepository;

    /**
     * GET /api/achievements - Все доступные ачивки
     */
    @GetMapping
    public ResponseEntity<List<AchievementDto>> getAllAchievements() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }

    /**
     * GET /api/achievements/user/{userId} - Ачивки конкретного пользователя
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserAchievementDto>> getUserAchievements(@PathVariable Long userId) {
        return ResponseEntity.ok(achievementService.getAchievementsByUserId(userId));
    }

    /**
     * POST /api/achievements/check/{userId} - Принудительная проверка ачивок
     * (опционально, для тестирования или ручного триггера)
     */
    @PostMapping("/check/{userId}")
    public ResponseEntity<List<UserAchievementDto>> checkAchievements(
            @PathVariable Long userId,
            @RequestParam(required = false) Long levelId) {
        Progress latestProgress = null;
        if (levelId != null) {
            latestProgress = progressRepository
                    .findFirstByUserIdAndLevelIdOrderByCreatedAtDesc(userId, levelId)
                    .orElse(null);
        }
        List<UserAchievementDto> unlocked = achievementService.checkAndUnlockAchievements(
                userId, levelId != null ? levelId : 1L, latestProgress);
        return ResponseEntity.ok(unlocked);
    }
}
