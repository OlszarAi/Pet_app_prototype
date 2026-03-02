package com.petsapp.friend;

import com.petsapp.auth.User;
import java.util.UUID;

/**
 * DTO dla pozycji w rankingu znajomych.
 */
public record LeaderboardEntryResponse(
    UUID userId,
    String username,
    String avatarUrl,
    int uniqueBreeds,
    int totalCatches,
    int rank) {

  public static LeaderboardEntryResponse from(User user, int rank) {
    return new LeaderboardEntryResponse(
        user.getId(),
        user.getUsername(),
        user.getAvatarUrl(),
        user.getUniqueBreeds(),
        user.getTotalCatches(),
        rank);
  }
}
