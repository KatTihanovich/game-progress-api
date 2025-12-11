package com.esdc.gameApi.service;

import com.esdc.gameApi.repository.LevelRepository;
import com.esdc.gameApi.domain.dto.LevelDto;
import com.esdc.gameApi.domain.entity.Level;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LevelService {

    private final LevelRepository levelRepository;

    public List<LevelDto> getAllLevels() {
        return levelRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private LevelDto toDto(Level level) {
        return LevelDto.builder()
                .id(level.getId())
                .levelName(level.getLevelName())
                .bossOnLevel(level.getBossOnLevel())
                .build();
    }
}
