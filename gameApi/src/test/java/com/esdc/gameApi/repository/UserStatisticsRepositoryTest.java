package com.esdc.gameApi.repository;

import com.esdc.gameApi.domain.entity.User;
import com.esdc.gameApi.domain.entity.UserStatistics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Tag("integration")
class UserStatisticsRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserStatisticsRepository userStatisticsRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUserId_shouldReturnStatistics_whenExists() {
        User user = new User();
        user.setNickname("testuser");
        entityManager.persistAndFlush(user);

        UserStatistics stats = new UserStatistics(user);
        entityManager.persistAndFlush(stats);

        Optional<UserStatistics> found = userStatisticsRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void findByUserId_shouldReturnEmpty_whenDoesNotExist() {
        Optional<UserStatistics> found = userStatisticsRepository.findByUserId(999L);

        assertThat(found).isEmpty();
    }
}
