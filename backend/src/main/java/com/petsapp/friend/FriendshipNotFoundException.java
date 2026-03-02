package com.petsapp.friend;

import java.util.UUID;

/**
 * Rzucany gdy prosba o znajomosc lub relacja znajomosci nie zostala znaleziona.
 */
public class FriendshipNotFoundException extends RuntimeException {

  public FriendshipNotFoundException(UUID friendshipId) {
    super("Friendship not found: " + friendshipId);
  }

  public FriendshipNotFoundException(String message) {
    super(message);
  }
}
