package com.petsapp.catch_;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO reprezentujacy komentarz pod polowaniem zwracany przez API.
 *
 * <p>Soft-deleted komentarze maja deletedAt != null — tresc jest wtedy null po stronie serwisu
 * (nie wyswietlamy usuniętej tresci, ale zachowujemy strukture wątku).
 */
public record CommentResponse(UUID id, UserSummary user, String content, Instant createdAt) {

  public static CommentResponse from(Comment entity) {
    String visibleContent = entity.getDeletedAt() != null ? null : entity.getContent();
    return new CommentResponse(
        entity.getId(),
        UserSummary.from(entity.getUser()),
        visibleContent,
        entity.getCreatedAt());
  }
}
