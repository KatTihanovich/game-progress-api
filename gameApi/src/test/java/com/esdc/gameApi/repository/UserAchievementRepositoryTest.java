package com.esdc.gameApi.repository;

import com.esdc.gameApi.domain.entity.Achievement;
import com.esdc.gameApi.domain.entity.User;
import com.esdc.gameApi.domain.entity.UserAchievement;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Tag("integration")
class UserAchievementRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUserId_shouldReturnAllAchievementsForUser() {
        User user = new User();
        user.setNickname("testuser");
        entityManager.persistAndFlush(user);

        Achievement achievement1 = new Achievement();
        achievement1.setAchievementName("First Win");
        entityManager.persistAndFlush(achievement1);

        Achievement achievement2 = new Achievement();
        achievement2.setAchievementName("Speed Runner");
        entityManager.persistAndFlush(achievement2);

        UserAchievement userAchievement1 = new UserAchievement(user, achievement1);
        entityManager.persistAndFlush(userAchievement1);

        UserAchievement userAchievement2 = new UserAchievement(user, achievement2);
        entityManager.persistAndFlush(userAchievement2);

        List<UserAchievement> found = userAchievementRepository.findByUserId(user.getId());

        assertThat(found).hasSize(2);
    }

    @Test
    void findByUserId_shouldReturnEmpty_whenNoAchievements() {
        List<UserAchievement> found = userAchievementRepository.findByUserId(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void existsByUserIdAndAchievementId_shouldReturnTrue_whenExists() {
        User user = new User();
        user.setNickname("testuser");
        entityManager.persistAndFlush(user);

        Achievement achievement = new Achievement();
        achievement.setAchievementName("First Win");
        entityManager.persistAndFlush(achievement);

        UserAchievement userAchievement = new UserAchievement(user, achievement);
        entityManager.persistAndFlush(userAchievement);

        boolean exists = userAchievementRepository.existsByUserIdAndAchievementId(
                user.getId(), achievement.getId()
        );

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserIdAndAchievementId_shouldReturnFalse_whenDoesNotExist() {
        boolean exists = userAchievementRepository.existsByUserIdAndAchievementId(999L, 999L);

        assertThat(exists).isFalse();
    }

    @Test
    void existsByUserIdAndAchievementId_shouldReturnFalse_whenOnlyUserExists() {
        User user = new User();
        user.setNickname("testuser");
        entityManager.persistAndFlush(user);

        boolean exists = userAchievementRepository.existsByUserIdAndAchievementId(
                user.getId(), 999L
        );

        assertThat(exists).isFalse();
    }
}
