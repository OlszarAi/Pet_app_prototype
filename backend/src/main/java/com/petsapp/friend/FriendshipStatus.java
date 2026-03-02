package com.petsapp.friend;

/**
 * Status relacji znajomosci. Mapuje sie na typ ENUM {@code friendship_status} w PostgreSQL.
 */
public enum FriendshipStatus {
  PENDING,
  ACCEPTED,
  BLOCKED
}
