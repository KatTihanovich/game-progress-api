package com.esdc.gameApi.controller;

import com.esdc.gameApi.domain.dto.ProgressDto;
import com.esdc.gameApi.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping
    public ResponseEntity<ProgressDto> createProgress(@Valid @RequestBody ProgressDto request) {
        ProgressDto response = progressService.createProgress(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}/{levelId}")
    public ResponseEntity<ProgressDto> getLatestProgressByUserAndLevel(
            @PathVariable Long userId,
            @PathVariable Long levelId) {
        ProgressDto response = progressService.getLatestProgressByUserAndLevel(userId, levelId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<ProgressDto>> getProgressByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(progressService.getProgressByUserId(userId));
    }
}
