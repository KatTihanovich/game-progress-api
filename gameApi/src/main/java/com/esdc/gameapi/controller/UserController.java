package com.esdc.gameapi.controller;

import com.esdc.gameapi.domain.dto.AuthResponse;
import com.esdc.gameapi.domain.dto.UserLoginDto;
import com.esdc.gameapi.domain.dto.UserRegistrationDto;
import com.esdc.gameapi.domain.dto.UserResponseDto;
import com.esdc.gameapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user authentication and management.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and login endpoints")
public class UserController {

  private final UserService userService;

  /**
   * Registers new user and returns JWT token.
   */
  @PostMapping("/register")
  @Operation(summary = "Register a new user",
      description = "Creates a new user and returns a JWT token")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "User successfully registered"),
      @ApiResponse(responseCode = "409", description = "Nickname already exists"),
      @ApiResponse(responseCode = "400", description = "Invalid input data")
  })
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRegistrationDto dto) {
    AuthResponse response = userService.register(dto);
    log.info("User registered successfully: {}", dto.getNickname());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Authenticates user and returns JWT token.
   */
  @PostMapping("/login")
  @Operation(summary = "User login",
      description = "Authenticates user and returns a JWT token")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials")
  })
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody UserLoginDto dto) {
    log.info("Login request for user: {}", dto.getNickname());
    AuthResponse response = userService.login(dto);
    return ResponseEntity.ok(response);
  }

  /**
   * Updates existing user data.
   */
  @PutMapping("/{userId}")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Update user")
  public ResponseEntity<UserResponseDto> updateUser(
      @PathVariable Long userId,
      @Valid @RequestBody UserRegistrationDto dto) {
    log.info("Update request for user: {}", userId);
    UserResponseDto updatedUser = userService.updateUser(userId, dto);
    return ResponseEntity.ok(updatedUser);
  }

  /**
   * Deletes user by ID.
   */
  @DeleteMapping("/{userId}")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Delete user")
  public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
    log.info("Delete request for user: {}", userId);
    userService.deleteUser(userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Gets user nickname by ID.
   */
  @GetMapping("/{userId}/nickname")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get user nickname")
  public ResponseEntity<String> getUserNickname(@PathVariable Long userId) {
    log.debug("Request to get nickname for user: {}", userId);
    String nickname = userService.getUserNickname(userId);
    return ResponseEntity.ok(nickname);
  }
}