package com.esdc.gameApi.util;

import com.esdc.gameApi.domain.AchievementConditionType;
import lombok.Data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class AchievementConditionParser {
    private AchievementConditionType type;
    private Integer requiredValue;

    /**
     * Парсит описание ачивки и извлекает условие
     */
    public static AchievementConditionParser parse(String description) {
        if (description == null || description.isEmpty()) {
            return null;
        }

        String lowerDesc = description.toLowerCase().trim();

        // Проверяем каждый тип условия
        for (AchievementConditionType type : AchievementConditionType.values()) {
            String pattern = type.getPattern().toLowerCase();

            // Для DEFEAT_BOSS проверяем точное совпадение (без числа)
            if (type == AchievementConditionType.DEFEAT_BOSS) {
                if (lowerDesc.equals(pattern)) {
                    AchievementConditionParser condition = new AchievementConditionParser();
                    condition.setType(type);
                    condition.setRequiredValue(null); // Нет числового значения
                    return condition;
                }
                continue;
            }

            // Для остальных типов парсим число
            String regex = pattern.replace("%d", "(\\d+)");
            Pattern regexPattern = Pattern.compile(regex);
            Matcher matcher = regexPattern.matcher(lowerDesc);

            if (matcher.find()) {
                AchievementConditionParser condition = new AchievementConditionParser();
                condition.setType(type);
                condition.setRequiredValue(Integer.parseInt(matcher.group(1)));
                return condition;
            }
        }

        return null; // Не удалось распарсить
    }
}
