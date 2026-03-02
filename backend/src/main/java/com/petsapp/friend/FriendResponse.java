package com.petsapp.friend;

import com.petsapp.auth.User;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO odpowiedzi dla relacji znajomosci.
 *
 * <p>Uzywany zarowno dla listy znajomych jak i dla oczekujacych prosb.
 */
public record FriendResponse(
    UUID friendshipId,
    UUID userId,
    String username,
    String avatarUrl,
    String bio,
    int totalCatches,
    int uniqueBreeds,
    String status,
    Instant createdAt) {

  public static FriendResponse from(Friendship friendship, UUID currentUserId) {
    User other =
        friendship.getRequester().getId().equals(currentUserId)
            ? friendship.getAddressee()
            : friendship.getRequester();

    return new FriendResponse(
        friendship.getId(),
        other.getId(),
        other.getUsername(),
        other.getAvatarUrl(),
        other.getBio(),
        other.getTotalCatches(),
        other.getUniqueBreeds(),
        friendship.getStatus().name().toLowerCase(),
        friendship.getCreatedAt());
  }

  /** Wariant dla oczekujacych prosb — zawsze zwraca dane requestera. */
  public static FriendResponse fromPendingRequest(Friendship friendship) {
    User requester = friendship.getRequester();
    return new FriendResponse(
        friendship.getId(),
        requester.getId(),
        requester.getUsername(),
        requester.getAvatarUrl(),
        requester.getBio(),
        requester.getTotalCatches(),
        requester.getUniqueBreeds(),
        "pending",
        friendship.getCreatedAt());
  }
}
