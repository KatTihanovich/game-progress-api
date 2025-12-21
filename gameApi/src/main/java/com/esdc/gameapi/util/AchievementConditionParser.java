package com.esdc.gameapi.util;

import com.esdc.gameapi.domain.AchievementConditionType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;

/**
 * Parses achievement condition descriptions into structured data.
 *
 * @see AchievementConditionType
 */
@Data
public class AchievementConditionParser {
  private static final String NUMBER_REGEX = "(\\d+)";
  private static final String NUMBER_PLACEHOLDER = "%d";
  private static final int FIRST_GROUP = 1;

  private AchievementConditionType type;
  private Integer requiredValue;

  /**
   * Parses achievement condition description.
   *
   * @param description condition description
   * @return parsed object or null
   */
  public static AchievementConditionParser parse(String description) {
    if (description == null || description.isEmpty()) {
      return null;
    }

    String lowerDesc = description.toLowerCase().trim();

    for (AchievementConditionType type : AchievementConditionType.values()) {
      String pattern = type.getPattern().toLowerCase();

      if (type == AchievementConditionType.DEFEAT_BOSS) {
        if (lowerDesc.equals(pattern)) {
          AchievementConditionParser condition = new AchievementConditionParser();
          condition.setType(type);
          condition.setRequiredValue(null);
          return condition;
        }
        continue;
      }

      String regex = pattern.replace(NUMBER_PLACEHOLDER, NUMBER_REGEX);
      Pattern regexPattern = Pattern.compile(regex);
      Matcher matcher = regexPattern.matcher(lowerDesc);

      if (matcher.find()) {
        AchievementConditionParser condition = new AchievementConditionParser();
        condition.setType(type);
        condition.setRequiredValue(Integer.parseInt(matcher.group(FIRST_GROUP)));
        return condition;
      }
    }

    return null;
  }
}
