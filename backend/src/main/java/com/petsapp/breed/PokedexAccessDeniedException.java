package com.petsapp.breed;

import java.util.UUID;

/**
 * Wyjatek rzucany gdy uzytkownik proba dostac sie do Pokedeksu prywatnego profilu.
 *
 * <p>Mapowany na HTTP 403 przez {@code GlobalExceptionHandler}.
 */
public class PokedexAccessDeniedException extends RuntimeException {

  private final UUID targetUserId;

  public PokedexAccessDeniedException(UUID targetUserId) {
    super("Access to pokedex denied for user: " + targetUserId);
    this.targetUserId = targetUserId;
  }

  public UUID getTargetUserId() {
    return targetUserId;
  }
}
