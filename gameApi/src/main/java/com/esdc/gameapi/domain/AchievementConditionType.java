package com.esdc.gameapi.domain;

import lombok.Getter;

/**
 * Types of achievement unlock conditions with parsing patterns.
 */
@Getter
public enum AchievementConditionType {
  LEVEL_ENEMIES("kill %d enemies in one level"),         // "kill 10 enemies in one level"
  LEVEL_PUZZLES("solve %d puzzles in one level"),        // "solve 5 puzzles in one level"
  LEVEL_TIME("complete level under %d seconds"),         // "complete level under 300 seconds"
  LEVEL_STARS("collect %d stars in one level"),          // "collect 3 stars in one level"
  SPECIFIC_LEVEL("complete level %d"),                   // "complete level 5"
  DEFEAT_BOSS("defeat boss"),                            // "defeat boss"
  TOTAL_STARS("collect %d stars"),                       // "collect 100 stars"
  TOTAL_LEVELS("complete %d levels"),                    // "complete 10 levels"
  TOTAL_ENEMIES("kill %d enemies"),                      // "kill 100 enemies"
  TOTAL_PUZZLES("solve %d puzzles"),                     // "solve 50 puzzles"
  TOTAL_TIME("play for %d minutes");                     // "play for 60 minutes"

  private final String pattern;

  AchievementConditionType(String pattern) {
    this.pattern = pattern;
  }
}
