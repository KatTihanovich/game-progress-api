package com.esdc.gameapi.controller;

import com.esdc.gameapi.domain.dto.LevelDto;
import com.esdc.gameapi.exception.UnauthorizedException;
import com.esdc.gameapi.service.LevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/levels")
@RequiredArgsConstructor
public class LevelController {
  private static final String ADMIN_PASSWORD_HEADER = "Admin-Password";

  private final LevelService levelService;

  @Value("${admin.password}")
  private String adminPassword;

  @GetMapping
  public ResponseEntity<List<LevelDto>> getAllLevels() {
    log.debug("Request to get all levels");
    return ResponseEntity.ok(levelService.getAllLevels());
  }

  @GetMapping("/{id}")
  public ResponseEntity<LevelDto> getLevelById(@PathVariable Long id) {
    log.debug("Request to get level: {}", id);
    return ResponseEntity.ok(levelService.getLevelById(id));
  }

  @PostMapping("/create")
  public ResponseEntity<LevelDto> createLevel(
      @RequestHeader(ADMIN_PASSWORD_HEADER) String password,
      @RequestBody LevelDto dto) {
    validateAdminPassword(password);
    log.info("Admin creating level: {}", dto.getLevelName());
    LevelDto created = levelService.createLevel(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/update/{id}")
  public ResponseEntity<LevelDto> updateLevel(
      @RequestHeader(ADMIN_PASSWORD_HEADER) String password,
      @PathVariable Long id,
      @RequestBody LevelDto dto) {
    validateAdminPassword(password);
    log.info("Admin updating level: {}", id);
    LevelDto updated = levelService.updateLevel(id, dto);
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/delete/{id}")
  public ResponseEntity<Void> deleteLevel(
      @RequestHeader(ADMIN_PASSWORD_HEADER) String password,
      @PathVariable Long id) {
    validateAdminPassword(password);
    log.info("Admin deleting level: {}", id);
    levelService.deleteLevel(id);
    return ResponseEntity.noContent().build();
  }

  private void validateAdminPassword(String password) {
    if (!adminPassword.equals(password)) {
      log.warn("Invalid admin password attempt");
      throw new UnauthorizedException("Invalid admin password");
    }
  }
}
