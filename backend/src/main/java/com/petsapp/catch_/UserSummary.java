package com.petsapp.catch_;

import com.petsapp.auth.User;
import java.util.UUID;

/**
 * Minimalne dane uzytkownika osadzane w CatchResponse i CommentResponse.
 *
 * <p>Zapobiega eksponowaniu pełnej encji User (email, passwordHash) w odpowiedziach catch/comment.
 */
public record UserSummary(UUID id, String username, String avatarUrl) {

  public static UserSummary from(User user) {
    return new UserSummary(user.getId(), user.getUsername(), user.getAvatarUrl());
  }
}
