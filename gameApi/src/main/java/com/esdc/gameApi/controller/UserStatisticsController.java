package com.esdc.gameApi.controller;

import com.esdc.gameApi.domain.dto.UserStatisticsDto;
import com.esdc.gameApi.service.UserStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class UserStatisticsController {

    private final UserStatisticsService statisticsService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserStatisticsDto> getUserStatistics(@PathVariable Long userId) {
        return statisticsService.getStatisticsByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
