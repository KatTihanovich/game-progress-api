package com.esdc.gameApi.controller;

import com.esdc.gameApi.service.LevelService;
import com.esdc.gameApi.domain.dto.LevelDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/levels")
@RequiredArgsConstructor
public class LevelController {

    private final LevelService levelService;

    @GetMapping
    public ResponseEntity<List<LevelDto>> getAllLevels() {
        return ResponseEntity.ok(levelService.getAllLevels());
    }
}
