package com.esdc.gameApi.service;

import com.esdc.gameApi.domain.dto.AuthResponse;
import com.esdc.gameApi.domain.dto.UserLoginDto;
import com.esdc.gameApi.domain.dto.UserRegistrationDto;
import com.esdc.gameApi.domain.dto.UserResponseDto;
import com.esdc.gameApi.domain.entity.User;
import com.esdc.gameApi.repository.UserRepository;
import com.esdc.gameApi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(UserRegistrationDto dto) {
        // Проверка, существует ли пользователь
        if (userRepository.existsByNickname(dto.getNickname())) {
            throw new RuntimeException("Nickname already exists");
        }

        // Создание пользователя с хешированным паролем
        User user = User.builder()
                .nickname(dto.getNickname())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .age(dto.getAge())
                .build();

        user = userRepository.save(user);

        // Генерация JWT токена
        String token = jwtUtil.generateToken(user.getNickname(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(toResponseDto(user))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(UserLoginDto dto) {
        // Аутентификация через Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getNickname(), dto.getPassword())
        );

        // Получаем пользователя
        User user = userRepository.findByNickname(dto.getNickname())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Генерация JWT токена
        String token = jwtUtil.generateToken(user.getNickname(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(toResponseDto(user))
                .build();
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
