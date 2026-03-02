package com.petsapp.catch_;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO reprezentujacy polowanie psa zwracane przez API.
 *
 * <p>Nie eksponujemy encji JPA bezposrednio — to jest jedyny widok DogCatch na zewnatrz.
 * Pole {@code liked} informuje zalogowanego uzytkownika czy sami polajkowali ten catch.
 */
public record CatchResponse(
    UUID id,
    UserSummary user,
    BreedSummary breed,
    String photoUrl,
    String thumbnailUrl,
    String caption,
    Double latitude,
    Double longitude,
    String locationName,
    boolean isPublic,
    int likeCount,
    int commentCount,
    boolean liked,
    Instant caughtAt) {

  public static CatchResponse from(DogCatch entity, boolean liked) {
    return new CatchResponse(
        entity.getId(),
        UserSummary.from(entity.getUser()),
        BreedSummary.from(entity.getBreed()),
        entity.getPhotoUrl(),
        entity.getThumbnailUrl(),
        entity.getCaption(),
        entity.getLatitude(),
        entity.getLongitude(),
        entity.getLocationName(),
        entity.isPublic(),
        entity.getLikeCount(),
        entity.getCommentCount(),
        liked,
        entity.getCaughtAt());
  }
}
