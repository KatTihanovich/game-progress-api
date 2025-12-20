package com.esdc.gameapi.service;

import com.esdc.gameapi.domain.dto.AuthResponse;
import com.esdc.gameapi.domain.dto.UserLoginDto;
import com.esdc.gameapi.domain.dto.UserRegistrationDto;
import com.esdc.gameapi.domain.dto.UserResponseDto;
import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.exception.DuplicateResourceException;
import com.esdc.gameapi.exception.ResourceNotFoundException;
import com.esdc.gameapi.repository.UserRepository;
import com.esdc.gameapi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user authentication and management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final AuthenticationManager authenticationManager;

  /**
   * Registers new user and generates JWT token.
   */
  @Transactional
  public AuthResponse register(UserRegistrationDto dto) {
    log.info("Attempting to register user: {}", dto.getNickname());

    if (userRepository.existsByNickname(dto.getNickname())) {
      throw new DuplicateResourceException("User", "nickname", dto.getNickname());
    }

    User user = User.builder()
        .nickname(dto.getNickname())
        .passwordHash(passwordEncoder.encode(dto.getPassword()))
        .age(dto.getAge())
        .build();

    user = userRepository.save(user);
    log.info("User registered successfully: {}", user.getNickname());

    String token = jwtUtil.generateToken(user.getNickname(), user.getId());

    return AuthResponse.builder()
        .token(token)
        .user(toResponseDto(user))
        .build();
  }

  /**
   * Authenticates user and generates JWT token.
   */
  @Transactional(readOnly = true)
  public AuthResponse login(UserLoginDto dto) {
    log.info("Attempting to login user: {}", dto.getNickname());

    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(dto.getNickname(), dto.getPassword())
    );

    User user = userRepository.findByNickname(dto.getNickname())
        .orElseThrow(() -> new ResourceNotFoundException("User", "nickname", dto.getNickname()));

    log.info("User logged in successfully: {}", user.getNickname());

    String token = jwtUtil.generateToken(user.getNickname(), user.getId());

    return AuthResponse.builder()
        .token(token)
        .user(toResponseDto(user))
        .build();
  }

  /**
   * Updates existing user data.
   */
  @Transactional
  public UserResponseDto updateUser(Long userId, UserRegistrationDto dto) {
    log.info("Attempting to update user: {}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    user.setNickname(dto.getNickname());
    user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
    user.setAge(dto.getAge());

    User updatedUser = userRepository.save(user);
    log.info("User updated successfully: {}", updatedUser.getId());

    return toResponseDto(updatedUser);
  }

  /**
   * Deletes user by ID.
   */
  @Transactional
  public void deleteUser(Long userId) {
    log.info("Attempting to delete user: {}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    userRepository.delete(user);
    log.info("User deleted successfully: {}", userId);
  }

  /**
   * Gets user nickname by ID.
   */
  @Transactional(readOnly = true)
  public String getUserNickname(Long userId) {
    log.debug("Fetching nickname for user: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    return user.getNickname();
  }

  private UserResponseDto toResponseDto(User user) {
    return UserResponseDto.builder()
        .id(user.getId())
        .nickname(user.getNickname())
        .age(user.getAge())
        .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
        .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
        .build();
  }
}
