package com.petsapp.achievement;

/**
 * Typy warunkow dla osiagniec (achievementow).
 *
 * <p>Odwzorowuje wartosci kolumny condition_type z tabeli achievement.
 * Uzywany w {@link AchievementChecker} zamiast magic stringow.
 */
public enum ConditionType {

  /** Calkowita liczba zlowien uzytkownika. Wymaga: total_catches >= condition_value. */
  TOTAL_CATCHES("total_catches"),

  /** Liczba unikalnych ras zlowionych przez uzytkownika. Wymaga: unique_breeds >= condition_value. */
  UNIQUE_BREEDS("unique_breeds"),

  /** Rarity score nowo zlapanej rasy. Wymaga: rarity >= condition_value. */
  RARE_CATCH("rare_catch"),

  /** Liczba zaakceptowanych znajomych. Wymaga: friends_count >= condition_value. */
  FRIENDS_COUNT("friends_count"),

  /** Streak — dni pod rzad z co najmniej 1 catch. Wymaga: streak >= condition_value. */
  STREAK("streak");

  private final String dbValue;

  ConditionType(String dbValue) {
    this.dbValue = dbValue;
  }

  public String getDbValue() {
    return dbValue;
  }

  /**
   * Zamienia wartosc z bazy danych na enum.
   *
   * @param dbValue wartosc z kolumny condition_type
   * @return odpowiedni ConditionType
   * @throws IllegalArgumentException gdy wartosc jest nieznana
   */
  public static ConditionType fromDbValue(String dbValue) {
    for (ConditionType type : values()) {
      if (type.dbValue.equals(dbValue)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown condition_type: " + dbValue);
  }
}
