package com.esdc.gameApi.repository;

import com.esdc.gameApi.domain.entity.Achievement;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Tag("integration")
class AchievementRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByAchievementName_shouldReturnAchievement_whenExists() {
        Achievement achievement = new Achievement();
        achievement.setAchievementName("First Win");
        entityManager.persistAndFlush(achievement);

        Optional<Achievement> found = achievementRepository.findByAchievementName("First Win");

        assertThat(found).isPresent();
        assertThat(found.get().getAchievementName()).isEqualTo("First Win");
    }

    @Test
    void findByAchievementName_shouldReturnEmpty_whenDoesNotExist() {
        Optional<Achievement> found = achievementRepository.findByAchievementName("NonExistent");

        assertThat(found).isEmpty();
    }
}
