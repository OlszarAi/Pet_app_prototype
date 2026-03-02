package com.petsapp.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Dane do aktualizacji ustawien uzytkownika.
 *
 * <p>Wszystkie flagi push sa wymagane (false = wylaczone). Language musi byc jednym z
 * obslugiwalnych kodow jezykowych.
 */
public record UpdateUserSettingsRequest(
    boolean pushLikes,
    boolean pushComments,
    boolean pushFriendRequests,
    boolean pushAchievements,
    @NotBlank(message = "Language is required")
        @Pattern(regexp = "^(pl|en)$", message = "Language must be 'pl' or 'en'")
        String language,
    boolean darkMode) {}
