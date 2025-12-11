package com.esdc.gameApi.repository;

import com.esdc.gameApi.domain.entity.Level;
import com.esdc.gameApi.domain.entity.Progress;
import com.esdc.gameApi.domain.entity.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Tag("integration")
class ProgressRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUserId_shouldReturnAllProgressForUser() {
        User user = new User();
        user.setNickname("testuser");
        entityManager.persistAndFlush(user);

        Level level1 = new Level();
        level1.setLevelName("Easy");
        entityManager.persistAndFlush(level1);

        Level level2 = new Level();
        level2.setLevelName("Medium");
        entityManager.persistAndFlush(level2);

        Progress progress1 = new Progress(user, level1);
        entityManager.persistAndFlush(progress1);

        Progress progress2 = new Progress(user, level2);
        entityManager.persistAndFlush(progress2);

        List<Progress> found = progressRepository.findByUserId(user.getId());

        assertThat(found).hasSize(2);
    }

    @Test
    void findByUserId_shouldReturnEmpty_whenNoProgress() {
        List<Progress> found = progressRepository.findByUserId(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findByUserIdAndLevelId_shouldReturnProgress_whenExists() {
        User user = new User();
        user.setNickname("testuser");
        entityManager.persistAndFlush(user);

        Level level = new Level();
        level.setLevelName("Easy");
        entityManager.persistAndFlush(level);

        Progress progress = new Progress(user, level);
        entityManager.persistAndFlush(progress);

        Optional<Progress> found = progressRepository.findByUserIdAndLevelId(
                user.getId(), level.getId()
        );

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(found.get().getLevel().getId()).isEqualTo(level.getId());
    }

    @Test
    void findFirstByUserIdAndLevelIdOrderByCreatedAtDesc_shouldReturnLatestProgress() {
        User user = new User();
        user.setNickname("testuser");
        entityManager.persistAndFlush(user);

        Level level = new Level();
        level.setLevelName("Easy");
        entityManager.persistAndFlush(level);

        Progress oldProgress = new Progress(user, level);
        entityManager.persistAndFlush(oldProgress);
        entityManager.clear(); // Очищаем контекст для создания новой записи

        Progress newProgress = new Progress(user, level);
        entityManager.persistAndFlush(newProgress);

        Optional<Progress> found = progressRepository.findFirstByUserIdAndLevelIdOrderByCreatedAtDesc(
                user.getId(), level.getId()
        );

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(newProgress.getId());
    }
}
