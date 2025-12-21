package com.esdc.gameapi.repository;

import com.esdc.gameapi.domain.entity.Level;
import com.esdc.gameapi.domain.entity.Progress;
import com.esdc.gameapi.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Tag("integration")
@DisplayName("Progress Repository Tests")
class ProgressRepositoryTest {

  @Autowired
  private ProgressRepository progressRepository;

  @Autowired
  private TestEntityManager entityManager;

  private User testUser;
  private Level testLevel;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .nickname("testPlayer")
        .passwordHash("password123")
        .build();
    entityManager.persist(testUser);

    testLevel = Level.builder()
        .levelName("Level 1")
        .bossOnLevel(false)
        .starsOnLevel(3)
        .build();
    entityManager.persist(testLevel);

    entityManager.flush();
  }

  @Test
  @DisplayName("Should calculate total stars for user and level")
  void shouldGetTotalStarsByUserIdAndLevelId() {
    // Arrange
    Progress progress1 = createProgress(testUser, testLevel, 3);
    Progress progress2 = createProgress(testUser, testLevel, 2);
    Progress progress3 = createProgress(testUser, testLevel, 1);
    entityManager.persist(progress1);
    entityManager.persist(progress2);
    entityManager.persist(progress3);
    entityManager.flush();

    // Act
    Integer totalStars = progressRepository.getTotalStarsByUserIdAndLevelId(
        testUser.getId(),
        testLevel.getId()
    );

    // Assert
    assertThat(totalStars).isEqualTo(6); // 3 + 2 + 1 = 6
  }

  @Test
  @DisplayName("Should return zero when user has no stars for level")
  void shouldReturnZeroWhenNoStars() {
    // Act
    Integer totalStars = progressRepository.getTotalStarsByUserIdAndLevelId(
        testUser.getId(),
        testLevel.getId()
    );

    // Assert
    assertThat(totalStars).isEqualTo(0);
  }

  @Test
  @DisplayName("Should return zero for non-existent user")
  void shouldReturnZeroForNonExistentUser() {
    // Arrange
    Progress progress = createProgress(testUser, testLevel, 5);
    entityManager.persist(progress);
    entityManager.flush();

    // Act
    Integer totalStars = progressRepository.getTotalStarsByUserIdAndLevelId(
        999L,
        testLevel.getId()
    );

    // Assert
    assertThat(totalStars).isEqualTo(0);
  }

  @Test
  @DisplayName("Should handle multiple users with same level")
  void shouldCalculateStarsOnlyForSpecificUser() {
    // Arrange
    User anotherUser = User.builder()
        .nickname("anotherPlayer")
        .passwordHash("password456")
        .build();
    entityManager.persist(anotherUser);

    Progress progress1 = createProgress(testUser, testLevel, 3);
    Progress progress2 = createProgress(anotherUser, testLevel, 5);
    entityManager.persist(progress1);
    entityManager.persist(progress2);
    entityManager.flush();

    // Act
    Integer totalStarsUser1 = progressRepository.getTotalStarsByUserIdAndLevelId(
        testUser.getId(),
        testLevel.getId()
    );
    Integer totalStarsUser2 = progressRepository.getTotalStarsByUserIdAndLevelId(
        anotherUser.getId(),
        testLevel.getId()
    );

    // Assert
    assertThat(totalStarsUser1).isEqualTo(3);
    assertThat(totalStarsUser2).isEqualTo(5);
  }

  private Progress createProgress(User user, Level level, int stars) {
    Progress progress = new Progress(user, level);
    progress.setStars(stars);
    progress.setKilledEnemiesNumber(10);
    progress.setSolvedPuzzlesNumber(5);
    progress.setTimeSpent("00:10:30");
    return progress;
  }
}
