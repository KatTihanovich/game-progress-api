package com.esdc.gameApi.repository;

import com.esdc.gameApi.domain.entity.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Tag("integration")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByNickname_shouldReturnUser_whenUserExists() {
        User user = new User();
        user.setNickname("testuser");
        user.setPasswordHash("hashedpassword123");
        user.setCreatedAt(LocalDateTime.now());   // ← быстрый фикс

        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByNickname("testuser");

        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("testuser");
    }

    @Test
    void findByNickname_shouldReturnEmpty_whenUserDoesNotExist() {
        Optional<User> found = userRepository.findByNickname("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByNickname_shouldReturnTrue_whenUserExists() {
        User user = new User();
        user.setNickname("existinguser");
        user.setPasswordHash("hashedpassword123");
        user.setCreatedAt(LocalDateTime.now());   // ← быстрый фикс

        entityManager.persistAndFlush(user);

        boolean exists = userRepository.existsByNickname("existinguser");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByNickname_shouldReturnFalse_whenUserDoesNotExist() {
        boolean exists = userRepository.existsByNickname("nonexistent");

        assertThat(exists).isFalse();
    }
}
