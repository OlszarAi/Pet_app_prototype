package com.petsapp.catch_;

import java.util.UUID;

/**
 * Rzucany gdy polowanie (DogCatch) o podanym ID nie istnieje lub zostalo soft-deleted.
 *
 * <p>Mapowany na HTTP 404 przez GlobalExceptionHandler.
 */
public class CatchNotFoundException extends RuntimeException {

  private final UUID catchId;

  public CatchNotFoundException(UUID catchId) {
    super("Catch not found: " + catchId);
    this.catchId = catchId;
  }

  public UUID getCatchId() {
    return catchId;
  }
}
