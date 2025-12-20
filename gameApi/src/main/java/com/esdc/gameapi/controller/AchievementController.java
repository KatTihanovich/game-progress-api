package com.esdc.gameapi.controller;

import com.esdc.gameapi.domain.dto.AchievementDto;
import com.esdc.gameapi.domain.dto.UserAchievementDto;
import com.esdc.gameapi.exception.UnauthorizedException;
import com.esdc.gameapi.service.AchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {
  private static final String ADMIN_PASSWORD_HEADER = "Admin-Password";

  private final AchievementService achievementService;

  @Value("${admin.password}")
  private String adminPassword;

  @GetMapping
  public ResponseEntity<List<AchievementDto>> getAllAchievements() {
    log.debug("Request to get all achievements");
    return ResponseEntity.ok(achievementService.getAllAchievements());
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<List<UserAchievementDto>> getUserAchievements(@PathVariable Long userId) {
    log.debug("Request to get achievements for user: {}", userId);
    return ResponseEntity.ok(achievementService.getAchievementsByUserId(userId));
  }

  @PostMapping("/create")
  public ResponseEntity<AchievementDto> createAchievement(
      @RequestHeader(ADMIN_PASSWORD_HEADER) String password,
      @RequestBody AchievementDto dto) {
    validateAdminPassword(password);
    log.info("Admin creating achievement: {}", dto.getAchievementName());
    AchievementDto created = achievementService.createAchievement(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/update/{id}")
  public ResponseEntity<AchievementDto> updateAchievement(
      @RequestHeader(ADMIN_PASSWORD_HEADER) String password,
      @PathVariable Long id,
      @RequestBody AchievementDto dto) {
    validateAdminPassword(password);
    log.info("Admin updating achievement: {}", id);
    AchievementDto updated = achievementService.updateAchievement(id, dto);
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/delete/{id}")
  public ResponseEntity<Void> deleteAchievement(
      @RequestHeader(ADMIN_PASSWORD_HEADER) String password,
      @PathVariable Long id) {
    validateAdminPassword(password);
    log.info("Admin deleting achievement: {}", id);
    achievementService.deleteAchievement(id);
    return ResponseEntity.noContent().build();
  }

  private void validateAdminPassword(String password) {
    if (!adminPassword.equals(password)) {
      log.warn("Invalid admin password attempt");
      throw new UnauthorizedException("Invalid admin password");
    }
  }
}
