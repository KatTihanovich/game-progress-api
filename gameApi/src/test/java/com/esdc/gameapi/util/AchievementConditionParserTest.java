package com.esdc.gameapi.util;

import com.esdc.gameapi.domain.AchievementConditionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("Achievement Condition Parser Tests")
class AchievementConditionParserTest {

  @Test
  @Tag("unit")
  @DisplayName("Should return null for null description")
  void shouldReturnNullForNullDescription() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse(null);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return null for empty description")
  void shouldReturnNullForEmptyDescription() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse("");

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return null for whitespace description")
  void shouldReturnNullForWhitespaceDescription() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse("   ");

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should parse DEFEAT_BOSS without required value")
  void shouldParseDefeatBoss() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse("defeat boss");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(AchievementConditionType.DEFEAT_BOSS);
    assertThat(result.getRequiredValue()).isNull();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should parse DEFEAT_BOSS case insensitive")
  void shouldParseDefeatBossCaseInsensitive() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse("DEFEAT BOSS");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(AchievementConditionType.DEFEAT_BOSS);
    assertThat(result.getRequiredValue()).isNull();
  }

  @ParameterizedTest
  @Tag("unit")
  @CsvSource({
      "complete 10 levels, TOTAL_LEVELS, 10",
      "complete 5 levels, TOTAL_LEVELS, 5",
      "complete 100 levels, TOTAL_LEVELS, 100",
      "COMPLETE 25 LEVELS, TOTAL_LEVELS, 25"
  })
  @DisplayName("Should parse TOTAL_LEVELS with different values")
  void shouldParseTotalLevels(String description, AchievementConditionType expectedType, int expectedValue) {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse(description);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(expectedType);
    assertThat(result.getRequiredValue()).isEqualTo(expectedValue);
  }

  @ParameterizedTest
  @Tag("unit")
  @CsvSource({
      "kill 50 enemies, TOTAL_ENEMIES, 50",
      "kill 100 enemies, TOTAL_ENEMIES, 100",
      "KILL 25 ENEMIES, TOTAL_ENEMIES, 25",
      "kill 1 enemies, TOTAL_ENEMIES, 1"
  })
  @DisplayName("Should parse TOTAL_ENEMIES with different values")
  void shouldParseTotalEnemies(String description, AchievementConditionType expectedType, int expectedValue) {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse(description);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(expectedType);
    assertThat(result.getRequiredValue()).isEqualTo(expectedValue);
  }

  @ParameterizedTest
  @Tag("unit")
  @CsvSource({
      "solve 20 puzzles, TOTAL_PUZZLES, 20",
      "solve 15 puzzles, TOTAL_PUZZLES, 15",
      "solve 30 puzzles, TOTAL_PUZZLES, 30"
  })
  @DisplayName("Should parse TOTAL_PUZZLES with different values")
  void shouldParseTotalPuzzles(String description, AchievementConditionType expectedType, int expectedValue) {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse(description);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(expectedType);
    assertThat(result.getRequiredValue()).isEqualTo(expectedValue);
  }

  @ParameterizedTest
  @Tag("unit")
  @CsvSource({
      "play for 60 minutes, TOTAL_TIME, 60",
      "play for 120 minutes, TOTAL_TIME, 120",
      "PLAY FOR 30 MINUTES, TOTAL_TIME, 30"
  })
  @DisplayName("Should parse TOTAL_TIME with different values")
  void shouldParseTotalTime(String description, AchievementConditionType expectedType, int expectedValue) {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse(description);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(expectedType);
    assertThat(result.getRequiredValue()).isEqualTo(expectedValue);
  }

  @ParameterizedTest
  @Tag("unit")
  @CsvSource({
      "kill 10 enemies in one level, LEVEL_ENEMIES, 10",
      "kill 5 enemies in one level, LEVEL_ENEMIES, 5",
      "KILL 15 ENEMIES IN ONE LEVEL, LEVEL_ENEMIES, 15"
  })
  @DisplayName("Should parse LEVEL_ENEMIES with different values")
  void shouldParseLevelEnemies(String description, AchievementConditionType expectedType, int expectedValue) {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse(description);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(expectedType);
    assertThat(result.getRequiredValue()).isEqualTo(expectedValue);
  }

  @ParameterizedTest
  @Tag("unit")
  @CsvSource({
      "solve 5 puzzles in one level, LEVEL_PUZZLES, 5",
      "solve 3 puzzles in one level, LEVEL_PUZZLES, 3",
      "SOLVE 10 PUZZLES IN ONE LEVEL, LEVEL_PUZZLES, 10"
  })
  @DisplayName("Should parse LEVEL_PUZZLES with different values")
  void shouldParseLevelPuzzles(String description, AchievementConditionType expectedType, int expectedValue) {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse(description);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(expectedType);
    assertThat(result.getRequiredValue()).isEqualTo(expectedValue);
  }

  @ParameterizedTest
  @Tag("unit")
  @CsvSource({
      "complete level under 300 seconds, LEVEL_TIME, 300",
      "complete level under 60 seconds, LEVEL_TIME, 60",
      "COMPLETE LEVEL UNDER 180 SECONDS, LEVEL_TIME, 180"
  })
  @DisplayName("Should parse LEVEL_TIME with different values")
  void shouldParseLevelTime(String description, AchievementConditionType expectedType, int expectedValue) {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse(description);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(expectedType);
    assertThat(result.getRequiredValue()).isEqualTo(expectedValue);
  }

  @ParameterizedTest
  @Tag("unit")
  @CsvSource({
      "complete level 5, SPECIFIC_LEVEL, 5",
      "complete level 1, SPECIFIC_LEVEL, 1",
      "COMPLETE LEVEL 10, SPECIFIC_LEVEL, 10"
  })
  @DisplayName("Should parse SPECIFIC_LEVEL with different values")
  void shouldParseSpecificLevel(String description, AchievementConditionType expectedType, int expectedValue) {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse(description);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(expectedType);
    assertThat(result.getRequiredValue()).isEqualTo(expectedValue);
  }

  @ParameterizedTest
  @Tag("unit")
  @CsvSource({
      "collect 100 stars, TOTAL_STARS, 100",
      "collect 50 stars, TOTAL_STARS, 50",
      "COLLECT 200 STARS, TOTAL_STARS, 200"
  })
  @DisplayName("Should parse TOTAL_STARS with different values")
  void shouldParseTotalStars(String description, AchievementConditionType expectedType, int expectedValue) {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse(description);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(expectedType);
    assertThat(result.getRequiredValue()).isEqualTo(expectedValue);
  }

  @ParameterizedTest
  @Tag("unit")
  @CsvSource({
      "collect 3 stars in one level, LEVEL_STARS, 3",
      "collect 2 stars in one level, LEVEL_STARS, 2",
      "COLLECT 1 STARS IN ONE LEVEL, LEVEL_STARS, 1"
  })
  @DisplayName("Should parse LEVEL_STARS with different values")
  void shouldParseLevelStars(String description, AchievementConditionType expectedType, int expectedValue) {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse(description);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(expectedType);
    assertThat(result.getRequiredValue()).isEqualTo(expectedValue);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle mixed case descriptions")
  void shouldHandleMixedCase() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse("CoMpLeTe 10 LeVeLs");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(AchievementConditionType.TOTAL_LEVELS);
    assertThat(result.getRequiredValue()).isEqualTo(10);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should parse description with leading/trailing spaces")
  void shouldParseWithLeadingTrailingSpaces() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse("  kill 50 enemies  ");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(AchievementConditionType.TOTAL_ENEMIES);
    assertThat(result.getRequiredValue()).isEqualTo(50);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should parse large numbers correctly")
  void shouldParseLargeNumbers() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse("collect 99999 stars");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(AchievementConditionType.TOTAL_STARS);
    assertThat(result.getRequiredValue()).isEqualTo(99999);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return null for invalid description")
  void shouldReturnNullForInvalidDescription() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse("Invalid achievement description");

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return null for partial match")
  void shouldReturnNullForPartialMatch() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse("complete levels");

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should return null for description with text instead of number")
  void shouldReturnNullForTextInsteadOfNumber() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse("complete ten levels");

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @Tag("unit")
  @DisplayName("Should distinguish between similar patterns")
  void shouldDistinguishBetweenSimilarPatterns() {
    // Act & Assert - TOTAL_ENEMIES vs LEVEL_ENEMIES
    AchievementConditionParser totalEnemies = AchievementConditionParser.parse("kill 50 enemies");
    assertThat(totalEnemies.getType()).isEqualTo(AchievementConditionType.TOTAL_ENEMIES);

    AchievementConditionParser levelEnemies = AchievementConditionParser.parse("kill 10 enemies in one level");
    assertThat(levelEnemies.getType()).isEqualTo(AchievementConditionType.LEVEL_ENEMIES);
  }

  @Test
  @Tag("unit")
  @DisplayName("Should handle zero as valid number")
  void shouldHandleZeroAsValidNumber() {
    // Act
    AchievementConditionParser result = AchievementConditionParser.parse("complete 0 levels");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getType()).isEqualTo(AchievementConditionType.TOTAL_LEVELS);
    assertThat(result.getRequiredValue()).isEqualTo(0);
  }
}
