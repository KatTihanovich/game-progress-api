package com.esdc.gameApi.repository;

import com.esdc.gameApi.domain.entity.Level;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Tag("integration")
class LevelRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByLevelName_shouldReturnLevel_whenLevelExists() {
        Level level = new Level();
        level.setLevelName("Easy");
        entityManager.persistAndFlush(level);

        Optional<Level> found = levelRepository.findByLevelName("Easy");

        assertThat(found).isPresent();
        assertThat(found.get().getLevelName()).isEqualTo("Easy");
    }

    @Test
    void findByLevelName_shouldReturnEmpty_whenLevelDoesNotExist() {
        Optional<Level> found = levelRepository.findByLevelName("NonExistent");

        assertThat(found).isEmpty();
    }

    @Test
    void findByLevelName_shouldBeCaseSensitive() {
        Level level = new Level();
        level.setLevelName("Hard");
        entityManager.persistAndFlush(level);

        Optional<Level> found = levelRepository.findByLevelName("hard");

        assertThat(found).isEmpty();
    }
}
