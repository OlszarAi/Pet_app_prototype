package com.petsapp.user;

import com.petsapp.auth.User;
import java.time.Instant;
import java.util.UUID;

/**
 * Publiczny profil uzytkownika zwracany przez API.
 *
 * <p>Nigdy nie eksponujemy encji JPA bezposrednio — to DTO jest jedynym widokiem uzytkownika na
 * zewnatrz. hasEmail — wyswietlane tylko przy GET /users/me, null dla obcych profili.
 */
public record UserProfileResponse(
    UUID id,
    String username,
    String bio,
    String avatarUrl,
    int totalCatches,
    int uniqueBreeds,
    boolean isPrivate,
    String email,
    Instant createdAt) {

  /** Projekt pelny (wlasny profil) — zawiera email. */
  public static UserProfileResponse fromOwner(User user) {
    return new UserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.getBio(),
        user.getAvatarUrl(),
        user.getTotalCatches(),
        user.getUniqueBreeds(),
        user.isPrivate(),
        user.getEmail(),
        user.getCreatedAt());
  }

  /** Profil publiczny (inny uzytkownik) — bez emaila. */
  public static UserProfileResponse fromPublic(User user) {
    return new UserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.getBio(),
        user.getAvatarUrl(),
        user.getTotalCatches(),
        user.getUniqueBreeds(),
        user.isPrivate(),
        null,
        user.getCreatedAt());
  }
}
