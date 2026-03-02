package com.petsapp.achievement;

/**
 * DTO odpowiedzi dla definicji achievementu.
 */
public record AchievementResponse(
    int id,
    String code,
    String name,
    String namePl,
    String description,
    String iconUrl,
    String conditionType,
    int conditionValue,
    boolean unlocked) {

  public static AchievementResponse from(Achievement achievement, boolean unlocked) {
    return new AchievementResponse(
        achievement.getId(),
        achievement.getCode(),
        achievement.getName(),
        achievement.getNamePl(),
        achievement.getDescription(),
        achievement.getIconUrl(),
        achievement.getConditionType(),
        achievement.getConditionValue(),
        unlocked);
  }
}
