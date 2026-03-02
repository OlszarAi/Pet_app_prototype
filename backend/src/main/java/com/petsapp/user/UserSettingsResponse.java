package com.petsapp.user;

import com.petsapp.auth.UserSettings;

/**
 * Ustawienia uzytkownika zwracane przez API.
 *
 * <p>Odwzorowuje encje UserSettings na DTO — bez ekspozycji szczegolów JPA ani relacji.
 */
public record UserSettingsResponse(
    boolean pushLikes,
    boolean pushComments,
    boolean pushFriendRequests,
    boolean pushAchievements,
    String language,
    boolean darkMode) {

  public static UserSettingsResponse from(UserSettings settings) {
    return new UserSettingsResponse(
        settings.isPushLikes(),
        settings.isPushComments(),
        settings.isPushFriendRequests(),
        settings.isPushAchievements(),
        settings.getLanguage(),
        settings.isDarkMode());
  }
}
