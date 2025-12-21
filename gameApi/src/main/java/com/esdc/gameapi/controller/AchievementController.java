package com.esdc.gameapi.controller;

import com.esdc.gameapi.domain.dto.AchievementDto;
import com.esdc.gameapi.domain.dto.UserAchievementDto;
import com.esdc.gameapi.service.AchievementService;
import com.esdc.gameapi.service.AdminAuthService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for achievement CRUD operations with admin authentication.
 */
@Slf4j
@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {
  private static final String ADMIN_PASSWORD_HEADER = "Admin-Password";

  private final AchievementService achievementService;
  private final AdminAuthService adminAuthService;

  @Value("${admin.password}")
  private String adminPassword;

  /**
   * Gets all achievements.
   */
  @GetMapping
  public ResponseEntity<List<AchievementDto>> getAllAchievements() {
    log.debug("Request to get all achievements");
    return ResponseEntity.ok(achievementService.getAllAchievements());
  }

  /**
   * Gets user achievements by ID.
   */
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<UserAchievementDto>> getUserAchievements(@PathVariable Long userId) {
    log.debug("Request to get achievements for user: {}", userId);
    return ResponseEntity.ok(achievementService.getAchievementsByUserId(userId));
  }

  /**
   * Creates new achievement (admin only).
   */
  @PostMapping("/create")
  public ResponseEntity<AchievementDto> createAchievement(
      @RequestHeader(ADMIN_PASSWORD_HEADER) String password,
      @RequestBody AchievementDto dto) {
    adminAuthService.validateAdminPassword(password);
    log.info("Admin creating achievement: {}", dto.getAchievementName());
    AchievementDto created = achievementService.createAchievement(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * Updates existing achievement (admin only).
   */
  @PutMapping("/update/{id}")
  public ResponseEntity<AchievementDto> updateAchievement(
      @RequestHeader(ADMIN_PASSWORD_HEADER) String password,
      @PathVariable Long id,
      @RequestBody AchievementDto dto) {
    adminAuthService.validateAdminPassword(password);
    log.info("Admin updating achievement: {}", id);
    AchievementDto updated = achievementService.updateAchievement(id, dto);
    return ResponseEntity.ok(updated);
  }

  /**
   * Deletes achievement by ID (admin only).
   */
  @DeleteMapping("/delete/{id}")
  public ResponseEntity<Void> deleteAchievement(
      @RequestHeader(ADMIN_PASSWORD_HEADER) String password,
      @PathVariable Long id) {
    adminAuthService.validateAdminPassword(password);
    log.info("Admin deleting achievement: {}", id);
    achievementService.deleteAchievement(id);
    return ResponseEntity.noContent().build();
  }
}
