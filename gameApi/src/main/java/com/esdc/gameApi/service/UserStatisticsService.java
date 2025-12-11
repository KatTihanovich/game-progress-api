package com.esdc.gameApi.service;

import com.esdc.gameApi.domain.dto.UserStatisticsDto;
import com.esdc.gameApi.domain.entity.Progress;
import com.esdc.gameApi.domain.entity.User;
import com.esdc.gameApi.domain.entity.UserStatistics;
import com.esdc.gameApi.repository.ProgressRepository;
import com.esdc.gameApi.repository.UserRepository;
import com.esdc.gameApi.repository.UserStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserStatisticsService {

    private final UserStatisticsRepository statisticsRepository;
    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;

    /**
     * Получить статистику пользователя
     */
    @Transactional(readOnly = true)
    public Optional<UserStatisticsDto> getStatisticsByUserId(Long userId) {
        return statisticsRepository.findByUserId(userId)
                .map(this::toDto);
    }

    /**
     * Пересчитать общую статистику пользователя на основе всех его прогрессов
     * Вызывается автоматически после создания нового прогресса
     */
    @Transactional
    public UserStatisticsDto recalculateUserStatistics(Long userId) {
        // Валидация пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Получаем все прогрессы пользователя
        List<Progress> allProgress = progressRepository.findByUserId(userId);

        // Подсчитываем статистику
        int totalLevelsCompleted = (int) allProgress.stream()
                .map(progress -> progress.getLevel().getId())
                .distinct()
                .count();

        int totalKilledEnemies = allProgress.stream()
                .mapToInt(Progress::getKilledEnemiesNumber)
                .sum();

        int totalSolvedPuzzles = allProgress.stream()
                .mapToInt(Progress::getSolvedPuzzlesNumber)
                .sum();

        String totalTimePlayed = calculateTotalTime(allProgress);

        // Получаем или создаем запись статистики
        UserStatistics statistics = statisticsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    return new UserStatistics(user);
                });

        // Обновляем значения
        statistics.setTotalLevelsCompleted(totalLevelsCompleted);
        statistics.setTotalTimePlayed(totalTimePlayed);
        statistics.setTotalKilledEnemies(totalKilledEnemies);
        statistics.setTotalSolvedPuzzles(totalSolvedPuzzles);

        // Сохраняем
        UserStatistics savedStatistics = statisticsRepository.save(statistics);

        return toDto(savedStatistics);
    }

    /**
     * Рассчитывает общее время игры из списка прогрессов
     * Складывает все время в формате HH:MM:SS
     */
    private String calculateTotalTime(List<Progress> progressList) {
        int totalSeconds = 0;

        for (Progress progress : progressList) {
            String timeSpent = progress.getTimeSpent();
            if (timeSpent != null && !timeSpent.isEmpty()) {
                totalSeconds += convertTimeToSeconds(timeSpent);
            }
        }

        return convertSecondsToTime(totalSeconds);
    }

    /**
     * Конвертирует время из формата HH:MM:SS в секунды
     */
    private int convertTimeToSeconds(String time) {
        try {
            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return hours * 3600 + minutes * 60 + seconds;
        } catch (Exception e) {
            return 0; // В случае ошибки возвращаем 0
        }
    }

    /**
     * Конвертирует секунды обратно в формат HH:MM:SS
     */
    private String convertSecondsToTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private UserStatisticsDto toDto(UserStatistics stats) {
        return UserStatisticsDto.builder()
                .totalLevelsCompleted(stats.getTotalLevelsCompleted())
                .totalTimePlayed(stats.getTotalTimePlayed())
                .totalKilledEnemies(stats.getTotalKilledEnemies())
                .totalSolvedPuzzles(stats.getTotalSolvedPuzzles())
                .build();
    }
}
