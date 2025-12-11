package com.esdc.gameApi.service;

import com.esdc.gameApi.domain.entity.Level;
import com.esdc.gameApi.domain.entity.Progress;
import com.esdc.gameApi.domain.entity.User;
import com.esdc.gameApi.repository.LevelRepository;
import com.esdc.gameApi.repository.ProgressRepository;
import com.esdc.gameApi.repository.UserRepository;
import com.esdc.gameApi.domain.dto.ProgressDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final LevelRepository levelRepository;
    private final UserStatisticsService userStatisticsService;
    private final AchievementService achievementService;

    @Transactional
    public ProgressDto createProgress(ProgressDto request) {
        // Валидация пользователя
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        // Валидация уровня
        Level level = levelRepository.findById(request.getLevelId())
                .orElseThrow(() -> new RuntimeException("Level not found with id: " + request.getLevelId()));

        // Валидация формата времени
        validateTimeFormat(request.getTimeSpent());

        // Создание новой записи прогресса
        Progress progress = new Progress();
        progress.setUser(user);
        progress.setLevel(level);
        progress.setKilledEnemiesNumber(request.getKilledEnemiesNumber());
        progress.setSolvedPuzzlesNumber(request.getSolvedPuzzlesNumber());
        progress.setTimeSpent(request.getTimeSpent());

        Progress savedProgress = progressRepository.save(progress);

        // Пересчет статистики пользователя
        userStatisticsService.recalculateUserStatistics(user.getId());

        // Проверка и выдача ачивок (передаем сохраненный прогресс напрямую)
        achievementService.checkAndUnlockAchievements(user.getId(), level.getId(), savedProgress);

        // Формирование ответа
        return toDto(savedProgress);
    }

    @Transactional(readOnly = true)
    public ProgressDto getLatestProgressByUserAndLevel(Long userId, Long levelId) {
        // Валидация пользователя
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Валидация уровня
        levelRepository.findById(levelId)
                .orElseThrow(() -> new RuntimeException("Level not found with id: " + levelId));

        // Поиск последней попытки по уровню
        Progress latestProgress = progressRepository.findFirstByUserIdAndLevelIdOrderByCreatedAtDesc(userId, levelId)
                .orElseThrow(() -> new RuntimeException(
                        "No progress found for user " + userId + " on level " + levelId));

        return toDto(latestProgress);
    }

    @Transactional(readOnly = true)
    public List<ProgressDto> getProgressByUserId(Long userId) {
        return progressRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private ProgressDto toDto(Progress progress) {
        return ProgressDto.builder()
                .userId(progress.getUser().getId())
                .levelId(progress.getLevel().getId())
                .killedEnemiesNumber(progress.getKilledEnemiesNumber())
                .solvedPuzzlesNumber(progress.getSolvedPuzzlesNumber())
                .timeSpent(progress.getTimeSpent())
                .build();
    }

    private void validateTimeFormat(String timeSpent) {
        if (!timeSpent.matches("^([0-9]{2}):([0-5][0-9]):([0-5][0-9])$")) {
            throw new IllegalArgumentException("Invalid time format. Expected HH:MM:SS");
        }
    }
}
