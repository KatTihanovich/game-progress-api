package com.esdc.gameApi.service;

import com.esdc.gameApi.domain.entity.*;
import com.esdc.gameApi.repository.*;
import com.esdc.gameApi.domain.dto.AchievementDto;
import com.esdc.gameApi.domain.dto.UserAchievementDto;
import com.esdc.gameApi.util.AchievementConditionParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final UserStatisticsRepository userStatisticsRepository;
    private final ProgressRepository progressRepository;
    private final LevelRepository levelRepository;

    /**
     * Получить все доступные ачивки
     */
    @Transactional(readOnly = true)
    public List<AchievementDto> getAllAchievements() {
        return achievementRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить ачивки пользователя
     */
    @Transactional(readOnly = true)
    public List<UserAchievementDto> getAchievementsByUserId(Long userId) {
        return userAchievementRepository.findByUserId(userId).stream()
                .map(this::toUserAchievementDto)
                .collect(Collectors.toList());
    }

    /**
     * Проверить и выдать ачивки после завершения уровня
     * @param userId ID пользователя
     * @param levelId ID уровня
     * @param currentProgress Текущий (только что созданный) прогресс
     */
    @Transactional
    public List<UserAchievementDto> checkAndUnlockAchievements(Long userId, Long levelId, Progress currentProgress) {
        System.out.println("=== START: checkAndUnlockAchievements for userId=" + userId + ", levelId=" + levelId);

        // Валидация пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        System.out.println("User found: " + user.getId());

        // Получаем статистику пользователя (уже пересчитана)
        UserStatistics stats = userStatisticsRepository.findByUserId(userId).orElse(null);
        System.out.println("User statistics: " + (stats != null ? "found" : "not found"));
        if (stats != null) {
            System.out.println("  - Total levels: " + stats.getTotalLevelsCompleted());
            System.out.println("  - Total enemies: " + stats.getTotalKilledEnemies());
            System.out.println("  - Total puzzles: " + stats.getTotalSolvedPuzzles());
            System.out.println("  - Total time: " + stats.getTotalTimePlayed());
        }

        // Используем переданный прогресс
        Progress latestProgress = currentProgress;
        System.out.println("Current progress: " + (latestProgress != null ? "provided" : "null"));
        if (latestProgress != null) {
            System.out.println("  - Killed enemies: " + latestProgress.getKilledEnemiesNumber());
            System.out.println("  - Solved puzzles: " + latestProgress.getSolvedPuzzlesNumber());
            System.out.println("  - Time spent: " + latestProgress.getTimeSpent());
        }

        // Получаем все ачивки
        List<Achievement> allAchievements = achievementRepository.findAll();
        System.out.println("Total achievements in DB: " + allAchievements.size());

        // Получаем уже разблокированные ачивки
        Set<Long> unlockedIds = userAchievementRepository.findByUserId(userId).stream()
                .map(ua -> ua.getAchievement().getId())
                .collect(Collectors.toSet());
        System.out.println("Already unlocked achievements: " + unlockedIds.size());

        // Проверяем условия для каждой неразблокированной ачивки
        List<UserAchievementDto> newlyUnlocked = new ArrayList<>();

        for (Achievement achievement : allAchievements) {
            System.out.println("\n--- Checking achievement: " + achievement.getAchievementName());
            System.out.println("Description: " + achievement.getAchievementDescription());

            // Пропускаем уже разблокированные
            if (unlockedIds.contains(achievement.getId())) {
                System.out.println("SKIPPED: Already unlocked");
                continue;
            }

            // Парсим условие из описания
            AchievementConditionParser condition = AchievementConditionParser.parse(
                    achievement.getAchievementDescription());

            if (condition == null) {
                System.out.println("SKIPPED: Could not parse condition");
                continue;
            }

            System.out.println("Parsed condition type: " + condition.getType());
            System.out.println("Required value: " + condition.getRequiredValue());

            // Проверяем выполнение условия
            boolean conditionMet = checkCondition(condition, stats, latestProgress, levelId);
            System.out.println("Condition met: " + conditionMet);

            if (conditionMet) {
                // Выдаем ачивку
                UserAchievement userAchievement = new UserAchievement(user, achievement);
                userAchievementRepository.save(userAchievement);
                System.out.println("✓ ACHIEVEMENT UNLOCKED!");

                newlyUnlocked.add(toUserAchievementDto(userAchievement));
            }
        }

        System.out.println("\n=== END: Newly unlocked achievements: " + newlyUnlocked.size());
        return newlyUnlocked;
    }

    /**
     * Проверяет выполнение условия ачивки
     */
    private boolean checkCondition(AchievementConditionParser condition,
                                   UserStatistics stats,
                                   Progress latestProgress,
                                   Long levelId) {
        switch (condition.getType()) {
            case TOTAL_LEVELS:
                return stats != null &&
                        stats.getTotalLevelsCompleted() >= condition.getRequiredValue();

            case TOTAL_ENEMIES:
                return stats != null &&
                        stats.getTotalKilledEnemies() >= condition.getRequiredValue();

            case TOTAL_PUZZLES:
                return stats != null &&
                        stats.getTotalSolvedPuzzles() >= condition.getRequiredValue();

            case TOTAL_TIME:
                if (stats == null) return false;
                int totalMinutes = convertTimeToMinutes(stats.getTotalTimePlayed());
                return totalMinutes >= condition.getRequiredValue();

            case LEVEL_ENEMIES:
                return latestProgress != null &&
                        latestProgress.getKilledEnemiesNumber() >= condition.getRequiredValue();

            case LEVEL_PUZZLES:
                return latestProgress != null &&
                        latestProgress.getSolvedPuzzlesNumber() >= condition.getRequiredValue();

            case LEVEL_TIME:
                if (latestProgress == null) return false;
                int seconds = convertTimeToSeconds(latestProgress.getTimeSpent());
                return seconds <= condition.getRequiredValue();

            case SPECIFIC_LEVEL:
                return levelId.equals(condition.getRequiredValue().longValue());

            case DEFEAT_BOSS:
                System.out.println("  >> Checking DEFEAT_BOSS condition");
                System.out.println("  >> levelId: " + levelId);
                System.out.println("  >> latestProgress: " + (latestProgress != null ? "exists" : "null"));

                if (levelId == null || latestProgress == null) {
                    System.out.println("  >> FAILED: levelId or latestProgress is null");
                    return false;
                }

                // Получаем информацию об уровне
                Level level = levelRepository.findById(levelId).orElse(null);
                System.out.println("  >> level: " + (level != null ? "found" : "not found"));

                if (level == null) {
                    System.out.println("  >> FAILED: level not found");
                    return false;
                }

                System.out.println("  >> level.getBossOnLevel(): " + level.getBossOnLevel());

                // Проверяем, есть ли босс на уровне
                boolean hasBoss = level.getBossOnLevel() != null && level.getBossOnLevel();
                System.out.println("  >> hasBoss: " + hasBoss);

                return hasBoss;

            default:
                return false;
        }
    }

    /**
     * Конвертирует время из HH:MM:SS в минуты
     */
    private int convertTimeToMinutes(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return hours * 60 + minutes + (seconds > 0 ? 1 : 0);
    }

    /**
     * Конвертирует время из HH:MM:SS в секунды
     */
    private int convertTimeToSeconds(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }

    private AchievementDto toDto(Achievement achievement) {
        return AchievementDto.builder()
                .id(achievement.getId())
                .achievementName(achievement.getAchievementName())
                .achievementDescription(achievement.getAchievementDescription())
                .build();
    }

    private UserAchievementDto toUserAchievementDto(UserAchievement ua) {
        return UserAchievementDto.builder()
                .achievementId(ua.getAchievement().getId())
                .achievementName(ua.getAchievement().getAchievementName())
                .achievementDescription(ua.getAchievement().getAchievementDescription())
                .createdAt(ua.getCreatedAt() != null
                        ? ua.getCreatedAt().toString()
                        : LocalDateTime.now().toString()) // Используем текущее время как fallback
                .build();
    }

}
