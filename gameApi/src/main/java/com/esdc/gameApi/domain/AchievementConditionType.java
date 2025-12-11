package com.esdc.gameApi.domain;

import lombok.Getter;

@Getter
public enum AchievementConditionType {
    TOTAL_LEVELS("complete %d levels"),                    // "complete 10 levels"
    TOTAL_ENEMIES("kill %d enemies"),                      // "kill 100 enemies"
    TOTAL_PUZZLES("solve %d puzzles"),                     // "solve 50 puzzles"
    TOTAL_TIME("play for %d minutes"),                     // "play for 60 minutes"
    LEVEL_ENEMIES("kill %d enemies in one level"),         // "kill 10 enemies in one level"
    LEVEL_PUZZLES("solve %d puzzles in one level"),        // "solve 5 puzzles in one level"
    LEVEL_TIME("complete level under %d seconds"),         // "complete level under 300 seconds"
    SPECIFIC_LEVEL("complete level %d"),
    DEFEAT_BOSS("defeat boss");// "complete level 5"

    private final String pattern;

    AchievementConditionType(String pattern) {
        this.pattern = pattern;
    }

}
